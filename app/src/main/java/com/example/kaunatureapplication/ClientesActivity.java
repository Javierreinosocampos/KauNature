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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ClientesActivity extends AppCompatActivity {

    // ── Modelo local ─────────────────────────────────────────────────
    static class Cliente {
        String id;
        String nombre;
        String apellidos;
        String telefono;
        String email;
        String notas;
        String estado;
        double saldo;
        int    citas;
        int    membresias;
        String fechaAlta;

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
            this.membresias = 0;
            this.fechaAlta = m.createdAt != null && m.createdAt.length() >= 10
                    ? m.createdAt.substring(8,10) + "/" + m.createdAt.substring(5,7)
                    + "/" + m.createdAt.substring(0,4) : "—";
        }

        String inicial() {
            return nombre != null && !nombre.isEmpty()
                    ? String.valueOf(nombre.charAt(0)).toUpperCase() : "?";
        }

        String nombreCompleto() {
            return (apellidos != null && !apellidos.isEmpty())
                    ? nombre + " " + apellidos : nombre;
        }

        double  deudaReal   = 0;   // suma cobros pendientes
        int     citasReales = -1;  // -1 = aún no cargadas
        int     citasProx   = 0;   // citas pendiente/confirmadas

        boolean tieneDeuda() { return deudaReal > 0 || saldo < 0; }
        double  deudaTotal() { return deudaReal > 0 ? deudaReal : Math.abs(Math.min(saldo, 0)); }
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
        cargarClientes();
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
                            for (ClienteModel m : data) todosLosClientes.add(new Cliente(m));
                            renderLista();
                            cargarDeudasYCitasReales();
                        });
                    }
                    @Override public void onError(String e) {
                        runOnUiThread(() -> {
                            Toast.makeText(ClientesActivity.this,
                                    "Error al cargar: " + e, Toast.LENGTH_LONG).show();
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
            for (Cliente c : clientesFiltrados) listaClientes.addView(buildClienteCard(c));
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
            badge.setText(String.format("−%.2f€", cliente.deudaTotal()).replace(".", ","));
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

        TextView tvSaldo = view.findViewById(R.id.tvDetalleSaldo);
        if (cliente.tieneDeuda()) {
            tvSaldo.setText(String.format(java.util.Locale.US, "%.2f", cliente.deudaTotal()).replace(".", ",") + "€ pendiente");
            tvSaldo.setTextColor(Color.parseColor("#EF4444"));
        } else {
            tvSaldo.setText("Al día ✅");
            tvSaldo.setTextColor(Color.parseColor("#12B76A"));
        }
        // Citas reales (cargadas en segundo plano)
        TextView tvCitasD = view.findViewById(R.id.tvDetalleCitas);
        if (tvCitasD != null) {
            if (cliente.citasReales >= 0)
                tvCitasD.setText(cliente.citasReales + (cliente.citasProx > 0 ? " (" + cliente.citasProx + " próx.)" : ""));
            else
                tvCitasD.setText(String.valueOf(cliente.citas));
        }

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
            String nuevoEstado = "activo".equals(cliente.estado) ? "inactivo" : "activo";
            view.findViewById(R.id.btnDetalleToggle).setEnabled(false);
            if (cliente.id == null) {
                cliente.estado = nuevoEstado; sheet.dismiss(); renderLista(); return;
            }
            SupabaseRepository.get().toggleEstadoCliente(cliente.id, nuevoEstado,
                    new SupabaseRepository.Callback<Void>() {
                        @Override public void onSuccess(Void data) {
                            runOnUiThread(() -> {
                                cliente.estado = nuevoEstado;
                                sheet.dismiss(); renderLista();
                                Toast.makeText(ClientesActivity.this,
                                        cliente.nombreCompleto() + " → " + nuevoEstado,
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

        // Botones existentes en el XML
        // ── COBRAR: abre CobrosActivity con el cliente preseleccionado ──
        view.findViewById(R.id.btnDetalleCobrar).setOnClickListener(v -> {
            sheet.dismiss();
            Intent iCobros = new Intent(this, CobrosActivity.class);
            iCobros.putExtra("CLIENTE_ID",     cliente.id);
            iCobros.putExtra("CLIENTE_NOMBRE", cliente.nombreCompleto());
            startActivity(iCobros);
        });

        // ── CITA: abre AgendaActivity y abre directamente el form con cliente relleno ──
        view.findViewById(R.id.btnDetalleCita).setOnClickListener(v -> {
            sheet.dismiss();
            Intent iAgenda = new Intent(this, AgendaActivity.class);
            iAgenda.putExtra("CLIENTE_ID",       cliente.id);
            iAgenda.putExtra("CLIENTE_NOMBRE",   cliente.nombreCompleto());
            iAgenda.putExtra("ABRIR_NUEVA_CITA", true);
            startActivity(iAgenda);
        });

        // ── GYM: abre GimnasioActivity y abre directamente el sheet de apuntar ──
        view.findViewById(R.id.btnDetalleCheckin).setOnClickListener(v -> {
            sheet.dismiss();
            Intent iGym = new Intent(this, GimnasioActivity.class);
            iGym.putExtra("CLIENTE_NOMBRE",   cliente.nombreCompleto());
            iGym.putExtra("ABRIR_APUNTAR",    true);
            startActivity(iGym);
        });

        // ── Botones Editar y Eliminar ─────────────────────────────
        // Se añaden al contenedor del sheet dinámicamente
        // El sheet_detalle_cliente.xml tiene un ScrollView con un LinearLayout dentro
        // Localizamos el LinearLayout raíz y añadimos los botones al final
        android.view.ViewGroup contenedor = encontrarContenedor(view);
        if (contenedor != null) {
            // Separador
            View sep = new View(this);
            sep.setBackgroundColor(Color.parseColor("#DDE6FF"));
            LinearLayout.LayoutParams sepP = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(1));
            sepP.setMargins(0, dpToPx(12), 0, dpToPx(12));
            sep.setLayoutParams(sepP);
            contenedor.addView(sep);

            // Botón editar
            contenedor.addView(mkBtnSheet("✏️  Editar datos del cliente",
                    "#EEF4FF", "#0A66FF",
                    v -> { sheet.dismiss(); showFormularioCliente(cliente); }));

            // Botón eliminar
            contenedor.addView(mkBtnSheet("🗑  Eliminar cliente",
                    "#FFF0F0", "#EF4444",
                    v -> { sheet.dismiss(); confirmarEliminar(cliente); }));
        }

        sheet.show();

        // Cargar membresía activa y mostrar banner si existe
        if (cliente.id != null) {
            SupabaseRepository.get().getMembresias(cliente.id, true,
                    new SupabaseRepository.Callback<List<MembresiaModel>>() {
                        @Override public void onSuccess(List<MembresiaModel> mems) {
                            runOnUiThread(() -> {
                                if (!mems.isEmpty()) mostrarBannerMembresia(view, mems.get(0), sheet);
                            });
                        }
                        @Override public void onError(String e) {}
                    });
        }
    }

    private void mostrarBannerMembresia(View sheetView, MembresiaModel mem, BottomSheetDialog sheet) {
        android.view.ViewGroup c2 = encontrarContenedor(sheetView);
        if (c2 == null) return;

        CardView banner = new CardView(this);
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-1, -2);
        bp.setMargins(0, dpToPx(12), 0, 0); banner.setLayoutParams(bp);
        banner.setRadius(dpToPx(16)); banner.setCardElevation(dpToPx(2));
        banner.setCardBackgroundColor(Color.parseColor("#E8F0FF"));

        LinearLayout inner = new LinearLayout(this);
        inner.setOrientation(LinearLayout.VERTICAL);
        inner.setPadding(dpToPx(16), dpToPx(14), dpToPx(16), dpToPx(14));

        TextView tvTit = new TextView(this);
        tvTit.setText("🎫 Membresía activa · " + mem.tipo);
        tvTit.setTextSize(13f); tvTit.setTextColor(Color.parseColor("#0D1B3E"));
        tvTit.setTypeface(getResources().getFont(R.font.outfit_bold));
        inner.addView(tvTit);

        TextView tvPr = new TextView(this);
        tvPr.setText(String.format("%.0f€/mes · desde %s", mem.precio,
                mem.fechaInicio != null ? mem.fechaInicio : "—"));
        tvPr.setTextSize(11f); tvPr.setTextColor(Color.parseColor("#6B7FA3"));
        tvPr.setTypeface(getResources().getFont(R.font.outfit_regular));
        LinearLayout.LayoutParams pp2 = new LinearLayout.LayoutParams(-2, -2);
        pp2.topMargin = dpToPx(3); tvPr.setLayoutParams(pp2);
        inner.addView(tvPr);

        LinearLayout rowB = new LinearLayout(this);
        rowB.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rbP = new LinearLayout.LayoutParams(-1, -2);
        rbP.topMargin = dpToPx(12); rowB.setLayoutParams(rbP);

        CardView bEdit = new CardView(this);
        LinearLayout.LayoutParams ep = new LinearLayout.LayoutParams(0, dpToPx(42), 1f);
        ep.setMarginEnd(dpToPx(8)); bEdit.setLayoutParams(ep);
        bEdit.setRadius(dpToPx(10)); bEdit.setCardElevation(0);
        bEdit.setCardBackgroundColor(Color.parseColor("#0A66FF"));
        TextView tvEd = new TextView(this); tvEd.setText("✏️ Editar cuota");
        tvEd.setTextSize(12f); tvEd.setTextColor(Color.WHITE);
        tvEd.setTypeface(getResources().getFont(R.font.outfit_bold));
        tvEd.setGravity(Gravity.CENTER); tvEd.setLayoutParams(new LinearLayout.LayoutParams(-1, -1));
        bEdit.addView(tvEd); bEdit.setClickable(true);
        bEdit.setOnClickListener(v -> showEditarCuotaSheet(mem));
        rowB.addView(bEdit);

        CardView bCan = new CardView(this);
        bCan.setLayoutParams(new LinearLayout.LayoutParams(0, dpToPx(42), 1f));
        bCan.setRadius(dpToPx(10)); bCan.setCardElevation(0);
        bCan.setCardBackgroundColor(Color.parseColor("#FFF0F0"));
        TextView tvCan = new TextView(this); tvCan.setText("❌ Cancelar membresía");
        tvCan.setTextSize(11f); tvCan.setTextColor(Color.parseColor("#EF4444"));
        tvCan.setTypeface(getResources().getFont(R.font.outfit_bold));
        tvCan.setGravity(Gravity.CENTER); tvCan.setLayoutParams(new LinearLayout.LayoutParams(-1, -1));
        bCan.addView(tvCan); bCan.setClickable(true);
        bCan.setOnClickListener(v -> {
            if (mem.id == null) return;
            tvCan.setText("Cancelando..."); bCan.setClickable(false);
            String hoy = new java.text.SimpleDateFormat("yyyy-MM-dd",
                    java.util.Locale.getDefault()).format(new java.util.Date());
            SupabaseRepository.get().cancelarMembresia(mem.id, hoy,
                    new SupabaseRepository.Callback<Void>() {
                        @Override public void onSuccess(Void d) {
                            runOnUiThread(() -> { sheet.dismiss();
                                Toast.makeText(ClientesActivity.this, "Membresía cancelada", Toast.LENGTH_SHORT).show(); });
                        }
                        @Override public void onError(String e) {
                            runOnUiThread(() -> { tvCan.setText("❌ Cancelar membresía"); bCan.setClickable(true);
                                Toast.makeText(ClientesActivity.this, "Error: " + e, Toast.LENGTH_SHORT).show(); });
                        }
                    });
        });
        rowB.addView(bCan);
        inner.addView(rowB);
        banner.addView(inner);
        c2.addView(banner);
    }

    private void showEditarCuotaSheet(MembresiaModel mem) {
        BottomSheetDialog s2 = new BottomSheetDialog(this,
                com.google.android.material.R.style.Theme_Material3_Light_BottomSheetDialog);
        LinearLayout root2 = new LinearLayout(this);
        root2.setOrientation(LinearLayout.VERTICAL);
        root2.setBackgroundColor(Color.WHITE);
        root2.setPadding(dpToPx(20), dpToPx(24), dpToPx(20), dpToPx(48));

        TextView tvT2 = new TextView(this);
        tvT2.setText("Editar cuota mensual"); tvT2.setTextSize(18f);
        tvT2.setTextColor(Color.parseColor("#0D1B3E"));
        tvT2.setTypeface(getResources().getFont(R.font.outfit_bold));
        root2.addView(tvT2);

        final double[] np = {mem.precio};
        LinearLayout rowP = new LinearLayout(this);
        rowP.setOrientation(LinearLayout.HORIZONTAL);
        rowP.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rpp = new LinearLayout.LayoutParams(-1, -2);
        rpp.topMargin = dpToPx(24); rpp.bottomMargin = dpToPx(32); rowP.setLayoutParams(rpp);

        CardView bm = new CardView(this);
        bm.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(52), dpToPx(52)));
        bm.setRadius(dpToPx(12)); bm.setCardElevation(dpToPx(1));
        bm.setCardBackgroundColor(Color.parseColor("#EEF4FF"));
        TextView tvMn = new TextView(this); tvMn.setText("−"); tvMn.setTextSize(26f);
        tvMn.setTextColor(Color.parseColor("#0A66FF")); tvMn.setTypeface(getResources().getFont(R.font.outfit_bold));
        tvMn.setGravity(Gravity.CENTER); tvMn.setLayoutParams(new LinearLayout.LayoutParams(-1, -1));
        bm.addView(tvMn); bm.setClickable(true); rowP.addView(bm);

        final TextView tvPrecio2 = new TextView(this);
        tvPrecio2.setText(String.format("%.0f€", mem.precio));
        tvPrecio2.setTextSize(40f); tvPrecio2.setTextColor(Color.parseColor("#0A66FF"));
        tvPrecio2.setTypeface(getResources().getFont(R.font.outfit_bold));
        tvPrecio2.setGravity(Gravity.CENTER);
        tvPrecio2.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
        rowP.addView(tvPrecio2);

        CardView bma = new CardView(this);
        bma.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(52), dpToPx(52)));
        bma.setRadius(dpToPx(12)); bma.setCardElevation(dpToPx(1));
        bma.setCardBackgroundColor(Color.parseColor("#EEF4FF"));
        TextView tvMa = new TextView(this); tvMa.setText("+"); tvMa.setTextSize(26f);
        tvMa.setTextColor(Color.parseColor("#0A66FF")); tvMa.setTypeface(getResources().getFont(R.font.outfit_bold));
        tvMa.setGravity(Gravity.CENTER); tvMa.setLayoutParams(new LinearLayout.LayoutParams(-1, -1));
        bma.addView(tvMa); bma.setClickable(true); rowP.addView(bma);

        bm.setOnClickListener(v -> { if (np[0]>5) { np[0]-=5; tvPrecio2.setText(String.format("%.0f€",np[0])); } });
        bma.setOnClickListener(v -> { if (np[0]<500) { np[0]+=5; tvPrecio2.setText(String.format("%.0f€",np[0])); } });
        root2.addView(rowP);

        CardView btnSave = new CardView(this);
        btnSave.setLayoutParams(new LinearLayout.LayoutParams(-1, dpToPx(54)));
        btnSave.setRadius(dpToPx(16)); btnSave.setCardElevation(dpToPx(3));
        btnSave.setCardBackgroundColor(Color.parseColor("#0A66FF"));
        TextView tvSave = new TextView(this); tvSave.setText("Guardar nueva cuota");
        tvSave.setTextSize(14f); tvSave.setTextColor(Color.WHITE);
        tvSave.setTypeface(getResources().getFont(R.font.outfit_bold));
        tvSave.setGravity(Gravity.CENTER); tvSave.setLayoutParams(new LinearLayout.LayoutParams(-1, -1));
        btnSave.addView(tvSave); btnSave.setClickable(true);
        btnSave.setOnClickListener(v -> {
            if (mem.id == null) return;
            tvSave.setText("Guardando..."); btnSave.setClickable(false);
            SupabaseRepository.get().actualizarPrecioMembresia(mem.id, np[0],
                    new SupabaseRepository.Callback<Void>() {
                        @Override public void onSuccess(Void d) {
                            runOnUiThread(() -> { mem.precio = np[0]; s2.dismiss();
                                Toast.makeText(ClientesActivity.this,
                                        "✅ Cuota: " + (int)np[0] + "€/mes", Toast.LENGTH_SHORT).show(); });
                        }
                        @Override public void onError(String e) {
                            runOnUiThread(() -> { tvSave.setText("Guardar nueva cuota"); btnSave.setClickable(true);
                                Toast.makeText(ClientesActivity.this, "Error: "+e, Toast.LENGTH_SHORT).show(); });
                        }
                    });
        });
        root2.addView(btnSave);
        s2.setContentView(root2);
        s2.show();
    }

    /** Busca el LinearLayout raíz dentro del sheet (puede estar dentro de un ScrollView) */
    private android.view.ViewGroup encontrarContenedor(View root) {
        if (root instanceof LinearLayout) return (android.view.ViewGroup) root;
        if (root instanceof android.widget.ScrollView) {
            android.widget.ScrollView sv = (android.widget.ScrollView) root;
            if (sv.getChildCount() > 0 && sv.getChildAt(0) instanceof LinearLayout)
                return (LinearLayout) sv.getChildAt(0);
        }
        if (root instanceof android.view.ViewGroup) {
            android.view.ViewGroup vg = (android.view.ViewGroup) root;
            for (int i = 0; i < vg.getChildCount(); i++) {
                android.view.ViewGroup found = encontrarContenedor(vg.getChildAt(i));
                if (found != null) return found;
            }
        }
        return null;
    }

    private View mkBtnSheet(String texto, String bgColor, String textColor,
                            View.OnClickListener listener) {
        CardView btn = new CardView(this);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(52));
        p.bottomMargin = dpToPx(8);
        btn.setLayoutParams(p);
        btn.setRadius(dpToPx(14));
        btn.setCardElevation(dpToPx(1));
        btn.setCardBackgroundColor(Color.parseColor(bgColor));
        TextView tv = new TextView(this);
        tv.setText(texto);
        tv.setTextSize(13f);
        tv.setTextColor(Color.parseColor(textColor));
        tv.setTypeface(getResources().getFont(R.font.outfit_bold));
        tv.setGravity(Gravity.CENTER);
        tv.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        btn.addView(tv);
        btn.setClickable(true);
        btn.setOnClickListener(listener);
        return btn;
    }

    // ════════════════════════════════════════════════════════════════
    //  FORMULARIO CREAR / EDITAR (unificado)
    // ════════════════════════════════════════════════════════════════
    private void showFormularioCliente(Cliente clienteEditar) {
        BottomSheetDialog sheet = new BottomSheetDialog(
                this, com.google.android.material.R.style.Theme_Material3_Light_BottomSheetDialog);
        View view = getLayoutInflater().inflate(R.layout.sheet_nuevo_cliente, null);
        sheet.setContentView(view);

        EditText etNombre    = view.findViewById(R.id.etNuevoNombre);
        EditText etApellidos = view.findViewById(R.id.etNuevoApellidos);
        EditText etTelefono  = view.findViewById(R.id.etNuevoTelefono);
        EditText etEmail     = view.findViewById(R.id.etNuevoEmail);
        EditText etNotas     = view.findViewById(R.id.etNuevoNotas);

        // btnGuardarCliente es un CardView en el XML; el texto está en el TextView hijo
        androidx.cardview.widget.CardView btnGuardarCard = view.findViewById(R.id.btnGuardarCliente);
        TextView btnGuardarTv = (TextView) btnGuardarCard.getChildAt(0);

        boolean esEdicion = (clienteEditar != null);

        if (esEdicion) {
            etNombre.setText(clienteEditar.nombre);
            etApellidos.setText(clienteEditar.apellidos);
            etTelefono.setText(clienteEditar.telefono);
            etEmail.setText(clienteEditar.email);
            etNotas.setText(clienteEditar.notas);
            btnGuardarTv.setText("Guardar cambios");
        }

        btnGuardarCard.setOnClickListener(v -> {
            String nombre    = etNombre.getText().toString().trim();
            if (nombre.isEmpty()) { etNombre.setError("Obligatorio"); return; }

            String apellidos = etApellidos.getText().toString().trim();
            String telefono  = etTelefono.getText().toString().trim();
            String email     = etEmail.getText().toString().trim();
            String notas     = etNotas.getText().toString().trim();

            btnGuardarCard.setEnabled(false);
            btnGuardarTv.setText("Guardando...");

            if (esEdicion && clienteEditar.id != null) {
                // ── EDITAR ────────────────────────────────────────
                Map<String, Object> campos = new HashMap<>();
                campos.put("nombre",    nombre);
                campos.put("apellidos", apellidos);
                campos.put("telefono",  telefono);
                campos.put("email",     email);
                campos.put("notas",     notas);

                SupabaseRepository.get().actualizarCliente(clienteEditar.id, campos,
                        new SupabaseRepository.Callback<Void>() {
                            @Override public void onSuccess(Void data) {
                                runOnUiThread(() -> {
                                    clienteEditar.nombre    = nombre;
                                    clienteEditar.apellidos = apellidos;
                                    clienteEditar.telefono  = telefono;
                                    clienteEditar.email     = email;
                                    clienteEditar.notas     = notas;
                                    sheet.dismiss();
                                    ocultarTeclado();
                                    renderLista();
                                    Toast.makeText(ClientesActivity.this,
                                            "✅ Cliente actualizado", Toast.LENGTH_SHORT).show();
                                });
                            }
                            @Override public void onError(String e) {
                                runOnUiThread(() -> {
                                    btnGuardarCard.setEnabled(true);
                                    btnGuardarTv.setText("Guardar cambios");
                                    Toast.makeText(ClientesActivity.this,
                                            "Error: " + e, Toast.LENGTH_SHORT).show();
                                });
                            }
                        });
            } else {
                // ── CREAR ─────────────────────────────────────────
                ClienteModel modelo = new ClienteModel();
                modelo.nombre    = nombre;
                modelo.apellidos = apellidos;
                modelo.telefono  = telefono;
                modelo.email     = email;
                modelo.notas     = notas;
                modelo.estado    = "activo";
                modelo.saldo     = 0;

                SupabaseRepository.get().crearCliente(modelo,
                        new SupabaseRepository.Callback<ClienteModel>() {
                            @Override public void onSuccess(ClienteModel data) {
                                runOnUiThread(() -> {
                                    todosLosClientes.add(0, new Cliente(data));
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
                                    btnGuardarCard.setEnabled(true);
                                    btnGuardarTv.setText("Guardar");
                                    Toast.makeText(ClientesActivity.this,
                                            "Error: " + e, Toast.LENGTH_SHORT).show();
                                });
                            }
                        });
            }
        });

        sheet.show();
    }

    // ════════════════════════════════════════════════════════════════
    //  ELIMINAR CLIENTE (confirmación)
    // ════════════════════════════════════════════════════════════════
    private void confirmarEliminar(Cliente cliente) {
        BottomSheetDialog sheet = new BottomSheetDialog(
                this, com.google.android.material.R.style.Theme_Material3_Light_BottomSheetDialog);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.WHITE);
        root.setPadding(dpToPx(24), dpToPx(20), dpToPx(24), dpToPx(44));
        root.setGravity(Gravity.CENTER_HORIZONTAL);

        // Handle
        View handle = new View(this);
        android.graphics.drawable.GradientDrawable hBg = new android.graphics.drawable.GradientDrawable();
        hBg.setColor(Color.parseColor("#DDE6FF"));
        hBg.setCornerRadius(dpToPx(4));
        handle.setBackground(hBg);
        LinearLayout.LayoutParams hP = new LinearLayout.LayoutParams(dpToPx(40), dpToPx(4));
        hP.gravity = Gravity.CENTER_HORIZONTAL;
        hP.bottomMargin = dpToPx(24);
        handle.setLayoutParams(hP);
        root.addView(handle);

        TextView tvEmoji = new TextView(this);
        tvEmoji.setText("🗑");
        tvEmoji.setTextSize(44f);
        tvEmoji.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams eP = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        eP.bottomMargin = dpToPx(12);
        tvEmoji.setLayoutParams(eP);
        root.addView(tvEmoji);

        TextView tvTitulo = new TextView(this);
        tvTitulo.setText("Eliminar cliente");
        tvTitulo.setTextSize(20f);
        tvTitulo.setTextColor(Color.parseColor("#0D1B3E"));
        tvTitulo.setTypeface(getResources().getFont(R.font.outfit_bold));
        tvTitulo.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams tP = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        tP.bottomMargin = dpToPx(8);
        tvTitulo.setLayoutParams(tP);
        root.addView(tvTitulo);

        TextView tvDesc = new TextView(this);
        tvDesc.setText("¿Eliminar a " + cliente.nombreCompleto() + "?\nEsta acción no se puede deshacer.");
        tvDesc.setTextSize(13f);
        tvDesc.setTextColor(Color.parseColor("#6B7FA3"));
        tvDesc.setTypeface(getResources().getFont(R.font.outfit_regular));
        tvDesc.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams dP = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        dP.bottomMargin = dpToPx(28);
        tvDesc.setLayoutParams(dP);
        root.addView(tvDesc);

        LinearLayout btns = new LinearLayout(this);
        btns.setOrientation(LinearLayout.HORIZONTAL);
        btns.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        // Cancelar
        TextView btnCancelar = new TextView(this);
        btnCancelar.setText("Cancelar");
        btnCancelar.setTextSize(14f);
        btnCancelar.setTextColor(Color.parseColor("#0A66FF"));
        btnCancelar.setTypeface(getResources().getFont(R.font.outfit_bold));
        btnCancelar.setGravity(Gravity.CENTER);
        btnCancelar.setPadding(0, dpToPx(14), 0, dpToPx(14));
        android.graphics.drawable.GradientDrawable cancelBg = new android.graphics.drawable.GradientDrawable();
        cancelBg.setColor(Color.parseColor("#EEF4FF"));
        cancelBg.setCornerRadius(dpToPx(16));
        btnCancelar.setBackground(cancelBg);
        LinearLayout.LayoutParams cP = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        cP.setMarginEnd(dpToPx(10));
        btnCancelar.setLayoutParams(cP);
        btnCancelar.setOnClickListener(v -> sheet.dismiss());
        btns.addView(btnCancelar);

        // Eliminar
        TextView btnEliminar = new TextView(this);
        btnEliminar.setText("Eliminar");
        btnEliminar.setTextSize(14f);
        btnEliminar.setTextColor(Color.WHITE);
        btnEliminar.setTypeface(getResources().getFont(R.font.outfit_bold));
        btnEliminar.setGravity(Gravity.CENTER);
        btnEliminar.setPadding(0, dpToPx(14), 0, dpToPx(14));
        android.graphics.drawable.GradientDrawable elimBg = new android.graphics.drawable.GradientDrawable();
        elimBg.setColor(Color.parseColor("#EF4444"));
        elimBg.setCornerRadius(dpToPx(16));
        btnEliminar.setBackground(elimBg);
        btnEliminar.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        btnEliminar.setOnClickListener(v -> {
            if (cliente.id == null) {
                todosLosClientes.remove(cliente); sheet.dismiss(); renderLista(); return;
            }
            btnEliminar.setEnabled(false);
            btnEliminar.setText("Eliminando...");
            SupabaseRepository.get().eliminarCliente(cliente.id,
                    new SupabaseRepository.Callback<Void>() {
                        @Override public void onSuccess(Void data) {
                            runOnUiThread(() -> {
                                todosLosClientes.remove(cliente);
                                sheet.dismiss(); renderLista();
                                Toast.makeText(ClientesActivity.this,
                                        "🗑 " + cliente.nombreCompleto() + " eliminado",
                                        Toast.LENGTH_SHORT).show();
                            });
                        }
                        @Override public void onError(String e) {
                            runOnUiThread(() -> {
                                btnEliminar.setEnabled(true);
                                btnEliminar.setText("Eliminar");
                                Toast.makeText(ClientesActivity.this,
                                        "Error: " + e, Toast.LENGTH_SHORT).show();
                            });
                        }
                    });
        });
        btns.addView(btnEliminar);
        root.addView(btns);

        sheet.setContentView(root);
        sheet.show();
    }

    // ════════════════════════════════════════════════════════════════
    //  BOTONES Y NAV
    // ════════════════════════════════════════════════════════════════
    private void setupBotones() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnAddCliente).setOnClickListener(v -> showFormularioCliente(null));
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
    // ════════════════════════════════════════════════════════════════
    //  CARGA REAL DE DEUDAS Y CITAS DESDE SUPABASE
    // ════════════════════════════════════════════════════════════════
    private void cargarDeudasYCitasReales() {
        // Cobros pendientes → deuda real por cliente
        SupabaseRepository.get().getCobros("eq.pendiente",
                new SupabaseRepository.Callback<List<CobroModel>>() {
                    @Override public void onSuccess(List<CobroModel> cobros) {
                        java.util.Map<String, Double> deudas = new java.util.HashMap<>();
                        for (CobroModel c : cobros) {
                            if (c.clienteId == null) continue;
                            deudas.put(c.clienteId,
                                    deudas.getOrDefault(c.clienteId, 0.0) + c.importe);
                        }
                        boolean cambio = false;
                        for (Cliente cl : todosLosClientes) {
                            double d = deudas.getOrDefault(cl.id, 0.0);
                            if (cl.deudaReal != d) { cl.deudaReal = d; cambio = true; }
                        }
                        if (cambio) runOnUiThread(() -> renderLista());
                    }
                    @Override public void onError(String e) {}
                });

        // Citas: total y próximas por cliente
        SupabaseRepository.get().getCitasRango("2020-01-01", "2030-12-31",
                new SupabaseRepository.Callback<List<CitaModel>>() {
                    @Override public void onSuccess(List<CitaModel> citas) {
                        java.util.Map<String, int[]> mapa = new java.util.HashMap<>();
                        for (CitaModel c : citas) {
                            if (c.clienteId == null) continue;
                            int[] v = mapa.getOrDefault(c.clienteId, new int[]{0, 0});
                            v[0]++;
                            if ("pendiente".equals(c.estado) || "confirmada".equals(c.estado)) v[1]++;
                            mapa.put(c.clienteId, v);
                        }
                        for (Cliente cl : todosLosClientes) {
                            int[] v = mapa.getOrDefault(cl.id, new int[]{0, 0});
                            cl.citasReales = v[0];
                            cl.citasProx   = v[1];
                        }
                        runOnUiThread(() -> renderLista());
                    }
                    @Override public void onError(String e) {}
                });
    }

}