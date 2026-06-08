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
import android.widget.ScrollView;
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


    static class Cliente {
        String id, nombre, apellidos, telefono, email, notas, estado, fechaAlta;
        double saldo;
        int    citas;

        MembresiaModel membresiaActiva    = null;
        MembresiaModel ultimaMembresia    = null;
        boolean        membresiasCargadas = false;
        boolean        tieneMembresia     = false;

        double deudaReal   = 0;
        int    citasReales = -1;
        int    citasProx   = 0;

        Cliente(ClienteModel m) {
            id        = m.id;
            nombre    = m.nombre    != null ? m.nombre    : "";
            apellidos = m.apellidos != null ? m.apellidos : "";
            telefono  = m.telefono  != null ? m.telefono  : "";
            email     = m.email     != null ? m.email     : "";
            notas     = m.notas     != null ? m.notas     : "";
            estado    = m.estado    != null ? m.estado    : "activo";
            saldo     = m.saldo;
            citas     = m.citasTotal;
            fechaAlta = m.createdAt != null && m.createdAt.length() >= 10
                    ? m.createdAt.substring(8, 10) + "/" + m.createdAt.substring(5, 7)
                    + "/" + m.createdAt.substring(0, 4) : "—";
        }

        String inicial() {
            return nombre != null && !nombre.isEmpty()
                    ? String.valueOf(nombre.charAt(0)).toUpperCase() : "?";
        }

        String nombreCompleto() {
            return (apellidos != null && !apellidos.isEmpty())
                    ? nombre + " " + apellidos : nombre;
        }

        boolean tieneDeuda() { return deudaReal > 0.001 || saldo < -0.001; }
        double  deudaTotal() { return deudaReal > 0.001 ? deudaReal : Math.abs(Math.min(saldo, 0)); }
    }

    private final List<Cliente> todosLosClientes  = new ArrayList<>();
    private final List<Cliente> clientesFiltrados = new ArrayList<>();
    private String filtroActual   = "todos";
    private String busquedaActual = "";
    private boolean cancelandoMembresia = false;
    private boolean sincronizandoMembresias = false;


    private LinearLayout listaClientes;
    private LinearLayout layoutVacio;
    private TextView     tvSubtitle;
    private TextView     filtroTodos, filtroActivos, filtroAlDia, filtroDeuda, filtroInactivos;
    private TextView     filtroConMembresia, filtroSinMembresia, filtroCancelada;
    private EditText     etBuscar;
    private TextView     btnLimpiar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Extiende el contenido detrás de la status bar
        getWindow().getDecorView().setSystemUiVisibility(
                android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                        android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        );
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        setContentView(R.layout.activity_clientes);
        bindViews();
        setupBuscador();
        setupFiltros();
        setupBotones();
        setupBottomNav();
        cargarClientes();
    }

    @Override
    protected void onResume() {
        super.onResume();
        android.util.Log.d("LIFECYCLE", "onResume - clientes: " + todosLosClientes.size());
        if (!todosLosClientes.isEmpty()) {
            sincronizarDatosCompleto();
        }
    }

    private void sincronizarDatosCompleto() {
        if (sincronizandoMembresias) {
            android.util.Log.w("SYNC", "Sincronización ya en curso - ignorando");
            return;
        }
        sincronizandoMembresias = true;
        android.util.Log.d("SYNC", "Iniciando sincronización completa...");
        cargarDeudasYCitasReales();
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            cargarMembresiasEnBackground();
        }, 300);
    }

    private void bindViews() {
        listaClientes   = findViewById(R.id.listaClientes);
        layoutVacio     = findViewById(R.id.layoutVacioClientes);
        tvSubtitle      = findViewById(R.id.tvClientesSubtitle);
        filtroTodos     = findViewById(R.id.filtroTodos);
        filtroActivos   = findViewById(R.id.filtroActivos);
        filtroAlDia     = findViewById(R.id.filtroAlDia);
        filtroDeuda     = findViewById(R.id.filtroDeuda);
        filtroInactivos = findViewById(R.id.filtroInactivos);
        etBuscar        = findViewById(R.id.etBuscar);
        btnLimpiar      = findViewById(R.id.btnLimpiarBusqueda);

        // Los filtros de membresía se crean dinámicamente
        filtroConMembresia = null;
        filtroSinMembresia = null;
        filtroCancelada    = null;

        inyectarFiltrosMembresia();
    }

    /** Inyecta los filtros de membresía si no existen en el XML */
    private void inyectarFiltrosMembresia() {
        if (filtroConMembresia != null && filtroSinMembresia != null && filtroCancelada != null) return;
        if (filtroTodos == null) return;

        android.view.ViewGroup cont = (android.view.ViewGroup) filtroTodos.getParent();
        if (cont == null) return;

        View sep = new View(this);
        sep.setBackgroundColor(Color.parseColor("#DDE6FF"));
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1, dpToPx(1));
        sp.setMargins(0, dpToPx(6), 0, dpToPx(6));
        sep.setLayoutParams(sp);
        cont.addView(sep);

        // PRIMERA FILA: Con membresía y Sin membresía
        LinearLayout fila1 = new LinearLayout(this);
        fila1.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams f1P = new LinearLayout.LayoutParams(-1, -2);
        f1P.bottomMargin = dpToPx(6);
        fila1.setLayoutParams(f1P);

        filtroConMembresia = buildFiltroBtn("Con membresía", false);
        LinearLayout.LayoutParams p1 = new LinearLayout.LayoutParams(0, -2, 1f);
        p1.setMarginEnd(dpToPx(6));
        filtroConMembresia.setLayoutParams(p1);

        filtroSinMembresia = buildFiltroBtn("Sin membresía", false);
        filtroSinMembresia.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));

        fila1.addView(filtroConMembresia);
        fila1.addView(filtroSinMembresia);
        cont.addView(fila1);

        // SEGUNDA FILA: Membresía cancelada
        filtroCancelada = buildFiltroBtn("Membresía cancelada", false);
        LinearLayout.LayoutParams pC = new LinearLayout.LayoutParams(-1, -2);
        filtroCancelada.setLayoutParams(pC);
        cont.addView(filtroCancelada);
    }

    private TextView buildFiltroBtn(String texto, boolean activo) {
        TextView tv = new TextView(this);
        tv.setText(texto);
        tv.setTextSize(12f);
        tv.setTypeface(getResources().getFont(R.font.outfit_bold));
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(dpToPx(12), dpToPx(8), dpToPx(12), dpToPx(8));
        if (activo) {
            tv.setBackground(getDrawable(R.drawable.shape_filter_active));
            tv.setTextColor(Color.WHITE);
        } else {
            tv.setBackground(getDrawable(R.drawable.shape_filter_inactive));
            tv.setTextColor(Color.parseColor("#6B7FA3"));
        }
        return tv;
    }


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
                            cargarMembresiasEnBackground();
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

    /**
     * Carga TODAS las membresías activas Y las canceladas recientes
     * para aplicar correctamente la regla de 30 días
     */
    private void cargarMembresiasEnBackground() {
        // PASO 1: Cargar membresías ACTIVAS
        SupabaseRepository.get().getMembresias(null, true,
                new SupabaseRepository.Callback<List<MembresiaModel>>() {
                    @Override public void onSuccess(List<MembresiaModel> activas) {
                        // Crear mapa de membresías por clienteId
                        Map<String, MembresiaModel> porCliente = new HashMap<>();
                        for (MembresiaModel m : activas) {
                            if (m.clienteId != null) {
                                porCliente.put(m.clienteId, m);
                            }
                        }

                        // Asignar membresías activas a los clientes
                        for (Cliente c : todosLosClientes) {
                            MembresiaModel activa = porCliente.get(c.id);
                            c.membresiaActiva = activa;
                            c.tieneMembresia  = activa != null;

                            // Log para debug
                            if (activa != null) {
                                android.util.Log.d("MEMBRESIAS", "Cliente " + c.nombreCompleto() +
                                        " tiene membresía " + activa.tipo);
                            }
                        }

                        // PASO 2: Cargar historial para clientes sin membresía activa
                        cargarMembresiasHistorial(() -> {
                            // PASO 3: Generar cobros automáticos SOLO si NO estamos cancelando
                            if (!cancelandoMembresia) {
                                generarCobrosMensualesAutomaticos(activas);
                            }

                            // PASO 4: Re-renderizar lista
                            runOnUiThread(() -> {
                                renderLista();
                                sincronizandoMembresias = false;
                                android.util.Log.d("SYNC", "✅ Sincronización completa terminada");
                            });
                        });
                    }
                    @Override public void onError(String e) {
                        android.util.Log.e("MEMBRESIAS", "Error cargando activas: " + e);
                        runOnUiThread(() -> {
                            Toast.makeText(ClientesActivity.this,
                                    "Error al cargar membresías: " + e, Toast.LENGTH_SHORT).show();
                        });
                    }
                });
    }

    /**
     * Carga el historial de membresías para aplicar la regla de 30 días
     */
    private void cargarMembresiasHistorial(Runnable onComplete) {
        final int[] pendientes = {0};
        for (Cliente c : todosLosClientes) {
            if (c.id != null && !c.tieneMembresia) {
                pendientes[0]++;
            }
        }

        if (pendientes[0] == 0) {
            if (onComplete != null) onComplete.run();
            return;
        }

        for (Cliente c : todosLosClientes) {
            if (c.id == null) {
                c.membresiasCargadas = true;
                continue;
            }

            // Si tiene membresía activa, no necesita historial
            if (c.tieneMembresia) {
                c.membresiasCargadas = true;
                continue;
            }

            // Buscar TODAS las membresías de este cliente (incluyendo canceladas)
            SupabaseRepository.get().getMembresias(c.id, false,
                    new SupabaseRepository.Callback<List<MembresiaModel>>() {
                        @Override public void onSuccess(List<MembresiaModel> todas) {
                            // La primera es la más reciente (ordenadas por created_at.desc)
                            MembresiaModel ultima = todas.isEmpty() ? null : todas.get(0);
                            c.ultimaMembresia = ultima;
                            c.membresiasCargadas = true;

                            // Log para debug
                            if (ultima != null && !ultima.activa) {
                                android.util.Log.d("MEMBRESIAS_HISTORIAL",
                                        "Cliente " + c.nombreCompleto() +
                                                " tiene membresía cancelada el " + ultima.fechaFin);
                            }

                            synchronized (ClientesActivity.this) {
                                pendientes[0]--;
                                if (pendientes[0] <= 0) {
                                    runOnUiThread(() -> {
                                        renderLista();
                                        if (onComplete != null) onComplete.run();
                                    });
                                }
                            }
                        }
                        @Override public void onError(String e) {
                            c.membresiasCargadas = true;
                            android.util.Log.e("MEMBRESIAS_HISTORIAL",
                                    "Error para cliente " + c.nombreCompleto() + ": " + e);
                            synchronized (ClientesActivity.this) {
                                pendientes[0]--;
                                if (pendientes[0] <= 0 && onComplete != null) {
                                    runOnUiThread(onComplete);
                                }
                            }
                        }
                    });
        }
    }

    /**
     * Para cada membresía activa, verifica si el cobro del mes actual
     * ya existe; si no, lo genera como "pendiente".
     * Se ejecuta en background silenciosamente.
     */
    private void generarCobrosMensualesAutomaticos(List<MembresiaModel> mems) {
        if (cancelandoMembresia) return; // no generar cobros automáticos durante una cancelación
        for (MembresiaModel mem : mems) {
            if (!mem.activa || mem.clienteId == null) continue;

            // Buscar nombre del cliente
            String nombreCliente = "Cliente";
            for (Cliente c : todosLosClientes) {
                if (c.id != null && c.id.equals(mem.clienteId)) {
                    nombreCliente = c.nombreCompleto();
                    break;
                }
            }
            final String nomFinal = nombreCliente;

            SupabaseRepository.get().generarCobroMensualSiProcede(mem, nomFinal,
                    new SupabaseRepository.Callback<Boolean>() {
                        @Override public void onSuccess(Boolean generado) {
                            if (generado) {
                                // Cobro nuevo generado — refrescar deudas
                                cargarDeudasYCitasReales();
                                runOnUiThread(() ->
                                        Toast.makeText(ClientesActivity.this,
                                                "Cobro mensual generado: " + nomFinal,
                                                Toast.LENGTH_SHORT).show());
                            }
                        }
                        @Override public void onError(String e) {
                            android.util.Log.e("COBRO_AUTO", "Error: " + e);
                        }
                    });
        }
    }




    private void refrescarMembresiasCompleto() {
        android.util.Log.d("MEMBRESIAS", "Refrescando membresías completo...");
        cargarMembresiasEnBackground();
    }


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


    private void setupFiltros() {
        filtroTodos.setOnClickListener(v     -> setFiltro("todos",         filtroTodos));
        filtroActivos.setOnClickListener(v   -> setFiltro("activo",        filtroActivos));
        if (filtroAlDia != null)
            filtroAlDia.setOnClickListener(v -> setFiltro("al_dia",        filtroAlDia));
        filtroDeuda.setOnClickListener(v     -> setFiltro("deuda",         filtroDeuda));
        filtroInactivos.setOnClickListener(v -> setFiltro("inactivo",      filtroInactivos));
        if (filtroConMembresia != null)
            filtroConMembresia.setOnClickListener(v -> setFiltro("con_membresia", filtroConMembresia));
        if (filtroSinMembresia != null)
            filtroSinMembresia.setOnClickListener(v -> setFiltro("sin_membresia", filtroSinMembresia));
        if (filtroCancelada != null)
            filtroCancelada.setOnClickListener(v -> setFiltro("cancelada", filtroCancelada));
    }

    private void setFiltro(String filtro, TextView btn) {
        filtroActual = filtro;
        List<TextView> todos = new ArrayList<>();
        todos.add(filtroTodos); todos.add(filtroActivos);
        if (filtroAlDia != null) todos.add(filtroAlDia);
        todos.add(filtroDeuda); todos.add(filtroInactivos);
        if (filtroConMembresia != null) todos.add(filtroConMembresia);
        if (filtroSinMembresia != null) todos.add(filtroSinMembresia);
        if (filtroCancelada != null) todos.add(filtroCancelada);

        for (TextView f : todos) {
            if (f == null) continue;
            f.setBackground(getDrawable(R.drawable.shape_filter_inactive));
            f.setTextColor(Color.parseColor("#6B7FA3"));
        }
        btn.setBackground(getDrawable(R.drawable.shape_filter_active));
        btn.setTextColor(Color.WHITE);
        renderLista();
    }


    private void renderLista() {
        clientesFiltrados.clear();
        int conMembresia = 0;
        int canceladas = 0;
        for (Cliente c : todosLosClientes) {

            if (!c.tieneMembresia && c.ultimaMembresia != null &&
                    c.ultimaMembresia.canceladaEnPeriodoEspera()) {
                canceladas++;
            }

            android.util.Log.d("RENDER_LISTA",
                    "Total: " + todosLosClientes.size() +
                            " | Con membresía: " + conMembresia +
                            " | Canceladas en espera: " + canceladas +
                            " | Filtro actual: " + filtroActual);


            boolean pasaFiltro;
            switch (filtroActual) {
                case "activo":        pasaFiltro = "activo".equals(c.estado);   break;
                case "al_dia":        pasaFiltro = "activo".equals(c.estado) && !c.tieneDeuda(); break;
                case "inactivo":      pasaFiltro = "inactivo".equals(c.estado); break;
                case "deuda":         pasaFiltro = c.tieneDeuda();              break;
                case "con_membresia": pasaFiltro = c.tieneMembresia;            break;
                case "sin_membresia": pasaFiltro = !c.tieneMembresia;           break;
                case "cancelada":
                    pasaFiltro = !c.tieneMembresia && c.ultimaMembresia != null
                            && c.ultimaMembresia.canceladaEnPeriodoEspera();
                    break;
                default:              pasaFiltro = true;
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

        for (Cliente c : todosLosClientes) if (c.tieneMembresia) conMembresia++;
        tvSubtitle.setText(todosLosClientes.size() + " registrados · " + conMembresia + " con membresía");
    }

    private View buildClienteCard(Cliente cliente) {
        CardView card = new CardView(this);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2);
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
        int avatarColor;
        if (cliente.tieneMembresia)            avatarColor = Color.parseColor("#059669");
        else if ("inactivo".equals(cliente.estado)) avatarColor = Color.parseColor("#DDE6FF");
        else                                    avatarColor = Color.parseColor("#0A66FF");
        avatar.setCardBackgroundColor(avatarColor);

        TextView tvInicial = new TextView(this);
        tvInicial.setLayoutParams(new LinearLayout.LayoutParams(-1, -1));
        tvInicial.setText(cliente.inicial());
        tvInicial.setTextSize(18f);
        tvInicial.setTextColor(Color.WHITE);
        tvInicial.setTypeface(getResources().getFont(R.font.outfit_bold));
        tvInicial.setGravity(Gravity.CENTER);
        if ("inactivo".equals(cliente.estado))
            tvInicial.setTextColor(Color.parseColor("#6B7FA3"));
        avatar.addView(tvInicial);
        row.addView(avatar);

        // Bloque de texto
        LinearLayout textBlock = new LinearLayout(this);
        textBlock.setOrientation(LinearLayout.VERTICAL);
        textBlock.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));

        TextView tvNombre = new TextView(this);
        tvNombre.setText(cliente.nombreCompleto());
        tvNombre.setTextSize(13f);
        tvNombre.setTextColor(Color.parseColor("#0D1B3E"));
        tvNombre.setTypeface(getResources().getFont(R.font.outfit_bold));
        tvNombre.setSingleLine(true);
        tvNombre.setMaxLines(1);
        tvNombre.setEllipsize(android.text.TextUtils.TruncateAt.END);
        tvNombre.setAlpha("inactivo".equals(cliente.estado) ? 0.5f : 1f);
        textBlock.addView(tvNombre);

        // Sub-línea: teléfono + pill membresía
        LinearLayout subRow = new LinearLayout(this);
        subRow.setOrientation(LinearLayout.HORIZONTAL);
        subRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams srP = new LinearLayout.LayoutParams(-1, -2);
        srP.topMargin = dpToPx(2);
        subRow.setLayoutParams(srP);

        TextView tvTel = new TextView(this);
        tvTel.setText(cliente.telefono.isEmpty() ? "Sin teléfono" : cliente.telefono);
        tvTel.setTextSize(11f);
        tvTel.setTextColor(Color.parseColor("#6B7FA3"));
        tvTel.setTypeface(getResources().getFont(R.font.outfit_regular));
        tvTel.setSingleLine(true);
        tvTel.setMaxLines(1);
        tvTel.setEllipsize(android.text.TextUtils.TruncateAt.END);
        subRow.addView(tvTel);

        textBlock.addView(subRow);

        // Línea propia para la cuota (nunca salta de línea, siempre alineada)
        if (cliente.tieneMembresia && cliente.membresiaActiva != null) {
            int dia = cliente.membresiaActiva.diaMesInicio();

            String fechaInicio = cliente.membresiaActiva.fechaInicio;
            String mm = "";
            if (fechaInicio != null && fechaInicio.length() >= 7) {
                mm = fechaInicio.substring(5, 7);
            }

            TextView tvMemb = new TextView(this);
            tvMemb.setText(mm.isEmpty() ? ("Cuota día " + dia) : ("Cuota día " + dia + "/" + mm));
            tvMemb.setTextSize(10f);
            tvMemb.setTextColor(Color.parseColor("#059669"));
            tvMemb.setTypeface(getResources().getFont(R.font.outfit_bold));
            tvMemb.setSingleLine(true);
            tvMemb.setMaxLines(1);
            tvMemb.setEllipsize(android.text.TextUtils.TruncateAt.END);
            LinearLayout.LayoutParams mp = new LinearLayout.LayoutParams(-1, -2);
            mp.topMargin = dpToPx(1);
            tvMemb.setLayoutParams(mp);
            textBlock.addView(tvMemb);
        }
        row.addView(textBlock);

        // Badge derecho
        LinearLayout rightCol = new LinearLayout(this);
        rightCol.setOrientation(LinearLayout.VERTICAL);
        rightCol.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-2, -2);
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
        } else if (cliente.tieneMembresia) {
            badge.setText("Con membresía");
            badge.setTextColor(Color.WHITE);
            badge.setBackground(getDrawable(R.drawable.shape_chip_ok));
            badge.getBackground().setTint(Color.parseColor("#059669"));
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


    private void showDetalleCliente(Cliente cliente) {
        BottomSheetDialog sheet = new BottomSheetDialog(
                this, com.google.android.material.R.style.Theme_Material3_Light_BottomSheetDialog);
        View view = getLayoutInflater().inflate(R.layout.sheet_detalle_cliente, null);
        sheet.setContentView(view);

        ((TextView) view.findViewById(R.id.tvDetalleInicial)).setText(cliente.inicial());
        ((TextView) view.findViewById(R.id.tvDetalleNombre)).setText(cliente.nombreCompleto());
        ((TextView) view.findViewById(R.id.tvDetalleFecha)).setText("Alta: " + cliente.fechaAlta);
        ((TextView) view.findViewById(R.id.tvDetalleTelefono)).setText(
                cliente.telefono.isEmpty() ? "Sin teléfono" : cliente.telefono);
        ((TextView) view.findViewById(R.id.tvDetalleEmail)).setText(
                cliente.email.isEmpty() ? "Sin email" : cliente.email);
        ((TextView) view.findViewById(R.id.tvDetalleNotas)).setText(
                cliente.notas.isEmpty() ? "Sin notas" : cliente.notas);

        TextView tvDetalleMembresias = view.findViewById(R.id.tvDetalleMembresias);
        if (tvDetalleMembresias != null) {
            tvDetalleMembresias.setText(cliente.tieneMembresia ? "1 activa" : "Ninguna");
        }

        TextView tvCitasD = view.findViewById(R.id.tvDetalleCitas);
        if (tvCitasD != null) {
            if (cliente.citasReales >= 0)
                tvCitasD.setText(cliente.citasReales
                        + (cliente.citasProx > 0 ? " (" + cliente.citasProx + " próx.)" : ""));
            else tvCitasD.setText(String.valueOf(cliente.citas));
        }

        TextView tvSaldo = view.findViewById(R.id.tvDetalleSaldo);
        if (cliente.tieneDeuda()) {
            tvSaldo.setText(String.format(Locale.US, "%.2f", cliente.deudaTotal())
                    .replace(".", ",") + "€ pendiente");
            tvSaldo.setTextColor(Color.parseColor("#EF4444"));
        } else {
            tvSaldo.setText("Al día");
            tvSaldo.setTextColor(Color.parseColor("#12B76A"));
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
        tvToggle.setText("activo".equals(cliente.estado) ? "Inactivar" : "Activar");
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

        // Botón COBRAR
        view.findViewById(R.id.btnDetalleCobrar).setOnClickListener(v -> {
            sheet.dismiss();
            Intent i = new Intent(this, CobrosActivity.class);
            i.putExtra("CLIENTE_ID",     cliente.id);
            i.putExtra("CLIENTE_NOMBRE", cliente.nombreCompleto());
            startActivity(i);
        });

        // Botón CITA
        view.findViewById(R.id.btnDetalleCita).setOnClickListener(v -> {
            sheet.dismiss();
            Intent i = new Intent(this, AgendaActivity.class);
            i.putExtra("CLIENTE_ID",       cliente.id);
            i.putExtra("CLIENTE_NOMBRE",   cliente.nombreCompleto());
            i.putExtra("ABRIR_NUEVA_CITA", true);
            startActivity(i);
        });

        // Botón GYM
        view.findViewById(R.id.btnDetalleCheckin).setOnClickListener(v -> {
            sheet.dismiss();
            Intent i = new Intent(this, GimnasioActivity.class);
            i.putExtra("CLIENTE_NOMBRE", cliente.nombreCompleto());
            i.putExtra("ABRIR_APUNTAR",  true);
            startActivity(i);
        });

        // Botones Editar / Eliminar
        android.view.ViewGroup contenedor = encontrarContenedor(view);
        if (contenedor != null) {
            contenedor.addView(mkSeparador());
            contenedor.addView(mkBtnSheet("Editar datos del cliente",
                    "#EEF4FF", "#0A66FF",
                    v -> { sheet.dismiss(); showFormularioCliente(cliente); }));
            contenedor.addView(mkBtnSheet("Eliminar cliente",
                    "#FFF0F0", "#EF4444",
                    v -> { sheet.dismiss(); confirmarEliminar(cliente); }));
        }

        sheet.show();

        // Carga membresía fresca desde Supabase y renderiza el bloque
        if (cliente.id != null) {
            cargarYMostrarBloqueMembresiaDetalle(view, cliente, sheet);
        }
    }




    private void cargarYMostrarBloqueMembresiaDetalle(View sheetView, Cliente cliente,
                                                      BottomSheetDialog sheet) {
        SupabaseRepository.get().getMembresias(cliente.id, true,
                new SupabaseRepository.Callback<List<MembresiaModel>>() {
                    @Override public void onSuccess(List<MembresiaModel> activas) {
                        MembresiaModel activa = activas.isEmpty() ? null : activas.get(0);

                        if (activa == null) {
                            // Sin activa → buscar historial para regla 30 días
                            SupabaseRepository.get().getMembresias(cliente.id, false,
                                    new SupabaseRepository.Callback<List<MembresiaModel>>() {
                                        @Override public void onSuccess(List<MembresiaModel> todas) {
                                            MembresiaModel ultima = todas.isEmpty() ? null : todas.get(0);
                                            runOnUiThread(() -> {
                                                cliente.membresiaActiva    = null;
                                                cliente.ultimaMembresia    = ultima;
                                                cliente.tieneMembresia     = false;
                                                cliente.membresiasCargadas = true;
                                                mostrarBloqueMembresiaEnDetalle(sheetView, cliente, null, ultima, sheet);
                                                renderLista();
                                            });
                                        }
                                        @Override public void onError(String e) {
                                            runOnUiThread(() ->
                                                    mostrarBloqueMembresiaEnDetalle(sheetView, cliente, null, null, sheet));
                                        }
                                    });
                        } else {
                            runOnUiThread(() -> {
                                cliente.membresiaActiva    = activa;
                                cliente.tieneMembresia     = true;
                                cliente.membresiasCargadas = true;
                                mostrarBloqueMembresiaEnDetalle(sheetView, cliente, activa, null, sheet);
                                renderLista();
                            });
                        }
                    }
                    @Override public void onError(String e) {
                        runOnUiThread(() ->
                                mostrarBloqueMembresiaEnDetalle(sheetView, cliente, null, null, sheet));
                    }
                });
    }

    /**
     * Renderiza el bloque de membresía dentro del sheet de detalle.
     * CASOS:
     *  A) activa != null          → info + día inicio + próximo cobro + [Editar cuota] [Cancelar]
     *  B) cancelada en período espera → aviso bloqueo + días restantes DINÁMICOS
     *  C) sin membresía / período terminado → botón inscribirse
     */
    private void mostrarBloqueMembresiaEnDetalle(View sheetView, Cliente cliente,
                                                 MembresiaModel activa,
                                                 MembresiaModel ultimaCancelada,
                                                 BottomSheetDialog sheet) {
        android.view.ViewGroup cont = encontrarContenedor(sheetView);
        if (cont == null) return;

        cont.addView(mkSeparador());

        CardView banner = new CardView(this);
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-1, -2);
        bp.setMargins(0, dpToPx(4), 0, dpToPx(8));
        banner.setLayoutParams(bp);
        banner.setRadius(dpToPx(16));
        banner.setCardElevation(dpToPx(2));

        LinearLayout inner = new LinearLayout(this);
        inner.setOrientation(LinearLayout.VERTICAL);
        inner.setPadding(dpToPx(16), dpToPx(14), dpToPx(16), dpToPx(14));

        // ── CASO A: TIENE MEMBRESÍA ACTIVA ─────────────────────────
        if (activa != null) {
            banner.setCardBackgroundColor(Color.parseColor("#E8F5EE"));

            TextView tvTit = mkTv("Membresía activa · " + activa.tipo, 13f, "#0D1B3E", true);
            inner.addView(tvTit);

            // Información según tipo de membresía
            String infoTexto;
            if ("mensual".equalsIgnoreCase(activa.tipo)) {
                int diaInicio = activa.diaMesInicio();
                String diaTexto = diaInicio > 0 ? "Renovación el día " + diaInicio + " de cada mes" : "Renovación mensual";
                String proxCobro = activa.proximoCobro();
                String proxTexto = proxCobro.isEmpty() ? "" : "\nPróximo cobro: " + formatFecha(proxCobro);
                infoTexto = String.format("%.0f€/mes  ·  Inicio: %s\n%s%s", activa.precio,
                        activa.fechaInicio != null ? formatFecha(activa.fechaInicio) : "—", diaTexto, proxTexto);
            } else {
                String fechaFin = activa.calcularFechaFin();
                infoTexto = String.format("%.0f€  ·  Inicio: %s\nVálida hasta: %s\nCobro único al inicio",
                        activa.precio, activa.fechaInicio != null ? formatFecha(activa.fechaInicio) : "—",
                        !fechaFin.isEmpty() ? formatFecha(fechaFin) : "—");
            }

            TextView tvInfo = mkTv(infoTexto, 11f, "#6B7FA3", false);
            LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(-1, -2);
            ip.topMargin = dpToPx(6);
            tvInfo.setLayoutParams(ip);
            inner.addView(tvInfo);

            LinearLayout rowB = new LinearLayout(this);
            rowB.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams rbP = new LinearLayout.LayoutParams(-1, -2);
            rbP.topMargin = dpToPx(14);
            rowB.setLayoutParams(rbP);

            CardView bEdit = buildBtnCard("Editar cuota", "#EEF4FF", Color.parseColor("#0A66FF"));
            LinearLayout.LayoutParams ep = new LinearLayout.LayoutParams(0, dpToPx(42), 1f);
            ep.setMarginEnd(dpToPx(8));
            bEdit.setLayoutParams(ep);
            bEdit.setOnClickListener(v -> showEditarCuotaSheet(activa));
            rowB.addView(bEdit);

            CardView bCan = buildBtnCard("Cancelar membresía", "#FFF0F0", Color.parseColor("#EF4444"));
            bCan.setLayoutParams(new LinearLayout.LayoutParams(0, dpToPx(42), 1f));
            bCan.setOnClickListener(v -> confirmarCancelacionMembresia(activa, cliente, sheet));
            rowB.addView(bCan);
            inner.addView(rowB);

            // ── CASO B: CANCELADA EN PERÍODO DE ESPERA (DINÁMICO) ────────────────
        } else if (ultimaCancelada != null && ultimaCancelada.canceladaEnPeriodoEspera()) {
            banner.setCardBackgroundColor(Color.parseColor("#FFF8ED"));

            int diasRestantes = ultimaCancelada.diasParaReinscripcion();
            String tipoMembresia = ultimaCancelada.tipo != null ? ultimaCancelada.tipo : "Mensual";
            String fechaFin = ultimaCancelada.fechaFin != null ? formatFecha(ultimaCancelada.fechaFin) : "—";

            inner.addView(mkTv("Membresía " + tipoMembresia + " cancelada", 13f, "#92400E", true));

            String textoTiempo;
            if (diasRestantes == 0) textoTiempo = "¡Puede reinscribirse hoy!";
            else if (diasRestantes == 1) textoTiempo = "Podrá reinscribirse mañana.";
            else if (diasRestantes <= 7) textoTiempo = "Podrá reinscribirse en " + diasRestantes + " días.";
            else if (diasRestantes <= 30) textoTiempo = "Debe esperar " + diasRestantes + " días más.";
            else textoTiempo = "Debe esperar " + diasRestantes + " días hasta el fin del período.";

            TextView tvInfo = mkTv("Canceló su membresía " + tipoMembresia + ".\nPeríodo de suscripción termina el: " + fechaFin +
                    "\n\nDebe esperar hasta el final del período para reinscribirse.\nSi tiene cobros pendientes, debe saldarlos antes de reinscribirse.\n\n" +
                    textoTiempo, 11f, "#92400E", false);
            LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(-1, -2);
            ip.topMargin = dpToPx(6);
            tvInfo.setLayoutParams(ip);
            inner.addView(tvInfo);

            String badgeTexto, badgeColor;
            if (diasRestantes == 0) { badgeTexto = "Disponible hoy"; badgeColor = "#059669"; }
            else if (diasRestantes == 1) { badgeTexto = "Disponible mañana"; badgeColor = "#F59E0B"; }
            else if (diasRestantes <= 7) { badgeTexto = diasRestantes + " días restantes"; badgeColor = "#EF4444"; }
            else if (diasRestantes <= 30) { badgeTexto = diasRestantes + " días hasta " + fechaFin; badgeColor = "#F59E0B"; }
            else { badgeTexto = "Bloqueado (" + diasRestantes + " días)"; badgeColor = "#9CA3AF"; }


            CardView bDis = buildBtnCard(badgeTexto, "#E5E7EB", Color.parseColor(badgeColor));
            LinearLayout.LayoutParams dp2 = new LinearLayout.LayoutParams(-1, dpToPx(42));
            dp2.topMargin = dpToPx(12);
            bDis.setLayoutParams(dp2);
            bDis.setClickable(false);
            inner.addView(bDis);

            // ── CASO C: SIN MEMBRESÍA (primera vez o período terminado) ─────────
        } else {
            banner.setCardBackgroundColor(Color.parseColor("#F0F4FF"));

            inner.addView(mkTv("Sin membresía", 13f, "#0D1B3E", true));

            TextView tvInfo = mkTv("Este cliente no tiene membresía activa.\nPuede inscribirle desde el panel principal.",
                    11f, "#6B7FA3", false);
            LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(-1, -2);
            ip.topMargin = dpToPx(4);
            tvInfo.setLayoutParams(ip);
            inner.addView(tvInfo);

            CardView bInscribir = buildBtnCard("➕ Inscribir membresía", "#0A66FF", Color.WHITE);
            LinearLayout.LayoutParams bip = new LinearLayout.LayoutParams(-1, dpToPx(44));
            bip.topMargin = dpToPx(12);
            bInscribir.setLayoutParams(bip);
            bInscribir.setOnClickListener(v -> {
                sheet.dismiss();
                Intent iMain = new Intent(this, MainActivity.class);
                iMain.putExtra("ABRIR_INSCRIPCION", true);
                iMain.putExtra("CLIENTE_ID", cliente.id);
                iMain.putExtra("CLIENTE_NOMBRE", cliente.nombreCompleto());
                iMain.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(iMain);
            });
            inner.addView(bInscribir);
        }

        banner.addView(inner);
        cont.addView(banner);
    }




    private void confirmarCancelacionMembresia(MembresiaModel mem, Cliente cliente,
                                               BottomSheetDialog sheetPadre) {
        BottomSheetDialog dlg = new BottomSheetDialog(this, com.google.android.material.R.style.Theme_Material3_Light_BottomSheetDialog);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.WHITE);
        root.setPadding(dpToPx(24), dpToPx(24), dpToPx(24), dpToPx(48));
        root.addView(mkHandle());

        android.widget.ImageView ivCancel = new android.widget.ImageView(this);
        LinearLayout.LayoutParams eP = new LinearLayout.LayoutParams(dpToPx(52), dpToPx(52));
        eP.gravity = Gravity.CENTER_HORIZONTAL;
        eP.bottomMargin = dpToPx(10);
        ivCancel.setLayoutParams(eP);
        ivCancel.setImageResource(R.drawable.ic_cancel);
        ivCancel.setColorFilter(Color.parseColor("#EF4444"));
        root.addView(ivCancel);

        TextView tvTit = mkTv("Cancelar membresía", 20f, "#0D1B3E", true);
        tvTit.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams tP = new LinearLayout.LayoutParams(-1, -2);
        tP.bottomMargin = dpToPx(8);
        tvTit.setLayoutParams(tP);
        root.addView(tvTit);

        String fechaFinReal = mem.calcularFechaFin();
        if (fechaFinReal.isEmpty()) fechaFinReal = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        int diasEspera = 0;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            java.util.Date dFin = sdf.parse(fechaFinReal);
            java.util.Calendar calFin = java.util.Calendar.getInstance();
            calFin.setTime(dFin);
            java.util.Calendar hoy = java.util.Calendar.getInstance();
            long diffMs = calFin.getTimeInMillis() - hoy.getTimeInMillis();
            diasEspera = Math.max(0, (int) (diffMs / (1000 * 60 * 60 * 24)));
        } catch (Exception e) { diasEspera = 0; }

        String tipoMembresia = mem.tipo != null ? mem.tipo : "Mensual";
        String mensajeEspera = diasEspera > 0
                ? "El cliente deberá esperar " + diasEspera + " día(s) hasta el " + formatFecha(fechaFinReal) + " para reinscribirse."
                : "El cliente podrá reinscribirse inmediatamente.";

        TextView tvDesc = mkTv("Se cancelará la membresía " + tipoMembresia + " de " + cliente.nombreCompleto() + ".\n\n" +
                "• La membresía permanecerá activa hasta: " + formatFecha(fechaFinReal) + "\n" +
                "• " + mensajeEspera + "\n" +
                "• El cobro del período actual quedará como PENDIENTE (si no se ha cobrado ya).\n\n" +
                "¿Confirmas la cancelación?", 13f, "#6B7FA3", false);
        tvDesc.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams dP = new LinearLayout.LayoutParams(-1, -2);
        dP.bottomMargin = dpToPx(24);
        tvDesc.setLayoutParams(dP);
        root.addView(tvDesc);

        LinearLayout btns = new LinearLayout(this);
        btns.setOrientation(LinearLayout.HORIZONTAL);
        btns.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));

        TextView btnNo = buildTextBtn("No, mantener", "#EEF4FF", "#0A66FF");
        LinearLayout.LayoutParams noP = new LinearLayout.LayoutParams(0, -2, 1f);
        noP.setMarginEnd(dpToPx(10));
        btnNo.setLayoutParams(noP);
        btnNo.setOnClickListener(v -> dlg.dismiss());
        btns.addView(btnNo);

        TextView btnSi = buildTextBtn("Sí, cancelar", "#EF4444", "#FFFFFF");
        btnSi.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
        final String fechaFinFinal = fechaFinReal;
        btnSi.setOnClickListener(v -> {
            btnSi.setEnabled(false);
            btnSi.setText("Cancelando...");
            realizarCancelacionMembresia(mem, cliente, fechaFinFinal, dlg, sheetPadre);
        });
        btns.addView(btnSi);
        root.addView(btns);

        dlg.setContentView(root);
        dlg.show();
    }

    /**
     * Ejecuta la cancelación de membresía con sincronización completa:
     * 1. Marca activa=false, fecha_fin=hoy en Supabase
     * 2. Verifica si ya existe cobro de membresía del mes actual
     * 3. Si NO existe → crea cobro pendiente
     * 4. Actualiza TODOS los estados locales
     * 5. Re-renderiza la lista y cierra el sheet
     */
    private void realizarCancelacionMembresia(MembresiaModel mem, Cliente cliente,
                                              String fechaFin,
                                              BottomSheetDialog dlgConfirm,
                                              BottomSheetDialog sheetPadre) {
        // Bloquear generación automática mientras cancelamos
        cancelandoMembresia = true;

        // Paso 1: Cancelar en Supabase
        SupabaseRepository.get().cancelarMembresia(mem.id, fechaFin,
                new SupabaseRepository.Callback<Void>() {
                    @Override public void onSuccess(Void d) {
                        // Paso 2: Actualizar objeto de membresía local
                        mem.activa = false;
                        mem.fechaFin = fechaFin;

                        // Paso 3: Verificar si ya existe un cobro PENDIENTE de membresía
                        SupabaseRepository.get().getCobrosPorCliente(mem.clienteId,
                                new SupabaseRepository.Callback<List<CobroModel>>() {
                                    @Override public void onSuccess(List<CobroModel> cobros) {
                                        String tipoReal = (mem.tipo != null && !mem.tipo.isEmpty())
                                                ? mem.tipo.toLowerCase() : "mensual";

                                        // Buscar CUALQUIER cobro pendiente de membresía (sin importar la fecha)
                                        boolean yaExistePendiente = false;
                                        for (CobroModel co : cobros) {
                                            // Solo considerar cobros PENDIENTES
                                            if (!"pendiente".equals(co.estado)) continue;

                                            // Verificar si es un cobro de membresía del tipo correcto
                                            boolean esMembresia = co.concepto != null
                                                    && co.concepto.toLowerCase().contains("membresía")
                                                    && co.concepto.toLowerCase().contains(tipoReal);

                                            if (esMembresia) {
                                                yaExistePendiente = true;
                                                android.util.Log.d("CANCELACION",
                                                        "✓ Ya existe cobro pendiente: " + co.concepto +
                                                                " (" + co.importe + "€) - NO se creará duplicado");
                                                break;
                                            }
                                        }

                                        if (!yaExistePendiente) {
                                            // NO existe cobro pendiente → crear uno nuevo
                                            String concepto = "Membresía " + tipoReal + " (cancelación) · " + cliente.nombreCompleto();
                                            android.util.Log.d("CANCELACION",
                                                    "✓ No hay cobro pendiente - creando: " + concepto);

                                            SupabaseRepository.get().crearCobro(
                                                    mem.clienteId,
                                                    cliente.nombreCompleto(),
                                                    concepto,
                                                    mem.precio,
                                                    "Efectivo",
                                                    "pendiente",
                                                    "Generado automáticamente al cancelar membresía el " + fechaFin,
                                                    new SupabaseRepository.Callback<CobroModel>() {
                                                        @Override public void onSuccess(CobroModel c) {
                                                            //finalizarCancelacionConCobro(cliente, mem, dlgConfirm, sheetPadre,
                                                            // "✅ Membresía cancelada · cobro de " +
                                                            //  (int)mem.precio + "€ generado como pendiente");
                                                        }
                                                        @Override public void onError(String e) {
                                                            finalizarCancelacionConCobro(cliente, mem, dlgConfirm, sheetPadre,
                                                                    "Membresía cancelada · error al generar cobro: " + e);
                                                        }
                                                    });
                                        } else {
                                            // Ya existe cobro pendiente → NO crear duplicado
                                            finalizarCancelacionConCobro(cliente, mem, dlgConfirm, sheetPadre,
                                                    "Membresía cancelada · ya tenía cobro pendiente");
                                        }
                                    }
                                    @Override public void onError(String e) {
                                        finalizarCancelacionConCobro(cliente, mem, dlgConfirm, sheetPadre,
                                                "Membresía cancelada");
                                    }
                                });
                    }
                    @Override public void onError(String e) {
                        runOnUiThread(() -> {
                            Toast.makeText(ClientesActivity.this,
                                    "Error al cancelar: " + e, Toast.LENGTH_LONG).show();
                            dlgConfirm.dismiss();
                        });
                    }
                });
    }


    private void finalizarCancelacionConCobro(Cliente cliente, MembresiaModel memCancelada,
                                              BottomSheetDialog dlgConfirm,
                                              BottomSheetDialog sheetPadre, String mensaje) {
        runOnUiThread(() -> {
            // Actualizar estado del cliente local
            cliente.membresiaActiva = null;
            cliente.tieneMembresia  = false;
            cliente.ultimaMembresia = memCancelada;
            cliente.membresiasCargadas = true;

            // Cerrar diálogos
            dlgConfirm.dismiss();
            sheetPadre.dismiss();

            // Log para debug
            android.util.Log.d("CANCELACION",
                    "Membresía cancelada para " + cliente.nombreCompleto() +
                            " - Fecha fin: " + memCancelada.fechaFin);

            // Refrescar deudas (incluye el cobro recién creado)
            cargarDeudasYCitasReales();

            // Permitir generación automática de nuevo antes de refrescar
            cancelandoMembresia = false;

            // CRÍTICO: Refrescar membresías de TODOS los clientes
            refrescarMembresiasCompleto();

            // Mostrar mensaje
            Toast.makeText(ClientesActivity.this, mensaje, Toast.LENGTH_LONG).show();
        });
    }

    private void showEditarCuotaSheet(MembresiaModel mem) {
        BottomSheetDialog s2 = new BottomSheetDialog(this,
                com.google.android.material.R.style.Theme_Material3_Light_BottomSheetDialog);
        LinearLayout root2 = new LinearLayout(this);
        root2.setOrientation(LinearLayout.VERTICAL);
        root2.setBackgroundColor(Color.WHITE);
        root2.setPadding(dpToPx(20), dpToPx(24), dpToPx(20), dpToPx(48));
        root2.addView(mkHandle());

        root2.addView(mkTv("Editar cuota mensual", 18f, "#0D1B3E", true));

        final double[] np = {mem.precio};
        LinearLayout rowP = new LinearLayout(this);
        rowP.setOrientation(LinearLayout.HORIZONTAL);
        rowP.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rpp = new LinearLayout.LayoutParams(-1, -2);
        rpp.topMargin = dpToPx(24); rpp.bottomMargin = dpToPx(32);
        rowP.setLayoutParams(rpp);

        CardView bm = btnCircle("−");
        rowP.addView(bm);

        final TextView tvPrecio2 = new TextView(this);
        tvPrecio2.setText(String.format("%.0f€", mem.precio));
        tvPrecio2.setTextSize(40f);
        tvPrecio2.setTextColor(Color.parseColor("#0A66FF"));
        tvPrecio2.setTypeface(getResources().getFont(R.font.outfit_bold));
        tvPrecio2.setGravity(Gravity.CENTER);
        tvPrecio2.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
        rowP.addView(tvPrecio2);

        CardView bma = btnCircle("+");
        rowP.addView(bma);

        bm.setOnClickListener(v -> {
            if (np[0] > 5) { np[0] -= 5; tvPrecio2.setText(String.format("%.0f€", np[0])); }
        });
        bma.setOnClickListener(v -> {
            if (np[0] < 500) { np[0] += 5; tvPrecio2.setText(String.format("%.0f€", np[0])); }
        });
        root2.addView(rowP);

        CardView btnSave = new CardView(this);
        btnSave.setLayoutParams(new LinearLayout.LayoutParams(-1, dpToPx(54)));
        btnSave.setRadius(dpToPx(16));
        btnSave.setCardElevation(dpToPx(3));
        btnSave.setCardBackgroundColor(Color.parseColor("#0A66FF"));
        TextView tvSave = mkTv("Guardar nueva cuota", 14f, "#FFFFFF", true);
        tvSave.setGravity(Gravity.CENTER);
        tvSave.setLayoutParams(new LinearLayout.LayoutParams(-1, -1));
        btnSave.addView(tvSave);
        btnSave.setClickable(true);
        btnSave.setOnClickListener(v -> {
            if (mem.id == null) return;
            tvSave.setText("Guardando...");
            btnSave.setClickable(false);
            SupabaseRepository.get().actualizarPrecioMembresia(mem.id, np[0],
                    new SupabaseRepository.Callback<Void>() {
                        @Override public void onSuccess(Void d) {
                            runOnUiThread(() -> {
                                mem.precio = np[0];
                                s2.dismiss();
                                Toast.makeText(ClientesActivity.this,
                                        "Cuota actualizada: " + (int) np[0] + "€/mes",
                                        Toast.LENGTH_SHORT).show();
                            });
                        }
                        @Override public void onError(String e) {
                            runOnUiThread(() -> {
                                tvSave.setText("Guardar nueva cuota");
                                btnSave.setClickable(true);
                                Toast.makeText(ClientesActivity.this,
                                        "Error: " + e, Toast.LENGTH_SHORT).show();
                            });
                        }
                    });
        });
        root2.addView(btnSave);
        s2.setContentView(root2);
        s2.show();
    }


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

        CardView btnGuardarCard = view.findViewById(R.id.btnGuardarCliente);
        TextView btnGuardarTv = view.findViewById(R.id.tvGuardarCliente);

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
            String nombre = etNombre.getText().toString().trim();
            if (nombre.isEmpty()) { etNombre.setError("Obligatorio"); return; }

            String apellidos = etApellidos.getText().toString().trim();
            String telefono  = etTelefono.getText().toString().trim();
            String email     = etEmail.getText().toString().trim();
            String notas     = etNotas.getText().toString().trim();

            btnGuardarCard.setEnabled(false);
            btnGuardarTv.setText("Guardando...");

            if (esEdicion && clienteEditar.id != null) {
                Map<String, Object> campos = new HashMap<>();
                campos.put("nombre", nombre); campos.put("apellidos", apellidos);
                campos.put("telefono", telefono); campos.put("email", email);
                campos.put("notas", notas);
                SupabaseRepository.get().actualizarCliente(clienteEditar.id, campos,
                        new SupabaseRepository.Callback<Void>() {
                            @Override public void onSuccess(Void data) {
                                runOnUiThread(() -> {
                                    clienteEditar.nombre    = nombre;
                                    clienteEditar.apellidos = apellidos;
                                    clienteEditar.telefono  = telefono;
                                    clienteEditar.email     = email;
                                    clienteEditar.notas     = notas;
                                    sheet.dismiss(); ocultarTeclado(); renderLista();
                                    Toast.makeText(ClientesActivity.this,
                                            "Cliente actualizado", Toast.LENGTH_SHORT).show();
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
                ClienteModel modelo = new ClienteModel();
                modelo.nombre = nombre; modelo.apellidos = apellidos;
                modelo.telefono = telefono; modelo.email = email;
                modelo.notas = notas; modelo.estado = "activo"; modelo.saldo = 0;
                SupabaseRepository.get().crearCliente(modelo,
                        new SupabaseRepository.Callback<ClienteModel>() {
                            @Override public void onSuccess(ClienteModel data) {
                                runOnUiThread(() -> {
                                    todosLosClientes.add(0, new Cliente(data));
                                    sheet.dismiss(); ocultarTeclado();
                                    cargarMembresiasEnBackground();
                                    Toast.makeText(ClientesActivity.this,
                                            "Cliente añadido: " + nombre, Toast.LENGTH_SHORT).show();
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


    private void confirmarEliminar(Cliente cliente) {
        BottomSheetDialog sheet = new BottomSheetDialog(
                this, com.google.android.material.R.style.Theme_Material3_Light_BottomSheetDialog);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.WHITE);
        root.setPadding(dpToPx(24), dpToPx(20), dpToPx(24), dpToPx(44));
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(mkHandle());

        android.widget.ImageView ivDelete = new android.widget.ImageView(this);
        LinearLayout.LayoutParams eP = new LinearLayout.LayoutParams(dpToPx(52), dpToPx(52));
        eP.gravity = Gravity.CENTER_HORIZONTAL;
        eP.bottomMargin = dpToPx(12);
        ivDelete.setLayoutParams(eP);
        ivDelete.setImageResource(R.drawable.ic_delete);
        ivDelete.setColorFilter(Color.parseColor("#EF4444"));
        root.addView(ivDelete);

        TextView tvTitulo = mkTv("Eliminar cliente", 20f, "#0D1B3E", true);
        tvTitulo.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams tP = new LinearLayout.LayoutParams(-1, -2);
        tP.bottomMargin = dpToPx(8);
        tvTitulo.setLayoutParams(tP);
        root.addView(tvTitulo);

        TextView tvDesc = mkTv(
                "¿Eliminar a " + cliente.nombreCompleto() + "?\nEsta acción no se puede deshacer.",
                13f, "#6B7FA3", false);
        tvDesc.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams dP = new LinearLayout.LayoutParams(-1, -2);
        dP.bottomMargin = dpToPx(28);
        tvDesc.setLayoutParams(dP);
        root.addView(tvDesc);

        LinearLayout btns = new LinearLayout(this);
        btns.setOrientation(LinearLayout.HORIZONTAL);
        btns.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));

        TextView btnCancelar = buildTextBtn("Cancelar", "#EEF4FF", "#0A66FF");
        LinearLayout.LayoutParams cP = new LinearLayout.LayoutParams(0, -2, 1f);
        cP.setMarginEnd(dpToPx(10));
        btnCancelar.setLayoutParams(cP);
        btnCancelar.setOnClickListener(v -> sheet.dismiss());
        btns.addView(btnCancelar);

        TextView btnEliminar = buildTextBtn("Eliminar", "#EF4444", "#FFFFFF");
        btnEliminar.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
        btnEliminar.setOnClickListener(v -> {
            if (cliente.id == null) {
                todosLosClientes.remove(cliente); sheet.dismiss(); renderLista(); return;
            }
            btnEliminar.setEnabled(false);
            btnEliminar.setText("Eliminando...");
            SupabaseRepository.get().eliminarClienteEnCascada(cliente.id,
                    new SupabaseRepository.Callback<Void>() {
                        @Override public void onSuccess(Void data) {
                            runOnUiThread(() -> {
                                todosLosClientes.remove(cliente);
                                sheet.dismiss(); renderLista();
                                Toast.makeText(ClientesActivity.this,
                                        cliente.nombreCompleto() + " eliminado (con sus cobros y membresías)",
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


    private void setupBotones() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnAddCliente).setOnClickListener(v -> showFormularioCliente(null));
    }

    private void setupBottomNav() { NavHelper.setup(this, "clientes"); }


    private void cargarDeudasYCitasReales() {
        SupabaseRepository.get().getCobros("eq.pendiente",
                new SupabaseRepository.Callback<List<CobroModel>>() {
                    @Override public void onSuccess(List<CobroModel> cobros) {
                        Map<String, Double> deudas = new HashMap<>();
                        for (CobroModel c : cobros) {
                            if (c.clienteId == null) continue;
                            deudas.put(c.clienteId,
                                    deudas.getOrDefault(c.clienteId, 0.0) + c.importe);
                        }
                        for (Cliente cl : todosLosClientes) {
                            cl.deudaReal = deudas.getOrDefault(cl.id, 0.0);
                        }
                        runOnUiThread(() -> renderLista());
                    }
                    @Override public void onError(String e) {
                        android.util.Log.e("DEUDAS", "Error: " + e);
                    }
                });

        SupabaseRepository.get().getCitasRango("2020-01-01", "2030-12-31",
                new SupabaseRepository.Callback<List<CitaModel>>() {
                    @Override public void onSuccess(List<CitaModel> citas) {
                        Map<String, int[]> mapa = new HashMap<>();
                        for (CitaModel c : citas) {
                            if (c.clienteId == null) continue;
                            int[] vv = mapa.getOrDefault(c.clienteId, new int[]{0, 0});
                            vv[0]++;
                            if ("pendiente".equals(c.estado) || "confirmada".equals(c.estado)) vv[1]++;
                            mapa.put(c.clienteId, vv);
                        }
                        for (Cliente cl : todosLosClientes) {
                            int[] vv = mapa.getOrDefault(cl.id, new int[]{0, 0});
                            cl.citasReales = vv[0];
                            cl.citasProx   = vv[1];
                        }
                        runOnUiThread(() -> renderLista());
                    }
                    @Override public void onError(String e) {
                        android.util.Log.e("CITAS", "Error: " + e);
                    }
                });
    }


    private android.view.ViewGroup encontrarContenedor(View root) {
        if (root instanceof LinearLayout) return (android.view.ViewGroup) root;
        if (root instanceof ScrollView) {
            ScrollView sv = (ScrollView) root;
            if (sv.getChildCount() > 0 && sv.getChildAt(0) instanceof LinearLayout)
                return (LinearLayout) sv.getChildAt(0);
        }
        if (root instanceof androidx.core.widget.NestedScrollView) {
            androidx.core.widget.NestedScrollView nsv = (androidx.core.widget.NestedScrollView) root;
            if (nsv.getChildCount() > 0 && nsv.getChildAt(0) instanceof LinearLayout)
                return (LinearLayout) nsv.getChildAt(0);
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

    private View mkSeparador() {
        View sep = new View(this);
        sep.setBackgroundColor(Color.parseColor("#DDE6FF"));
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1, dpToPx(1));
        sp.setMargins(0, dpToPx(8), 0, dpToPx(8));
        sep.setLayoutParams(sp);
        return sep;
    }

    private View mkBtnSheet(String texto, String bgColor, String textColor,
                            View.OnClickListener listener) {
        CardView btn = new CardView(this);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dpToPx(52));
        p.bottomMargin = dpToPx(8);
        btn.setLayoutParams(p);
        btn.setRadius(dpToPx(14));
        btn.setCardElevation(dpToPx(1));
        btn.setCardBackgroundColor(Color.parseColor(bgColor));
        TextView tv = mkTv(texto, 13f, textColor, true);
        tv.setGravity(Gravity.CENTER);
        tv.setLayoutParams(new LinearLayout.LayoutParams(-1, -1));
        btn.addView(tv);
        btn.setClickable(true);
        btn.setOnClickListener(listener);
        return btn;
    }

    private CardView buildBtnCard(String texto, String bgHex, int textColor) {
        CardView c = new CardView(this);
        c.setRadius(dpToPx(10));
        c.setCardElevation(0);
        c.setCardBackgroundColor(Color.parseColor(bgHex));
        c.setClickable(true);
        TextView tv = new TextView(this);
        tv.setText(texto);
        tv.setTextSize(12f);
        tv.setTextColor(textColor);
        tv.setTypeface(getResources().getFont(R.font.outfit_bold));
        tv.setGravity(Gravity.CENTER);
        tv.setLayoutParams(new LinearLayout.LayoutParams(-1, -1));
        c.addView(tv);
        return c;
    }

    private CardView btnCircle(String label) {
        CardView c = new CardView(this);
        c.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(52), dpToPx(52)));
        c.setRadius(dpToPx(12));
        c.setCardElevation(dpToPx(1));
        c.setCardBackgroundColor(Color.parseColor("#EEF4FF"));
        TextView tv = mkTv(label, 26f, "#0A66FF", true);
        tv.setGravity(Gravity.CENTER);
        tv.setLayoutParams(new LinearLayout.LayoutParams(-1, -1));
        c.addView(tv);
        c.setClickable(true);
        return c;
    }

    private TextView buildTextBtn(String texto, String bgHex, String textHex) {
        TextView tv = new TextView(this);
        tv.setText(texto);
        tv.setTextSize(14f);
        tv.setTextColor(Color.parseColor(textHex));
        tv.setTypeface(getResources().getFont(R.font.outfit_bold));
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(0, dpToPx(14), 0, dpToPx(14));
        android.graphics.drawable.GradientDrawable bg =
                new android.graphics.drawable.GradientDrawable();
        bg.setColor(Color.parseColor(bgHex));
        bg.setCornerRadius(dpToPx(16));
        tv.setBackground(bg);
        return tv;
    }

    private TextView mkTv(String texto, float size, String colorHex, boolean bold) {
        TextView tv = new TextView(this);
        tv.setText(texto);
        tv.setTextSize(size);
        tv.setTextColor(Color.parseColor(colorHex));
        tv.setTypeface(getResources().getFont(bold ? R.font.outfit_bold : R.font.outfit_regular));
        return tv;
    }

    private View mkHandle() {
        View h = new View(this);
        android.graphics.drawable.GradientDrawable hBg =
                new android.graphics.drawable.GradientDrawable();
        hBg.setColor(Color.parseColor("#DDE6FF"));
        hBg.setCornerRadius(dpToPx(4));
        h.setBackground(hBg);
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(dpToPx(40), dpToPx(4));
        hp.gravity = Gravity.CENTER_HORIZONTAL;
        hp.bottomMargin = dpToPx(24);
        h.setLayoutParams(hp);
        return h;
    }

    /** "yyyy-MM-dd" → "dd/MM/yyyy" */
    private String formatFecha(String iso) {
        if (iso == null || iso.length() < 10) return iso != null ? iso : "—";
        return iso.substring(8, 10) + "/" + iso.substring(5, 7) + "/" + iso.substring(0, 4);
    }

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