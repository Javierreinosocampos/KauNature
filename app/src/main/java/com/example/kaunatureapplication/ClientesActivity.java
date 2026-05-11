package com.example.kaunatureapplication;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ClientesActivity extends AppCompatActivity {

    // ── Modelo local ─────────────────────────────────────────────────
    static class Cliente {
        String id;           // UUID de Supabase
        String nombre;
        String apellidos;
        String telefono;
        String email;
        String notas;
        String estado;       // "activo" | "inactivo"
        double saldo;
        int    citas;
        int    membresias;
        String fechaAlta;

        /** Construye desde el modelo de red */
        Cliente(ClienteModel m) {
            this.id        = m.id;
            this.nombre    = m.nombre    != null ? m.nombre    : "";
            this.apellidos = m.apellidos != null ? m.apellidos : "";
            this.telefono  = m.telefono  != null ? m.telefono  : "";
            this.email     = m.email     != null ? m.email     : "";
            this.notas     = m.notas     != null ? m.notas     : "";
            this.estado    = m.estado    != null ? m.estado    : "activo";
            this.saldo     = m.saldo;
            this.citas     = m.citasTotal;
            this.membresias = 0;   // la BD no tiene membresías aún en ClienteModel
            this.fechaAlta = m.createdAt != null && m.createdAt.length() >= 10
                    ? m.createdAt.substring(8, 10) + "/" + m.createdAt.substring(5, 7)
                    + "/" + m.createdAt.substring(0, 4)
                    : "—";
        }

        /** Constructor para clientes nuevos antes de guardar */
        Cliente(String nombre, String telefono, String email,
                String notas, String estado, double saldo,
                int citas, int membresias, String fechaAlta) {
            this.id        = null;
            this.nombre    = nombre;
            this.apellidos = "";
            this.telefono  = telefono;
            this.email     = email;
            this.notas     = notas;
            this.estado    = estado;
            this.saldo     = saldo;
            this.citas     = citas;
            this.membresias = membresias;
            this.fechaAlta = fechaAlta;
        }

        String inicial() {
            return nombre != null && !nombre.isEmpty()
                    ? String.valueOf(nombre.charAt(0)).toUpperCase() : "?";
        }
        boolean tieneDeuda() { return saldo < 0; }

        String nombreCompleto() {
            return (apellidos != null && !apellidos.isEmpty())
                    ? nombre + " " + apellidos : nombre;
        }
    }

    // ── Estado ───────────────────────────────────────────────────────
    private final List<Cliente> todosLosClientes  = new ArrayList<>();
    private final List<Cliente> clientesFiltrados = new ArrayList<>();
    private String filtroActual   = "todos";
    private String busquedaActual = "";

    // ── Views ────────────────────────────────────────────────────────
    private LinearLayout listaClientes;
    private LinearLayout layoutVacio;
    private TextView     tvSubtitle;
    private TextView     filtroTodos, filtroActivos, filtroDeuda, filtroInactivos;
    private EditText     etBuscar;
    private TextView     btnLimpiar;

    // ════════════════════════════════════════════════════════════════
    //  onCreate
    // ════════════════════════════════════════════════════════════════
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_clientes);

        bindViews();
        setupBuscador();
        setupFiltros();
        setupBotones();
        setupBottomNav();
        cargarClientes();   // ← carga desde Supabase
    }

    // ════════════════════════════════════════════════════════════════
    //  BIND
    // ════════════════════════════════════════════════════════════════
    private void bindViews() {
        listaClientes   = findViewById(R.id.listaClientes);
        layoutVacio     = findViewById(R.id.layoutVacioClientes);
        tvSubtitle      = findViewById(R.id.tvClientesSubtitle);
        filtroTodos     = findViewById(R.id.filtroTodos);
        filtroActivos   = findViewById(R.id.filtroActivos);
        filtroDeuda     = findViewById(R.id.filtroDeuda);
        filtroInactivos = findViewById(R.id.filtroInactivos);
        etBuscar        = findViewById(R.id.etBuscar);
        btnLimpiar      = findViewById(R.id.btnLimpiarBusqueda);
    }

    // ════════════════════════════════════════════════════════════════
    //  CARGA DESDE SUPABASE
    // ════════════════════════════════════════════════════════════════
    private void cargarClientes() {
        tvSubtitle.setText("Cargando...");

        SupabaseRepository.get().getClientes(null,
                new SupabaseRepository.Callback<List<ClienteModel>>() {
                    @Override public void onSuccess(List<ClienteModel> data) {
                        runOnUiThread(() -> {
                            todosLosClientes.clear();
                            for (ClienteModel m : data)
                                todosLosClientes.add(new Cliente(m));
                            renderLista();
                        });
                    }
                    @Override public void onError(String e) {
                        runOnUiThread(() -> {
                            Toast.makeText(ClientesActivity.this,
                                    "Error al cargar clientes: " + e, Toast.LENGTH_LONG).show();
                            renderLista();
                        });
                    }
                });
    }

    // ════════════════════════════════════════════════════════════════
    //  BUSCADOR
    // ════════════════════════════════════════════════════════════════
    private void setupBuscador() {
        etBuscar.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            public void onTextChanged(CharSequence s, int st, int b, int c) {}
            public void afterTextChanged(Editable s) {
                busquedaActual = s.toString().trim().toLowerCase();
                btnLimpiar.setVisibility(busquedaActual.isEmpty() ? View.GONE : View.VISIBLE);
                renderLista();
            }
        });

        btnLimpiar.setOnClickListener(v -> {
            etBuscar.setText("");
            busquedaActual = "";
            btnLimpiar.setVisibility(View.GONE);
            ocultarTeclado();
        });
    }

    // ════════════════════════════════════════════════════════════════
    //  FILTROS
    // ════════════════════════════════════════════════════════════════
    private void setupFiltros() {
        filtroTodos.setOnClickListener(v     -> setFiltro("todos",    filtroTodos));
        filtroActivos.setOnClickListener(v   -> setFiltro("activo",   filtroActivos));
        filtroDeuda.setOnClickListener(v     -> setFiltro("deuda",    filtroDeuda));
        filtroInactivos.setOnClickListener(v -> setFiltro("inactivo", filtroInactivos));
    }

    private void setFiltro(String filtro, TextView btn) {
        filtroActual = filtro;
        for (TextView f : new TextView[]{filtroTodos, filtroActivos, filtroDeuda, filtroInactivos}) {
            f.setBackground(getDrawable(R.drawable.shape_filter_inactive));
            f.setTextColor(Color.parseColor("#6B7FA3"));
        }
        btn.setBackground(getDrawable(R.drawable.shape_filter_active));
        btn.setTextColor(Color.WHITE);
        renderLista();
    }

    // ════════════════════════════════════════════════════════════════
    //  RENDER LISTA
    // ════════════════════════════════════════════════════════════════
    private void renderLista() {
        clientesFiltrados.clear();

        for (Cliente c : todosLosClientes) {
            boolean pasaFiltro;
            switch (filtroActual) {
                case "activo":   pasaFiltro = "activo".equals(c.estado);   break;
                case "inactivo": pasaFiltro = "inactivo".equals(c.estado); break;
                case "deuda":    pasaFiltro = c.tieneDeuda();               break;
                default:         pasaFiltro = true;
            }

            boolean pasaBusqueda = busquedaActual.isEmpty()
                    || c.nombre.toLowerCase().contains(busquedaActual)
                    || c.apellidos.toLowerCase().contains(busquedaActual)
                    || c.telefono.toLowerCase().contains(busquedaActual)
                    || c.email.toLowerCase().contains(busquedaActual);

            if (pasaFiltro && pasaBusqueda) clientesFiltrados.add(c);
        }

        listaClientes.removeAllViews();

        if (clientesFiltrados.isEmpty()) {
            layoutVacio.setVisibility(View.VISIBLE);
            listaClientes.setVisibility(View.GONE);
        } else {
            layoutVacio.setVisibility(View.GONE);
            listaClientes.setVisibility(View.VISIBLE);
            for (Cliente c : clientesFiltrados)
                listaClientes.addView(buildClienteCard(c));
        }

        tvSubtitle.setText(todosLosClientes.size() + " registrados");
    }

    // ════════════════════════════════════════════════════════════════
    //  CARD DE CLIENTE
    // ════════════════════════════════════════════════════════════════
    private View buildClienteCard(Cliente cliente) {
        CardView card = new CardView(this);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cp.bottomMargin = dpToPx(10);
        card.setLayoutParams(cp);
        card.setRadius(dpToPx(18));
        card.setCardElevation(dpToPx(2));
        card.setCardBackgroundColor(Color.WHITE);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dpToPx(14), dpToPx(14), dpToPx(14), dpToPx(14));
        card.addView(row);

        // Avatar
        CardView avatar = new CardView(this);
        LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(dpToPx(46), dpToPx(46));
        ap.setMarginEnd(dpToPx(12));
        avatar.setLayoutParams(ap);
        avatar.setRadius(dpToPx(15));
        avatar.setCardElevation(dpToPx(2));
        avatar.setCardBackgroundColor("inactivo".equals(cliente.estado)
                ? Color.parseColor("#DDE6FF") : Color.parseColor("#0A66FF"));

        TextView tvInicial = new TextView(this);
        tvInicial.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        tvInicial.setText(cliente.inicial());
        tvInicial.setTextSize(18f);
        tvInicial.setTextColor(Color.WHITE);
        tvInicial.setTypeface(getResources().getFont(R.font.outfit_bold));
        tvInicial.setGravity(Gravity.CENTER);
        avatar.addView(tvInicial);
        row.addView(avatar);

        // Texto
        LinearLayout textBlock = new LinearLayout(this);
        textBlock.setOrientation(LinearLayout.VERTICAL);
        textBlock.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView tvNombre = new TextView(this);
        tvNombre.setText(cliente.nombreCompleto());
        tvNombre.setTextSize(13f);
        tvNombre.setTextColor(Color.parseColor("#0D1B3E"));
        tvNombre.setTypeface(getResources().getFont(R.font.outfit_bold));
        tvNombre.setAlpha("inactivo".equals(cliente.estado) ? 0.5f : 1f);
        textBlock.addView(tvNombre);

        TextView tvTel = new TextView(this);
        tvTel.setText(cliente.telefono.isEmpty() ? "Sin teléfono" : cliente.telefono);
        tvTel.setTextSize(11f);
        tvTel.setTextColor(Color.parseColor("#6B7FA3"));
        tvTel.setTypeface(getResources().getFont(R.font.outfit_regular));
        LinearLayout.LayoutParams telP = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        telP.topMargin = dpToPx(2);
        tvTel.setLayoutParams(telP);
        textBlock.addView(tvTel);
        row.addView(textBlock);

        // Badge
        LinearLayout rightCol = new LinearLayout(this);
        rightCol.setOrientation(LinearLayout.VERTICAL);
        rightCol.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rp.setMarginStart(dpToPx(8));
        rightCol.setLayoutParams(rp);

        TextView badge = new TextView(this);
        if (cliente.tieneDeuda()) {
            badge.setText(String.format("−%.2f€", Math.abs(cliente.saldo)).replace(".", ","));
            badge.setTextColor(Color.WHITE);
            badge.setBackground(getDrawable(R.drawable.shape_chip_blue));
            badge.getBackground().setTint(Color.parseColor("#EF4444"));
        } else if ("inactivo".equals(cliente.estado)) {
            badge.setText("Inactivo");
            badge.setTextColor(Color.parseColor("#6B7FA3"));
            badge.setBackground(getDrawable(R.drawable.shape_filter_inactive));
        } else {
            badge.setText("Activo");
            badge.setTextColor(Color.WHITE);
            badge.setBackground(getDrawable(R.drawable.shape_chip_ok));
        }
        badge.setTextSize(10f);
        badge.setTypeface(getResources().getFont(R.font.outfit_bold));
        badge.setPadding(dpToPx(10), dpToPx(4), dpToPx(10), dpToPx(4));
        rightCol.addView(badge);
        row.addView(rightCol);

        card.setClickable(true);
        card.setForeground(getDrawable(android.R.drawable.list_selector_background));
        card.setOnClickListener(v -> showDetalleCliente(cliente));
        return card;
    }

    // ════════════════════════════════════════════════════════════════
    //  SHEET: DETALLE CLIENTE
    // ════════════════════════════════════════════════════════════════
    private void showDetalleCliente(Cliente cliente) {
        BottomSheetDialog sheet = new BottomSheetDialog(
                this, com.google.android.material.R.style.Theme_Material3_Light_BottomSheetDialog);
        View view = getLayoutInflater().inflate(R.layout.sheet_detalle_cliente, null);
        sheet.setContentView(view);

        ((TextView) view.findViewById(R.id.tvDetalleInicial)).setText(cliente.inicial());
        ((TextView) view.findViewById(R.id.tvDetalleNombre)).setText(cliente.nombreCompleto());
        ((TextView) view.findViewById(R.id.tvDetalleFecha)).setText("Alta: " + cliente.fechaAlta);
        ((TextView) view.findViewById(R.id.tvDetalleTelefono)).setText(
                "📞 " + (cliente.telefono.isEmpty() ? "Sin teléfono" : cliente.telefono));
        ((TextView) view.findViewById(R.id.tvDetalleEmail)).setText(
                "✉️ " + (cliente.email.isEmpty() ? "Sin email" : cliente.email));
        ((TextView) view.findViewById(R.id.tvDetalleNotas)).setText(
                "📝 " + (cliente.notas.isEmpty() ? "Sin notas" : cliente.notas));
        ((TextView) view.findViewById(R.id.tvDetalleMembresias)).setText(String.valueOf(cliente.membresias));
        ((TextView) view.findViewById(R.id.tvDetalleCitas)).setText(String.valueOf(cliente.citas));

        // Saldo
        TextView tvSaldo = view.findViewById(R.id.tvDetalleSaldo);
        if (cliente.tieneDeuda()) {
            tvSaldo.setText(String.format("−%.2f€", Math.abs(cliente.saldo)).replace(".", ","));
            tvSaldo.setTextColor(Color.parseColor("#EF4444"));
        } else {
            tvSaldo.setText("Al día");
            tvSaldo.setTextColor(Color.parseColor("#12B76A"));
        }

        // Badge estado
        TextView tvEstado = view.findViewById(R.id.tvDetalleEstado);
        if ("activo".equals(cliente.estado)) {
            tvEstado.setText("Activo");
            tvEstado.setBackground(getDrawable(R.drawable.shape_chip_ok));
        } else {
            tvEstado.setText("Inactivo");
            tvEstado.setBackground(getDrawable(R.drawable.shape_filter_inactive));
            tvEstado.setTextColor(Color.parseColor("#6B7FA3"));
        }

        // Toggle activo/inactivo
        TextView tvToggle = view.findViewById(R.id.tvDetalleToggleLabel);
        tvToggle.setText("activo".equals(cliente.estado) ? "⏸ Inactivar" : "▶ Activar");

        view.findViewById(R.id.btnDetalleToggle).setOnClickListener(v -> {
            if (cliente.id == null) {
                // cliente local sin guardar
                cliente.estado = "activo".equals(cliente.estado) ? "inactivo" : "activo";
                sheet.dismiss();
                renderLista();
                return;
            }
            String nuevoEstado = "activo".equals(cliente.estado) ? "inactivo" : "activo";
            view.findViewById(R.id.btnDetalleToggle).setEnabled(false);

            SupabaseRepository.get().toggleEstadoCliente(cliente.id, nuevoEstado,
                    new SupabaseRepository.Callback<Void>() {
                        @Override public void onSuccess(Void data) {
                            runOnUiThread(() -> {
                                cliente.estado = nuevoEstado;
                                sheet.dismiss();
                                renderLista();
                                Toast.makeText(ClientesActivity.this,
                                        cliente.nombre + " → " + nuevoEstado,
                                        Toast.LENGTH_SHORT).show();
                            });
                        }
                        @Override public void onError(String e) {
                            runOnUiThread(() -> {
                                view.findViewById(R.id.btnDetalleToggle).setEnabled(true);
                                Toast.makeText(ClientesActivity.this,
                                        "Error: " + e, Toast.LENGTH_SHORT).show();
                            });
                        }
                    });
        });

        // Botón cobrar → abre CobrosActivity (o podrías abrir sheet de cobro rápido)
        view.findViewById(R.id.btnDetalleCobrar).setOnClickListener(v -> {
            sheet.dismiss();
            startActivity(new Intent(this, CobrosActivity.class));
        });

        // Botón nueva cita → abre AgendaActivity
        view.findViewById(R.id.btnDetalleCita).setOnClickListener(v -> {
            sheet.dismiss();
            startActivity(new Intent(this, AgendaActivity.class));
        });

        // Check-in gimnasio → abre AforoActivity
        view.findViewById(R.id.btnDetalleCheckin).setOnClickListener(v -> {
            sheet.dismiss();
            startActivity(new Intent(this, AforoActivity.class));
        });

        sheet.show();
    }

    // ════════════════════════════════════════════════════════════════
    //  SHEET: NUEVO CLIENTE
    // ════════════════════════════════════════════════════════════════
    private void showNuevoClienteSheet() {
        BottomSheetDialog sheet = new BottomSheetDialog(
                this, com.google.android.material.R.style.Theme_Material3_Light_BottomSheetDialog);
        View view = getLayoutInflater().inflate(R.layout.sheet_nuevo_cliente, null);
        sheet.setContentView(view);

        EditText etNombre   = view.findViewById(R.id.etNuevoNombre);
        EditText etTelefono = view.findViewById(R.id.etNuevoTelefono);
        EditText etEmail    = view.findViewById(R.id.etNuevoEmail);
        EditText etNotas    = view.findViewById(R.id.etNuevoNotas);
        TextView btnGuardar = view.findViewById(R.id.btnGuardarCliente);

        btnGuardar.setOnClickListener(v -> {
            String nombre = etNombre.getText().toString().trim();
            if (nombre.isEmpty()) {
                etNombre.setError("El nombre es obligatorio");
                return;
            }

            btnGuardar.setEnabled(false);
            btnGuardar.setText("Guardando...");

            // Construir el modelo para Supabase
            ClienteModel modelo = new ClienteModel();
            modelo.nombre    = nombre;
            modelo.apellidos = "";
            modelo.telefono  = etTelefono.getText().toString().trim();
            modelo.email     = etEmail.getText().toString().trim();
            modelo.notas     = etNotas.getText().toString().trim();
            modelo.estado    = "activo";
            modelo.saldo     = 0;

            SupabaseRepository.get().crearCliente(modelo,
                    new SupabaseRepository.Callback<ClienteModel>() {
                        @Override public void onSuccess(ClienteModel data) {
                            runOnUiThread(() -> {
                                Cliente nuevo = new Cliente(data);
                                todosLosClientes.add(0, nuevo);
                                sheet.dismiss();
                                ocultarTeclado();
                                renderLista();
                                Toast.makeText(ClientesActivity.this,
                                        "✅ Cliente añadido: " + nombre,
                                        Toast.LENGTH_SHORT).show();
                            });
                        }
                        @Override public void onError(String e) {
                            runOnUiThread(() -> {
                                btnGuardar.setEnabled(true);
                                btnGuardar.setText("Guardar");
                                Toast.makeText(ClientesActivity.this,
                                        "Error: " + e, Toast.LENGTH_SHORT).show();
                            });
                        }
                    });
        });

        sheet.show();
    }

    // ════════════════════════════════════════════════════════════════
    //  BOTONES Y NAV
    // ════════════════════════════════════════════════════════════════
    private void setupBotones() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnAddCliente).setOnClickListener(v -> showNuevoClienteSheet());
    }

    private void setupBottomNav() {
        NavHelper.setup(this, "clientes");
    }

    // ════════════════════════════════════════════════════════════════
    //  HELPERS
    // ════════════════════════════════════════════════════════════════
    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private void ocultarTeclado() {
        View focus = getCurrentFocus();
        if (focus != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(focus.getWindowToken(), 0);
        }
    }
}