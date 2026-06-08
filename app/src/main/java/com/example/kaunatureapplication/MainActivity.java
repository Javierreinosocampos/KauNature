package com.example.kaunatureapplication;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    // ── Paleta ───────────────────────────────────────────────────────
    private static final int WHITE   = 0xFFFFFFFF;
    private static final int BG      = 0xFFF4F6FF;
    private static final int BLUE    = 0xFF2563EB;
    private static final int BLUE_XL = 0xFFEEF4FF;
    private static final int TEXT_D  = 0xFF0D1B3E;
    private static final int TEXT_M  = 0xFF4B5563;
    private static final int TEXT_L  = 0xFF9CA3AF;
    private static final int BORDER  = 0xFFE5EDFF;
    private static final int GREEN   = 0xFF10B981;
    private static final int YELLOW  = 0xFFF59E0B;
    private static final int RED     = 0xFFEF4444;

    // ── Views ────────────────────────────────────────────────────────
    private TextView     tvGreeting, tvFechaHoy;
    private TextView     tvKpiCitas, tvKpiGym, tvKpiIngresos;
    private LinearLayout listaCitas, listaPagos;

    // ── Datos en memoria (KPIs cargados de Supabase) ─────────────────
    private final List<CitaModel>  citasHoy       = new ArrayList<>();
    private final List<CobroModel> cobrosPendientes = new ArrayList<>();

    // ── Notificaciones (locales, se generan desde los datos reales) ───
    static class Notif {
        String tipo, titulo, desc, hora, id;
        boolean leida;
        Notif(String tipo, String id, String titulo, String desc, String hora) {
            this.tipo = tipo; this.id = id; this.titulo = titulo;
            this.desc = desc; this.hora = hora;
        }
    }
    private final List<Notif> notificaciones = new ArrayList<>();
    // Clave de SharedPreferences para persistencia real entre Activities
    private static final String PREFS       = "kau_notifs";
    private static final String KEY_LEIDAS  = "leidas";
    private static final String KEY_ELIM    = "eliminadas";
    private static final String SEP         = "|||";

    // Conjuntos en memoria (se sincronizan con SharedPreferences)
    private java.util.Set<String> notifsLeidas    = new java.util.HashSet<>();
    private java.util.Set<String> notifsEliminadas = new java.util.HashSet<>();

    // ── Helpers SharedPreferences ─────────────────────────────────
    private void cargarPrefs() {
        android.content.SharedPreferences p = getSharedPreferences(PREFS, MODE_PRIVATE);
        notifsLeidas    = new java.util.HashSet<>(
                java.util.Arrays.asList(p.getString(KEY_LEIDAS, "").split("\\|\\|\\|")));
        notifsEliminadas = new java.util.HashSet<>(
                java.util.Arrays.asList(p.getString(KEY_ELIM, "").split("\\|\\|\\|")));
        notifsLeidas.remove("");     // quitar el string vacío si no había nada
        notifsEliminadas.remove("");
    }

    private void guardarLeidas() {
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .putString(KEY_LEIDAS, String.join(SEP, notifsLeidas))
                .apply();
    }

    private void guardarEliminadas() {
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .putString(KEY_ELIM, String.join(SEP, notifsEliminadas))
                .apply();
    }

    private void marcarLeida(String id) {
        notifsLeidas.add(id);
        guardarLeidas();
    }

    private void marcarEliminada(String id) {
        notifsEliminadas.add(id);
        guardarEliminadas();
    }

    // ════════════════════════════════════════════════════════════════
    //  Lifecycle
    // ══════════════════════════════════════════════════════════════════
// MainActivity.java - CAMBIOS APLICADOS
// ══════════════════════════════════════════════════════════════════
//
// CAMBIO 1: Agregar en onCreate() después de cargarDatos()
// CAMBIO 2: Agregar método showInscribirSheetConDatos()
//
    // Author - Javier Reinoso Campos
// ══════════════════════════════════════════════════════════════════

// DENTRO DEL MÉTODO onCreate(), después de la línea cargarDatos();

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().getDecorView().setSystemUiVisibility(
                android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                        android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        );
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        setContentView(R.layout.activity_main);
        bind();
        setupGreeting();
        setupFecha();
        NavHelper.setup(this, "home");
        bindButtons();
        cargarPrefs();
        mostrarKpisVacios();
        cargarDatos();

        // ══════ NUEVO: Manejar Intent de inscripción desde ClientesActivity ══════
        Intent intent = getIntent();
        if (intent != null && intent.getBooleanExtra("ABRIR_INSCRIPCION", false)) {
            final String clienteId = intent.getStringExtra("CLIENTE_ID");
            final String clienteNombre = intent.getStringExtra("CLIENTE_NOMBRE");

            // Delay para que la UI cargue primero
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                showInscribirSheetConDatos(clienteId, clienteNombre);
            }, 300);

            // Limpiar el intent para que no se reabra al rotar pantalla
            getIntent().removeExtra("ABRIR_INSCRIPCION");
        }
    }

// ══════════════════════════════════════════════════════════════════
// NUEVO MÉTODO: showInscribirSheetConDatos
// Agregar DESPUÉS del método showInscribirSheet() existente
// ══════════════════════════════════════════════════════════════════

    /**
     * Abre el sheet de inscripción con el cliente ya pre-seleccionado.
     * Llamado desde ClientesActivity cuando el usuario pulsa "Inscribir membresía".
     */
    private void showInscribirSheetConDatos(final String clienteIdPreseleccionado,
                                            final String clienteNombrePreseleccionado) {
        if (clienteIdPreseleccionado == null || clienteNombrePreseleccionado == null) {
            showInscribirSheet();
            return;
        }

        BottomSheetDialog sheet = new BottomSheetDialog(this,
                com.google.android.material.R.style.Theme_Material3_Light_BottomSheetDialog);

        LinearLayout raiz = new LinearLayout(this);
        raiz.setOrientation(LinearLayout.VERTICAL);
        raiz.setBackgroundColor(WHITE);
        int alturaSheet = (int)(getResources().getDisplayMetrics().heightPixels * 0.85f);
        raiz.setLayoutParams(new LinearLayout.LayoutParams(-1, alturaSheet));

        // Handle
        LinearLayout hw = new LinearLayout(this);
        hw.setGravity(android.view.Gravity.CENTER_HORIZONTAL);
        LinearLayout.LayoutParams hwP = new LinearLayout.LayoutParams(-1, -2);
        hwP.topMargin = dp(10); hwP.bottomMargin = dp(4);
        hw.setLayoutParams(hwP);
        android.view.View handle = new android.view.View(this);
        android.graphics.drawable.GradientDrawable hBg = new android.graphics.drawable.GradientDrawable();
        hBg.setColor(BORDER); hBg.setCornerRadius(dp(3));
        handle.setBackground(hBg);
        handle.setLayoutParams(new LinearLayout.LayoutParams(dp(40), dp(4)));
        hw.addView(handle); raiz.addView(hw);

        // Título
        TextView tvTitulo = new TextView(this);
        tvTitulo.setText("Inscribir cliente");
        tvTitulo.setTextSize(20f); tvTitulo.setTextColor(TEXT_D);
        tvTitulo.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams titP = new LinearLayout.LayoutParams(-1, -2);
        titP.setMargins(dp(20), dp(12), dp(20), dp(4));
        tvTitulo.setLayoutParams(titP);
        raiz.addView(tvTitulo);

        TextView tvSub = new TextView(this);
        tvSub.setText("Crea una membresía mensual y genera el cobro automáticamente");
        tvSub.setTextSize(12f); tvSub.setTextColor(TEXT_L);
        LinearLayout.LayoutParams subP = new LinearLayout.LayoutParams(-1, -2);
        subP.setMargins(dp(20), 0, dp(20), dp(16));
        tvSub.setLayoutParams(subP);
        raiz.addView(tvSub);

        android.view.View div0 = new android.view.View(this);
        div0.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(1)));
        div0.setBackgroundColor(BORDER);
        raiz.addView(div0);

        androidx.core.widget.NestedScrollView nsv = new androidx.core.widget.NestedScrollView(this);
        nsv.setLayoutParams(new LinearLayout.LayoutParams(-1, 0, 1f));
        nsv.setFillViewport(true);

        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(20), dp(16), dp(20), dp(40));
        nsv.addView(form);
        raiz.addView(nsv);

        // Estado compartido
        final String[] clienteIdSel  = {clienteIdPreseleccionado};
        final String[] clienteNomSel = {clienteNombrePreseleccionado};
        final double[] precioSel     = {35.0}; // ← precio inicial actualizado a 2h

        // Referencias a los EditText de fecha — accesibles desde el listener
        final android.widget.EditText[] etDiaRef  = {null};
        final android.widget.EditText[] etMesRef  = {null};
        final android.widget.EditText[] etAnioRef = {null};

        // ── Campo cliente ────────────────────────────────────────────
        lbl(form, "Cliente *");
        TextView tvClienteSeleccionado = new TextView(this);
        tvClienteSeleccionado.setText("✅ " + clienteNombrePreseleccionado);
        tvClienteSeleccionado.setTextSize(14f);
        tvClienteSeleccionado.setTextColor(Color.parseColor("#059669"));
        tvClienteSeleccionado.setTypeface(Typeface.DEFAULT_BOLD);
        tvClienteSeleccionado.setBackground(cardBg());
        tvClienteSeleccionado.setPadding(dp(14), dp(14), dp(14), dp(14));
        LinearLayout.LayoutParams tvP = new LinearLayout.LayoutParams(-1, -2);
        tvP.bottomMargin = dp(16);
        tvClienteSeleccionado.setLayoutParams(tvP);
        form.addView(tvClienteSeleccionado);

        // ── Tipo de membresía ────────────────────────────────────────
        lbl(form, "Tipo de membresía");

        // Fila 1: Mensual 2h | Mensual 3h
        LinearLayout rowTipo1 = new LinearLayout(this);
        rowTipo1.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rowT1P = new LinearLayout.LayoutParams(-1, -2);
        rowT1P.bottomMargin = dp(8);
        rowTipo1.setLayoutParams(rowT1P);
        form.addView(rowTipo1);

        // Fila 2: Mensual 4h | Mensual personalizado
        LinearLayout rowTipo2 = new LinearLayout(this);
        rowTipo2.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rowT2P = new LinearLayout.LayoutParams(-1, -2);
        rowT2P.bottomMargin = dp(16);
        rowTipo2.setLayoutParams(rowT2P);
        form.addView(rowTipo2);

        final String[] tipoSel = {"mensual_2h"}; // ← tipo inicial actualizado

        // Fila 1: 2h y 3h
        String[] tiposFila1   = {"mensual_2h", "mensual_3h"};
        String[] tiposLabels1 = {"Mensual 2h", "Mensual 3h"};
        // Fila 2: 4h y personalizado
        String[] tiposFila2   = {"mensual_4h", "mensual_personalizado"};
        String[] tiposLabels2 = {"Mensual 4h", "Mensual personalizado"};

        android.view.View[] todosChips = new android.view.View[4];

        for (int i = 0; i < tiposFila1.length; i++) {
            final String t = tiposFila1[i];
            TextView chip = chipTv(tiposLabels1[i], t.equals(tipoSel[0]));
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0, dp(44), 1f);
            if (i < tiposFila1.length - 1) cp.setMarginEnd(dp(6));
            chip.setLayoutParams(cp);
            todosChips[i] = chip;
            rowTipo1.addView(chip);
        }
        for (int i = 0; i < tiposFila2.length; i++) {
            final String t = tiposFila2[i];
            TextView chip = chipTv(tiposLabels2[i], t.equals(tipoSel[0]));
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0, dp(44), 1f);
            if (i < tiposFila2.length - 1) cp.setMarginEnd(dp(6));
            chip.setLayoutParams(cp);
            todosChips[2 + i] = chip;
            rowTipo2.addView(chip);
        }

        precioSel[0] = 35; // ← precio inicial 2h
        final TextView[] tvPrecioRef = {null};

        // ← 4 tipos con precios actualizados
        String[] todosTipos      = {"mensual_2h", "mensual_3h", "mensual_4h", "mensual_personalizado"};
        double[] preciosPorTipo  = {35, 45, 55, 0};

        for (int i = 0; i < todosChips.length; i++) {
            final String t          = todosTipos[i];
            final double precioAuto = preciosPorTipo[i];
            todosChips[i].setOnClickListener(v -> {
                tipoSel[0]   = t;
                precioSel[0] = precioAuto;
                for (android.view.View chip : todosChips) {
                    boolean sel = chip == v;
                    ((TextView) chip).setBackground(sel ? chipActiveBg() : chipInactiveBg());
                    ((TextView) chip).setTextColor(sel ? WHITE : TEXT_M);
                }
                tvPrecioRef[0].setText(precioAuto == 0 ? "0€" : (int) precioAuto + "€");
            });
        }

        // ── Precio ───────────────────────────────────────────────────
        lbl(form, "Cuota (€)");
        LinearLayout rowPrecio = new LinearLayout(this);
        rowPrecio.setOrientation(LinearLayout.HORIZONTAL);
        rowPrecio.setGravity(android.view.Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rpP = new LinearLayout.LayoutParams(-1, -2);
        rpP.bottomMargin = dp(16);
        rowPrecio.setLayoutParams(rpP);
        form.addView(rowPrecio);

        TextView btnMenos = spinBtn("−");
        final TextView tvPrecio = new TextView(this);
        tvPrecioRef[0] = tvPrecio;
        tvPrecio.setText("35€"); // ← precio inicial actualizado
        tvPrecio.setTextSize(32f); tvPrecio.setTextColor(BLUE);
        tvPrecio.setTypeface(Typeface.DEFAULT_BOLD);
        tvPrecio.setGravity(android.view.Gravity.CENTER);
        tvPrecio.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
        tvPrecio.setClickable(true); tvPrecio.setFocusable(true);
        android.graphics.drawable.GradientDrawable precioBg = new android.graphics.drawable.GradientDrawable();
        precioBg.setColor(BLUE_XL); precioBg.setCornerRadius(dp(12));
        precioBg.setStroke(dp(1), adjustAlpha(BLUE, 0.3f));
        tvPrecio.setBackground(precioBg);
        tvPrecio.setPadding(dp(12), dp(8), dp(12), dp(8));
        TextView btnMas = spinBtn("+");

        btnMenos.setOnClickListener(v -> {
            if (precioSel[0] > 1) {
                precioSel[0] = Math.max(1, precioSel[0] - 5);
                tvPrecio.setText((int) precioSel[0] + "€");
            }
        });
        btnMas.setOnClickListener(v -> {
            if (precioSel[0] < 9999) {
                precioSel[0] = Math.min(9999, precioSel[0] + 5);
                tvPrecio.setText((int) precioSel[0] + "€");
            }
        });
        tvPrecio.setOnClickListener(v -> showPrecioPicker(precioSel, tvPrecio));

        rowPrecio.addView(btnMenos);
        rowPrecio.addView(tvPrecio);
        rowPrecio.addView(btnMas);

        // ── Fecha de inicio ──────────────────────────────────────────
        lbl(form, "Fecha de inicio *");

        java.util.Calendar cal = java.util.Calendar.getInstance();

        LinearLayout rowFecha = new LinearLayout(this);
        rowFecha.setOrientation(LinearLayout.HORIZONTAL);
        rowFecha.setGravity(android.view.Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rfP = new LinearLayout.LayoutParams(-1, -2);
        rfP.bottomMargin = dp(16);
        rowFecha.setLayoutParams(rfP);
        form.addView(rowFecha);

        // Día
        android.widget.EditText etDia = new android.widget.EditText(this);
        etDia.setText(String.valueOf(cal.get(java.util.Calendar.DAY_OF_MONTH)));
        etDia.setHint("DD");
        etDia.setTextSize(16f); etDia.setTextColor(TEXT_D);
        etDia.setBackground(cardBg());
        etDia.setPadding(dp(14), dp(14), dp(14), dp(14));
        etDia.setGravity(android.view.Gravity.CENTER);
        etDia.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        etDia.setMaxLines(1);
        LinearLayout.LayoutParams diaP = new LinearLayout.LayoutParams(0, -2, 1f);
        diaP.setMarginEnd(dp(6));
        etDia.setLayoutParams(diaP);
        etDiaRef[0] = etDia;
        rowFecha.addView(etDia);

        TextView tvSlash1 = new TextView(this);
        tvSlash1.setText("/");
        tvSlash1.setTextSize(20f); tvSlash1.setTextColor(TEXT_L);
        tvSlash1.setGravity(android.view.Gravity.CENTER);
        tvSlash1.setLayoutParams(new LinearLayout.LayoutParams(-2, -2));
        rowFecha.addView(tvSlash1);

        // Mes
        android.widget.EditText etMes = new android.widget.EditText(this);
        etMes.setText(String.format(java.util.Locale.getDefault(), "%02d",
                cal.get(java.util.Calendar.MONTH) + 1));
        etMes.setHint("MM");
        etMes.setTextSize(16f); etMes.setTextColor(TEXT_D);
        etMes.setBackground(cardBg());
        etMes.setPadding(dp(14), dp(14), dp(14), dp(14));
        etMes.setGravity(android.view.Gravity.CENTER);
        etMes.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        etMes.setMaxLines(1);
        LinearLayout.LayoutParams mesP = new LinearLayout.LayoutParams(0, -2, 1f);
        mesP.setMarginStart(dp(6)); mesP.setMarginEnd(dp(6));
        etMes.setLayoutParams(mesP);
        etMesRef[0] = etMes;
        rowFecha.addView(etMes);

        TextView tvSlash2 = new TextView(this);
        tvSlash2.setText("/");
        tvSlash2.setTextSize(20f); tvSlash2.setTextColor(TEXT_L);
        tvSlash2.setGravity(android.view.Gravity.CENTER);
        tvSlash2.setLayoutParams(new LinearLayout.LayoutParams(-2, -2));
        rowFecha.addView(tvSlash2);

        // Año
        android.widget.EditText etAnio = new android.widget.EditText(this);
        etAnio.setText(String.valueOf(cal.get(java.util.Calendar.YEAR)));
        etAnio.setHint("AAAA");
        etAnio.setTextSize(16f); etAnio.setTextColor(TEXT_D);
        etAnio.setBackground(cardBg());
        etAnio.setPadding(dp(14), dp(14), dp(14), dp(14));
        etAnio.setGravity(android.view.Gravity.CENTER);
        etAnio.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        etAnio.setMaxLines(1);
        LinearLayout.LayoutParams anioP = new LinearLayout.LayoutParams(0, -2, 1.5f);
        anioP.setMarginStart(dp(6));
        etAnio.setLayoutParams(anioP);
        etAnioRef[0] = etAnio;
        rowFecha.addView(etAnio);

        // ── Notas ────────────────────────────────────────────────────
        lbl(form, "Notas (opcional)");
        android.widget.EditText etNotas = new android.widget.EditText(this);
        etNotas.setHint("Observaciones...");
        etNotas.setTextSize(13f); etNotas.setTextColor(TEXT_D);
        etNotas.setBackground(cardBg());
        etNotas.setPadding(dp(14), dp(14), dp(14), dp(14));
        etNotas.setMinLines(2); etNotas.setGravity(android.view.Gravity.TOP);
        etNotas.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        LinearLayout.LayoutParams notasP = new LinearLayout.LayoutParams(-1, -2);
        notasP.bottomMargin = dp(24);
        etNotas.setLayoutParams(notasP);
        form.addView(etNotas);

        // ── Botón guardar ────────────────────────────────────────────
        android.graphics.drawable.GradientDrawable btnSaveBg = new android.graphics.drawable.GradientDrawable();
        btnSaveBg.setColor(BLUE); btnSaveBg.setCornerRadius(dp(16));
        TextView btnGuardar = new TextView(this);
        btnGuardar.setText("🎫 Inscribir y generar cobro");
        btnGuardar.setTextSize(15f); btnGuardar.setTextColor(WHITE);
        btnGuardar.setTypeface(Typeface.DEFAULT_BOLD);
        btnGuardar.setGravity(android.view.Gravity.CENTER);
        btnGuardar.setPadding(0, dp(18), 0, dp(18));
        btnGuardar.setBackground(btnSaveBg);
        btnGuardar.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
        btnGuardar.setClickable(true);
        form.addView(btnGuardar);

        // ── Lógica de guardar ─────────────────────────────────────────
        btnGuardar.setOnClickListener(v -> {
            int dia = 1, mes = 1, anio = 2026;
            try { dia  = Integer.parseInt(etDiaRef[0].getText().toString().trim()); }  catch (Exception ignored) {}
            try { mes  = Integer.parseInt(etMesRef[0].getText().toString().trim()); }  catch (Exception ignored) {}
            try { anio = Integer.parseInt(etAnioRef[0].getText().toString().trim()); } catch (Exception ignored) {}

            dia  = Math.max(1, Math.min(dia,  31));
            mes  = Math.max(1, Math.min(mes,  12));
            anio = Math.max(2020, Math.min(anio, 2100));

            final String fechaInicio = String.format(java.util.Locale.getDefault(),
                    "%04d-%02d-%02d", anio, mes, dia);

            btnGuardar.setEnabled(false);
            btnGuardar.setText("Guardando...");

            final String clienteId  = clienteIdSel[0];
            final String clienteNom = clienteNomSel[0];
            final double precio     = precioSel[0];
            final String tipo       = tipoSel[0];
            final String notas      = etNotas.getText().toString().trim();

            inscribirConChequeo(clienteId, clienteNom, tipo, precio, fechaInicio, notas,
                    () -> sheet.dismiss(),
                    () -> {
                        btnGuardar.setEnabled(true);
                        btnGuardar.setText("🎫 Inscribir y generar cobro");
                    });
        });

        sheet.setContentView(raiz);
        sheet.setOnShowListener(d -> {
            com.google.android.material.bottomsheet.BottomSheetBehavior<?> beh =
                    com.google.android.material.bottomsheet.BottomSheetBehavior.from(
                            (android.view.View) raiz.getParent());
            beh.setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED);
            beh.setSkipCollapsed(true);
        });
        sheet.show();
    }
    @Override
    protected void onResume() {
        super.onResume();
        cargarPrefs();
        cargarDatos();
        comprobarRenovaciones();
    }

    // ════════════════════════════════════════════════════════════════
    //  BIND
    // ════════════════════════════════════════════════════════════════
    private void bind() {
        tvGreeting    = findViewById(R.id.tvGreeting);
        tvFechaHoy    = findViewById(R.id.tvFechaHoy);
        tvKpiCitas    = findViewById(R.id.tvKpiCitas);
        tvKpiGym      = findViewById(R.id.tvKpiGym);
        tvKpiIngresos = findViewById(R.id.tvKpiIngresos);
        listaCitas    = findViewById(R.id.listaCitas);
        listaPagos    = findViewById(R.id.listaPagos);
    }

    // ════════════════════════════════════════════════════════════════
    //  CARGA DESDE SUPABASE
    // ════════════════════════════════════════════════════════════════
    private void cargarDatos() {
        cargarCitasHoy();
        cargarCobrosPendientes();
        cargarGymHoy();
    }

    /** Citas de hoy → KPI + lista */
    private void cargarCitasHoy() {
        String hoy = fechaHoy();
        SupabaseRepository.get().getCitasPorFecha(hoy,
                new SupabaseRepository.Callback<List<CitaModel>>() {
                    @Override public void onSuccess(List<CitaModel> data) {
                        runOnUiThread(() -> {
                            citasHoy.clear();
                            citasHoy.addAll(data);
                            actualizarKpiCitas();
                            buildListaCitas();
                            generarNotifsCitas();
                            refreshBadge();
                        });
                    }
                    @Override public void onError(String e) {
                        // Mantener placeholders si falla
                    }
                });
    }

    /** Cobros pendientes → KPI ingresos + lista pagos */
    private void cargarCobrosPendientes() {
        SupabaseRepository.get().getCobros("eq.pendiente",
                new SupabaseRepository.Callback<List<CobroModel>>() {
                    @Override public void onSuccess(List<CobroModel> data) {
                        runOnUiThread(() -> {
                            cobrosPendientes.clear();
                            cobrosPendientes.addAll(data);
                            actualizarKpiIngresos();
                            buildListaPagos();
                            generarNotifsDeudas();
                            refreshBadge();
                        });
                    }
                    @Override public void onError(String e) { /* mantener placeholder */ }
                });
    }

    /** Personas en el gimnasio hoy */
    private void cargarGymHoy() {
        String hoy = fechaHoyISO();  // "yyyy-MM-dd"
        SupabaseRepository.get().getFranjas(new SupabaseRepository.Callback<List<FranjaModel>>() {
            @Override public void onSuccess(List<FranjaModel> franjas) {
                // dia_semana de hoy (1=Lun…7=Dom)
                int diaSem = diaSemanaHoy();
                final int[] pendientes = {0};
                final int[] total = {0};

                List<FranjaModel> franjasHoy = new ArrayList<>();
                for (FranjaModel f : franjas)
                    if (f.diaSemana == diaSem) franjasHoy.add(f);

                if (franjasHoy.isEmpty()) {
                    runOnUiThread(() -> tvKpiGym.setText("0"));
                    return;
                }
                pendientes[0] = franjasHoy.size();

                for (FranjaModel f : franjasHoy) {
                    SupabaseRepository.get().getAsistencia(hoy, f.id,
                            new SupabaseRepository.Callback<List<AsistenciaModel>>() {
                                @Override public void onSuccess(List<AsistenciaModel> a) {
                                    synchronized (MainActivity.this) {
                                        total[0] += a.size();
                                        if (--pendientes[0] <= 0)
                                            runOnUiThread(() -> tvKpiGym.setText(String.valueOf(total[0])));
                                    }
                                }
                                @Override public void onError(String e) {
                                    synchronized (MainActivity.this) {
                                        if (--pendientes[0] <= 0)
                                            runOnUiThread(() -> tvKpiGym.setText(String.valueOf(total[0])));
                                    }
                                }
                            });
                }
            }
            @Override public void onError(String e) { /* mantener placeholder */ }
        });
    }

    // ════════════════════════════════════════════════════════════════
    //  KPIs
    // ════════════════════════════════════════════════════════════════
    private void mostrarKpisVacios() {
        if (tvKpiCitas    != null) tvKpiCitas.setText("—");
        if (tvKpiIngresos != null) tvKpiIngresos.setText("—");
        if (tvKpiGym      != null) tvKpiGym.setText("—");
    }

    private void actualizarKpiCitas() {
        if (tvKpiCitas == null) return;
        tvKpiCitas.setText(String.valueOf(citasHoy.size()));
    }

    private void actualizarKpiIngresos() {
        if (tvKpiIngresos == null) return;
        // Suma el total de cobros cobrados del mes (usamos los pendientes que tenemos)
        // Para ingresos reales deberíamos pedir cobros cobrados del mes,
        // pero mostramos la deuda pendiente como referencia rápida
        double totalDeuda = 0;
        for (CobroModel c : cobrosPendientes) totalDeuda += c.importe;
        // Mostrar como "X€ pendiente"
        tvKpiIngresos.setText(String.format(java.util.Locale.US, "%.2f", totalDeuda).replace(".", ",") + "€");
    }

    // ════════════════════════════════════════════════════════════════
    //  LISTAS
    // ════════════════════════════════════════════════════════════════
    private void buildListaCitas() {
        if (listaCitas == null) return;
        listaCitas.removeAllViews();

        // Filtrar canceladas y cobradas — el dashboard solo muestra pendientes/confirmadas
        List<CitaModel> visibles = new ArrayList<>();
        for (CitaModel c : citasHoy) {
            if (!"cancelada".equals(c.estado) && !"cobrada".equals(c.estado))
                visibles.add(c);
        }

        if (visibles.isEmpty()) {
            listaCitas.addView(rowVacio("Sin citas para hoy"));
            return;
        }
        int limit = Math.min(visibles.size(), 3);
        for (int i = 0; i < limit; i++) {
            CitaModel c = visibles.get(i);
            String titulo = c.horaDisplay() + "  " + (c.clienteNombre != null ? c.clienteNombre : "");
            String sub    = (c.servicioNombre != null ? c.servicioNombre : "") + " · " + c.precioDisplay();
            // Color según estado
            int color = "cobrada".equals(c.estado) ? BLUE
                    : "confirmada".equals(c.estado) ? GREEN : GREEN;
            View row = rowItem(titulo, sub, c.precioDisplay(), color);
            // Pasar el ID de la cita para abrir el detalle directamente
            final String citaId   = c.id;
            final String citaFecha = c.fecha; // yyyy-MM-dd
            row.setOnClickListener(v -> {
                Intent intent = new Intent(this, AgendaActivity.class);
                if (citaId != null) intent.putExtra("CITA_ID", citaId);
                if (citaFecha != null) intent.putExtra("CITA_FECHA", citaFecha);
                startActivity(intent);
            });
            listaCitas.addView(row);
        }
    }

    private void buildListaPagos() {
        if (listaPagos == null) return;
        listaPagos.removeAllViews();

        if (cobrosPendientes.isEmpty()) {
            listaPagos.addView(rowVacio("Sin pagos pendientes ✅"));
            return;
        }
        int limit = Math.min(cobrosPendientes.size(), 3);
        for (int i = 0; i < limit; i++) {
            CobroModel c = cobrosPendientes.get(i);
            String titulo = c.clienteNombre != null ? c.clienteNombre : "";
            String sub    = c.concepto != null ? c.concepto : "";
            View row = rowItem(titulo, sub, c.importeFormateado(), YELLOW);
            row.setOnClickListener(v -> startActivity(new Intent(this, CobrosActivity.class)));
            listaPagos.addView(row);
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  NOTIFICACIONES (generadas desde datos reales)
    // ════════════════════════════════════════════════════════════════
    private void generarNotifsCitas() {
        notificaciones.removeIf(n -> "cita".equals(n.tipo));
        for (CitaModel c : citasHoy) {
            // ID estable: usa UUID si existe, si no usa fecha+hora
            String id = "cita_" + (c.id != null && !c.id.isEmpty()
                    ? c.id : (c.fecha + "_" + c.horaDisplay()));
            if (notifsEliminadas.contains(id)) continue;
            String titulo = "Cita · " + (c.clienteNombre != null ? c.clienteNombre.trim() : "?");
            String desc   = (c.servicioNombre != null ? c.servicioNombre : "")
                    + " a las " + c.horaDisplay() + "h";
            Notif n = new Notif("cita", id, titulo, desc, c.horaDisplay());
            n.leida = notifsLeidas.contains(id); // restaurar estado
            notificaciones.add(n);
        }
    }

    private void generarNotifsDeudas() {
        notificaciones.removeIf(n -> "pago".equals(n.tipo) || "deuda".equals(n.tipo));
        for (CobroModel c : cobrosPendientes) {
            String id = "pago_" + (c.id != null && !c.id.isEmpty() ? c.id : c.clienteNombre);
            if (notifsEliminadas.contains(id)) continue;
            String cliente = c.clienteNombre != null ? c.clienteNombre : "";
            String titulo  = "Pago pendiente · " + cliente;
            String desc    = (c.concepto != null ? c.concepto : "") + " · " + c.importeFormateado();
            Notif n = new Notif("pago", id, titulo, desc, "Hoy");
            n.leida = notifsLeidas.contains(id); // restaurar estado
            notificaciones.add(n);
        }
    }

    private void refreshBadge() {
        View badge = findViewById(R.id.badgeNotif);
        if (badge == null) return;
        long nl = notificaciones.stream().filter(n -> !n.leida).count();
        badge.setVisibility(nl > 0 ? View.VISIBLE : View.GONE);
    }

    private void showNotifs() {
        BottomSheetDialog sheet = new BottomSheetDialog(this,
                com.google.android.material.R.style.Theme_Material3_Light_BottomSheetDialog);
        sheet.getWindow().setDimAmount(0.4f);

        LinearLayout raiz = new LinearLayout(this);
        raiz.setOrientation(LinearLayout.VERTICAL);
        raiz.setBackgroundColor(WHITE);
        int alturaSheet = (int)(getResources().getDisplayMetrics().heightPixels * 0.78f);
        raiz.setLayoutParams(new LinearLayout.LayoutParams(-1, alturaSheet));

        // ── Handle ───────────────────────────────────────────────────
        LinearLayout hw = new LinearLayout(this);
        hw.setGravity(Gravity.CENTER_HORIZONTAL);
        LinearLayout.LayoutParams hwP = new LinearLayout.LayoutParams(-1, -2);
        hwP.topMargin = dp(10); hwP.bottomMargin = dp(4);
        hw.setLayoutParams(hwP);
        View handle = new View(this);
        GradientDrawable hBg = new GradientDrawable();
        hBg.setColor(0xFFD1D5DB); hBg.setCornerRadius(dp(3));
        handle.setBackground(hBg);
        handle.setLayoutParams(new LinearLayout.LayoutParams(dp(40), dp(4)));
        hw.addView(handle);
        raiz.addView(hw);

        // ── HEADER gradiente azul ────────────────────────────────────
        FrameLayout headerCard = new FrameLayout(this);
        LinearLayout.LayoutParams hcP = new LinearLayout.LayoutParams(-1, -2);
        hcP.setMargins(dp(16), dp(8), dp(16), dp(14));
        headerCard.setLayoutParams(hcP);
        GradientDrawable headerBg = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{0xFF1560E0, 0xFF0D52CC, 0xFF0840AA});
        headerBg.setCornerRadius(dp(24));
        headerCard.setBackground(headerBg);
        headerCard.setElevation(dp(8));
        headerCard.setClipToOutline(true);
        headerCard.setOutlineProvider(android.view.ViewOutlineProvider.BACKGROUND);

        // Círculo decorativo top-right
        View circR = new View(this);
        FrameLayout.LayoutParams crP = new FrameLayout.LayoutParams(dp(100), dp(100));
        crP.gravity = Gravity.TOP | Gravity.END;
        circR.setLayoutParams(crP);
        GradientDrawable circRBg = new GradientDrawable();
        circRBg.setShape(GradientDrawable.OVAL);
        circRBg.setColor(0x15FFFFFF);
        circR.setBackground(circRBg);
        circR.setTranslationX(dp(30)); circR.setTranslationY(dp(-30));
        headerCard.addView(circR);

        // Círculo decorativo bottom-left
        View circL = new View(this);
        FrameLayout.LayoutParams clP = new FrameLayout.LayoutParams(dp(70), dp(70));
        clP.gravity = Gravity.BOTTOM | Gravity.START;
        circL.setLayoutParams(clP);
        GradientDrawable circLBg = new GradientDrawable();
        circLBg.setShape(GradientDrawable.OVAL);
        circLBg.setColor(0x10FFFFFF);
        circL.setBackground(circLBg);
        circL.setTranslationX(dp(-20)); circL.setTranslationY(dp(20));
        headerCard.addView(circL);

        // Contenido del header
        LinearLayout headerContent = new LinearLayout(this);
        headerContent.setOrientation(LinearLayout.HORIZONTAL);
        headerContent.setGravity(Gravity.CENTER_VERTICAL);
        headerContent.setPadding(dp(18), dp(18), dp(18), dp(18));
        headerContent.setLayoutParams(new FrameLayout.LayoutParams(-1, -2));

        // Icono campana
        FrameLayout iconFrame = new FrameLayout(this);
        LinearLayout.LayoutParams ifP = new LinearLayout.LayoutParams(dp(44), dp(44));
        ifP.setMarginEnd(dp(14));
        iconFrame.setLayoutParams(ifP);
        GradientDrawable iconBg = new GradientDrawable();
        iconBg.setShape(GradientDrawable.OVAL);
        iconBg.setColor(0x2AFFFFFF);
        iconBg.setStroke(dp(1), 0x35FFFFFF);
        iconFrame.setBackground(iconBg);
        android.widget.ImageView ivBell = new android.widget.ImageView(this);
        FrameLayout.LayoutParams ivP = new FrameLayout.LayoutParams(dp(22), dp(22));
        ivP.gravity = Gravity.CENTER;
        ivBell.setLayoutParams(ivP);
        ivBell.setImageResource(R.drawable.ic_bell);
        ivBell.setColorFilter(WHITE);
        iconFrame.addView(ivBell);
        headerContent.addView(iconFrame);

        // Títulos
        LinearLayout titulos = new LinearLayout(this);
        titulos.setOrientation(LinearLayout.VERTICAL);
        titulos.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
        TextView tvTitulo = new TextView(this);
        tvTitulo.setText("Notificaciones");
        tvTitulo.setTextSize(16f);
        tvTitulo.setTextColor(WHITE);
        tvTitulo.setTypeface(Typeface.DEFAULT_BOLD);
        tvTitulo.setShadowLayer(dp(3), 0, dp(1), 0x35FFFFFF);
        titulos.addView(tvTitulo);
        final TextView tvSubLocal = new TextView(this);
        long nl0 = notificaciones.stream().filter(n -> !n.leida).count();
        tvSubLocal.setText(nl0 > 0 ? nl0 + " sin leer" : "Todo al día ");
        tvSubLocal.setTextSize(12f);
        tvSubLocal.setTextColor(0xFF93C5FD);
        LinearLayout.LayoutParams subP = new LinearLayout.LayoutParams(-2, -2);
        subP.topMargin = dp(2);
        tvSubLocal.setLayoutParams(subP);
        titulos.addView(tvSubLocal);
        headerContent.addView(titulos);

        // Botón papelera (eliminar todas)
        FrameLayout btnPapelera = new FrameLayout(this);
        LinearLayout.LayoutParams papP = new LinearLayout.LayoutParams(dp(44), dp(44));
        papP.setMarginEnd(dp(8));
        btnPapelera.setLayoutParams(papP);
        GradientDrawable papBg = new GradientDrawable();
        papBg.setShape(GradientDrawable.OVAL);
        papBg.setColor(0x2AFFFFFF);
        papBg.setStroke(dp(1), 0x35FFFFFF);
        btnPapelera.setBackground(papBg);
        btnPapelera.setClickable(true);
        btnPapelera.setFocusable(true);
        android.widget.ImageView ivTrash = new android.widget.ImageView(this);
        FrameLayout.LayoutParams trashP = new FrameLayout.LayoutParams(dp(22), dp(22));
        trashP.gravity = Gravity.CENTER;
        ivTrash.setLayoutParams(trashP);
        ivTrash.setImageResource(R.drawable.ic_delete);
        ivTrash.setColorFilter(WHITE);
        btnPapelera.addView(ivTrash);
        headerContent.addView(btnPapelera);

        // Botón marcar leídas
        TextView btnTodas = new TextView(this);
        btnTodas.setText("✓ Leídas");
        btnTodas.setTextSize(11f);
        btnTodas.setTextColor(WHITE);
        btnTodas.setTypeface(Typeface.DEFAULT_BOLD);
        btnTodas.setPadding(dp(12), dp(8), dp(12), dp(8));
        GradientDrawable btnBg2 = new GradientDrawable();
        btnBg2.setColor(0x2AFFFFFF);
        btnBg2.setStroke(dp(1), 0x35FFFFFF);
        btnBg2.setCornerRadius(dp(12));
        btnTodas.setBackground(btnBg2);
        btnTodas.setClickable(true);
        btnTodas.setFocusable(true);
        headerContent.addView(btnTodas);

        headerCard.addView(headerContent);
        raiz.addView(headerCard);

        // ── NestedScrollView ─────────────────────────────────────────
        androidx.core.widget.NestedScrollView nsv = new androidx.core.widget.NestedScrollView(this);
        nsv.setLayoutParams(new LinearLayout.LayoutParams(-1, 0, 1f));
        nsv.setFillViewport(true);
        nsv.setNestedScrollingEnabled(true);

        final LinearLayout listaLocal = new LinearLayout(this);
        listaLocal.setOrientation(LinearLayout.VERTICAL);
        listaLocal.setPadding(dp(16), dp(4), dp(16), dp(48));
        nsv.addView(listaLocal);
        raiz.addView(nsv);

        // ── Refresh ──────────────────────────────────────────────────
        Runnable[] rh = {null};
        Runnable refresh = () -> {
            listaLocal.removeAllViews();
            long nlr = notificaciones.stream().filter(n -> !n.leida).count();
            tvSubLocal.setText(nlr > 0 ? nlr + " sin leer" : "Todo al día ");

            if (notificaciones.isEmpty()) {
                LinearLayout emptyWrap = new LinearLayout(this);
                emptyWrap.setOrientation(LinearLayout.VERTICAL);
                emptyWrap.setGravity(Gravity.CENTER);
                LinearLayout.LayoutParams ewP = new LinearLayout.LayoutParams(-1, -2);
                ewP.setMargins(0, dp(24), 0, dp(24));
                emptyWrap.setLayoutParams(ewP);
                GradientDrawable emptyBg = new GradientDrawable();
                emptyBg.setColor(BLUE_XL);
                emptyBg.setCornerRadius(dp(22));
                emptyWrap.setBackground(emptyBg);
                emptyWrap.setPadding(dp(32), dp(36), dp(32), dp(36));

                TextView tvEmoji = new TextView(this);
                tvEmoji.setText("✓");
                tvEmoji.setTextSize(44f);
                tvEmoji.setGravity(Gravity.CENTER);
                tvEmoji.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
                emptyWrap.addView(tvEmoji);

                TextView tvEmptyTit = new TextView(this);
                tvEmptyTit.setText("Todo al día");
                tvEmptyTit.setTextSize(18f);
                tvEmptyTit.setTextColor(TEXT_D);
                tvEmptyTit.setTypeface(Typeface.DEFAULT_BOLD);
                tvEmptyTit.setGravity(Gravity.CENTER);
                LinearLayout.LayoutParams etP = new LinearLayout.LayoutParams(-1, -2);
                etP.topMargin = dp(12);
                tvEmptyTit.setLayoutParams(etP);
                emptyWrap.addView(tvEmptyTit);

                TextView tvEmptySub = new TextView(this);
                tvEmptySub.setText("No hay notificaciones pendientes");
                tvEmptySub.setTextSize(12f);
                tvEmptySub.setTextColor(TEXT_L);
                tvEmptySub.setGravity(Gravity.CENTER);
                LinearLayout.LayoutParams esP = new LinearLayout.LayoutParams(-1, -2);
                esP.topMargin = dp(4);
                tvEmptySub.setLayoutParams(esP);
                emptyWrap.addView(tvEmptySub);

                listaLocal.addView(emptyWrap);
            } else {
                for (Notif n : new ArrayList<>(notificaciones)) {
                    listaLocal.addView(buildNotifRow(n, tvSubLocal, rh));
                }
            }
        };
        rh[0] = refresh;

        btnTodas.setOnClickListener(v -> {
            for (Notif n : notificaciones) { n.leida = true; marcarLeida(n.id); }
            refreshBadge();
            refresh.run();
        });

        btnPapelera.setOnClickListener(v ->
                new android.app.AlertDialog.Builder(this)
                        .setTitle("Eliminar todas")
                        .setMessage("¿Eliminar todas las notificaciones?")
                        .setPositiveButton("Eliminar", (d, w) -> {
                            for (Notif n : new ArrayList<>(notificaciones)) { marcarEliminada(n.id); }
                            notificaciones.clear();
                            refreshBadge();
                            refresh.run();
                        })
                        .setNegativeButton("Cancelar", null)
                        .show()
        );

        refresh.run();

        sheet.setContentView(raiz);
        sheet.setOnShowListener(d -> {
            com.google.android.material.bottomsheet.BottomSheetBehavior<?> behavior =
                    com.google.android.material.bottomsheet.BottomSheetBehavior.from(
                            (android.view.View) raiz.getParent());
            behavior.setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED);
            behavior.setSkipCollapsed(true);
        });
        sheet.show();
    }

    private View buildNotifRow(Notif n, TextView tvSub, Runnable[] refreshHolder) {
        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams wp = new LinearLayout.LayoutParams(-1, -2);
        wp.bottomMargin = dp(10);
        wrapper.setLayoutParams(wp);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), dp(14), dp(12), dp(14));
        GradientDrawable rBg = new GradientDrawable();
        rBg.setColor(n.leida ? 0xFFF8FAFF : BLUE_XL);
        rBg.setCornerRadius(dp(18));
        if (!n.leida) rBg.setStroke(dp(1), 0xFFD1E3FF);
        row.setBackground(rBg);
        row.setClickable(true);
        row.setFocusable(true);
        row.setElevation(n.leida ? 0 : dp(2));

        // Icono
        boolean esCita = "cita".equals(n.tipo);
        FrameLayout icFrame = new FrameLayout(this);
        LinearLayout.LayoutParams icP = new LinearLayout.LayoutParams(dp(42), dp(42));
        icP.setMarginEnd(dp(12));
        icFrame.setLayoutParams(icP);
        GradientDrawable icBg = new GradientDrawable();
        icBg.setShape(GradientDrawable.OVAL);
        icBg.setColor(esCita ? 0xFFE8F0FF : 0xFFFFF3DC);
        icFrame.setBackground(icBg);
        android.widget.ImageView ivIcon = new android.widget.ImageView(this);
        FrameLayout.LayoutParams iconP = new FrameLayout.LayoutParams(dp(20), dp(20));
        iconP.gravity = Gravity.CENTER;
        ivIcon.setLayoutParams(iconP);
        ivIcon.setImageResource(esCita ? R.drawable.ic_calendar_today : R.drawable.ic_payments);
        ivIcon.setColorFilter(esCita ? 0xFF2563EB : 0xFFF59E0B);
        icFrame.addView(ivIcon);
        row.addView(icFrame);

        // Textos
        LinearLayout txt = new LinearLayout(this);
        txt.setOrientation(LinearLayout.VERTICAL);
        txt.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));

        TextView tvT = new TextView(this);
        tvT.setText(n.titulo);
        tvT.setTextSize(13f);
        tvT.setTextColor(TEXT_D);
        tvT.setTypeface(Typeface.DEFAULT_BOLD);
        tvT.setAlpha(n.leida ? 0.4f : 1f);
        tvT.setSingleLine(true);
        tvT.setEllipsize(android.text.TextUtils.TruncateAt.END);
        txt.addView(tvT);

        TextView tvD2 = new TextView(this);
        tvD2.setText(n.desc);
        tvD2.setTextSize(11.5f);
        tvD2.setTextColor(TEXT_M);
        tvD2.setAlpha(n.leida ? 0.4f : 1f);
        tvD2.setSingleLine(true);
        tvD2.setEllipsize(android.text.TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams dP2 = new LinearLayout.LayoutParams(-2, -2);
        dP2.topMargin = dp(3);
        tvD2.setLayoutParams(dP2);
        txt.addView(tvD2);

        TextView tvH = new TextView(this);
        tvH.setText(n.hora);
        tvH.setTextSize(10f);
        tvH.setTextColor(TEXT_L);
        LinearLayout.LayoutParams htP = new LinearLayout.LayoutParams(-2, -2);
        htP.topMargin = dp(4);
        tvH.setLayoutParams(htP);
        txt.addView(tvH);

        row.addView(txt);

        // Punto azul no leída
        if (!n.leida) {
            View dot = new View(this);
            LinearLayout.LayoutParams dotP = new LinearLayout.LayoutParams(dp(8), dp(8));
            dotP.gravity = Gravity.CENTER_VERTICAL;
            dotP.setMarginEnd(dp(8));
            dot.setLayoutParams(dotP);
            GradientDrawable dotBg = new GradientDrawable();
            dotBg.setShape(GradientDrawable.OVAL);
            dotBg.setColor(BLUE);
            dot.setBackground(dotBg);
            row.addView(dot);
        }

        // Papelera individual
        FrameLayout btnDel = new FrameLayout(this);
        LinearLayout.LayoutParams delP = new LinearLayout.LayoutParams(dp(32), dp(32)); // ← TAMAÑO DEL CÍRCULO
        delP.gravity = Gravity.CENTER_VERTICAL;
        btnDel.setLayoutParams(delP);
        GradientDrawable delBg = new GradientDrawable();
        delBg.setShape(GradientDrawable.OVAL);
        delBg.setColor(0xFFEEF4FF); // ← COLOR FONDO CÍRCULO (azul claro)
        delBg.setStroke(dp(1), 0xFFD1E3FF); // ← BORDE AZUL
        btnDel.setBackground(delBg);
        btnDel.setClickable(true);
        btnDel.setFocusable(true);
        android.widget.ImageView ivDel = new android.widget.ImageView(this);
        FrameLayout.LayoutParams ivDelP = new FrameLayout.LayoutParams(dp(15), dp(15)); // ← TAMAÑO DEL ICONO
        ivDelP.gravity = Gravity.CENTER;
        ivDel.setLayoutParams(ivDelP);
        ivDel.setImageResource(R.drawable.ic_delete);
        ivDel.setColorFilter(BLUE); // ← COLOR ICONO
        btnDel.addView(ivDel);
        btnDel.setOnClickListener(vv -> {
            marcarEliminada(n.id);
            notificaciones.remove(n);
            refreshBadge();
            if (refreshHolder[0] != null) refreshHolder[0].run();
        });
        row.addView(btnDel);

        // Click fila → marcar leída
        row.setOnClickListener(v -> {
            if (!n.leida) {
                n.leida = true;
                marcarLeida(n.id);
                refreshBadge();
                rBg.setColor(0xFFF8FAFF);
                rBg.setStroke(0, 0);
                row.setElevation(0);
                tvT.setAlpha(0.4f);
                tvD2.setAlpha(0.4f);
                long nl = notificaciones.stream().filter(x -> !x.leida).count();
                if (tvSub != null) tvSub.setText(nl > 0 ? nl + " sin leer" : "Todo al día ✅");
                if (refreshHolder[0] != null) refreshHolder[0].run();
            }
        });

        wrapper.addView(row);
        return wrapper;
    }

    // Método antiguo mantenido por compatibilidad si el XML lo llama
    private View notifRow(Notif n, TextView tvSub) {
        return buildNotifRow(n, tvSub, new Runnable[]{null});
    }

    // ════════════════════════════════════════════════════════════════
    //  UI BUILDERS
    // ════════════════════════════════════════════════════════════════
    private void setupGreeting() {
        int h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        tvGreeting.setText(h < 13 ? "Buenos días " : h < 20 ? "Buenas tardes " : "Buenas noches ");
    }

    private void setupFecha() {
        String f = new SimpleDateFormat("EEEE, d 'de' MMM", new Locale("es")).format(new Date());
        tvFechaHoy.setText(f.substring(0, 1).toUpperCase() + f.substring(1));
    }

    private View rowItem(String titulo, String sub, String chipTxt, int accentColor) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(16), 0, dp(16), 0);
        row.setMinimumHeight(dp(68));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(WHITE);
        bg.setCornerRadius(dp(18));
        row.setBackground(bg);
        LinearLayout.LayoutParams rP = new LinearLayout.LayoutParams(-1, dp(68));
        rP.bottomMargin = dp(8);
        row.setLayoutParams(rP);
        row.setClickable(true);
        row.setFocusable(true);

        // Barra lateral
        View bar = new View(this);
        GradientDrawable barD = new GradientDrawable();
        barD.setColor(accentColor);
        barD.setCornerRadius(dp(3));
        bar.setBackground(barD);
        LinearLayout.LayoutParams barP = new LinearLayout.LayoutParams(dp(4), dp(32));
        barP.setMarginEnd(dp(14));
        barP.gravity = Gravity.CENTER_VERTICAL;
        bar.setLayoutParams(barP);
        row.addView(bar);

        // Texto
        LinearLayout txt = new LinearLayout(this);
        txt.setOrientation(LinearLayout.VERTICAL);
        txt.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
        TextView tv1 = new TextView(this);
        tv1.setText(titulo);
        tv1.setTextSize(14f);
        tv1.setTextColor(TEXT_D);
        tv1.setTypeface(Typeface.DEFAULT_BOLD);
        tv1.setSingleLine(true);
        tv1.setEllipsize(android.text.TextUtils.TruncateAt.END);
        txt.addView(tv1);
        TextView tv2 = new TextView(this);
        tv2.setText(sub);
        tv2.setTextSize(12f);
        tv2.setTextColor(TEXT_L);
        tv2.setSingleLine(true);
        tv2.setEllipsize(android.text.TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams s2P = new LinearLayout.LayoutParams(-2, -2);
        s2P.topMargin = dp(2);
        tv2.setLayoutParams(s2P);
        txt.addView(tv2);
        row.addView(txt);

        // Chip
        GradientDrawable chipBg = new GradientDrawable();
        chipBg.setColor(adjustAlpha(accentColor, 0.1f));
        chipBg.setCornerRadius(dp(10));
        TextView chip = new TextView(this);
        chip.setText(chipTxt);
        chip.setTextSize(15f);
        chip.setTextColor(accentColor);
        chip.setTypeface(Typeface.DEFAULT_BOLD);
        chip.setGravity(Gravity.CENTER);
        chip.setPadding(dp(10), dp(5), dp(10), dp(5));
        chip.setBackground(chipBg);
        LinearLayout.LayoutParams cP = new LinearLayout.LayoutParams(-2, -2);
        cP.setMarginStart(dp(10));
        cP.gravity = Gravity.CENTER_VERTICAL;
        chip.setLayoutParams(cP);
        row.addView(chip);
        return row;
    }

    private View rowVacio(String msg) {
        TextView tv = new TextView(this);
        tv.setText(msg);
        tv.setTextSize(13f);
        tv.setTextColor(TEXT_L);
        tv.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(56));
        p.bottomMargin = dp(4);
        tv.setLayoutParams(p);
        return tv;
    }

    // ════════════════════════════════════════════════════════════════
    //  BOTONES
    // ════════════════════════════════════════════════════════════════
    private void bindButtons() {
        safeClick(R.id.btnNotif,        v -> showNotifs());
        safeClick(R.id.btnSettings,     v -> startActivity(new Intent(this, SettingsActivity.class)));
        safeClick(R.id.btnAccionCobrar, v -> startActivity(new Intent(this, CobrosActivity.class)));
        safeClick(R.id.btnAccionCita,   v -> startActivity(new Intent(this, AgendaActivity.class)));
        safeClick(R.id.btnAccionGym,    v -> startActivity(new Intent(this, GimnasioActivity.class)));
        safeClick(R.id.btnAccionClientes,v -> startActivity(new Intent(this, ClientesActivity.class)));
        safeClick(R.id.tvVerCitas,        v -> startActivity(new Intent(this, AgendaActivity.class)));
        safeClick(R.id.tvVerPagos,        v -> startActivity(new Intent(this, CobrosActivity.class)));
        safeClick(R.id.tvKpiGym,          v -> startActivity(new Intent(this, GimnasioActivity.class)));
        safeClick(R.id.btnAccionInscribir, v -> showInscribirSheet());
    }

    private void safeClick(int id, View.OnClickListener l) {
        View v = findViewById(id);
        if (v != null) v.setOnClickListener(l);
    }

    // ════════════════════════════════════════════════════════════════
    //  SHEET: ACCIONES RÁPIDAS
    // ════════════════════════════════════════════════════════════════




    // ════════════════════════════════════════════════════════════════
    //  HELPERS
    // ════════════════════════════════════════════════════════════════

    /** Fecha de hoy "dd/MM/yyyy" para getCitasPorFecha (formato de la BD: yyyy-MM-dd) */
    private String fechaHoy() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
    }

    /** Fecha de hoy "yyyy-MM-dd" */
    private String fechaHoyISO() {
        return fechaHoy();
    }

    /** día de semana de hoy: 1=Lun…7=Dom */
    private int diaSemanaHoy() {
        int dow = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
        return (dow == Calendar.SUNDAY) ? 7 : dow - 1; // MONDAY=2 → 1
    }

    private int adjustAlpha(int color, float alpha) {
        return (Math.round(255 * alpha) << 24) | (color & 0x00FFFFFF);
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    // ════════════════════════════════════════════════════════════════
    //  SHEET: INSCRIBIR CLIENTE — membresía mensual
    // ════════════════════════════════════════════════════════════════
    private void showInscribirSheet() {
        BottomSheetDialog sheet = new BottomSheetDialog(this,
                com.google.android.material.R.style.Theme_Material3_Light_BottomSheetDialog);

        LinearLayout raiz = new LinearLayout(this);
        raiz.setOrientation(LinearLayout.VERTICAL);
        raiz.setBackgroundColor(WHITE);
        int alturaSheet = (int)(getResources().getDisplayMetrics().heightPixels * 0.85f);
        raiz.setLayoutParams(new LinearLayout.LayoutParams(-1, alturaSheet));

        // Handle
        LinearLayout hw = new LinearLayout(this);
        hw.setGravity(android.view.Gravity.CENTER_HORIZONTAL);
        LinearLayout.LayoutParams hwP = new LinearLayout.LayoutParams(-1, -2);
        hwP.topMargin = dp(10); hwP.bottomMargin = dp(4);
        hw.setLayoutParams(hwP);
        android.view.View handle = new android.view.View(this);
        android.graphics.drawable.GradientDrawable hBg = new android.graphics.drawable.GradientDrawable();
        hBg.setColor(BORDER); hBg.setCornerRadius(dp(3));
        handle.setBackground(hBg);
        handle.setLayoutParams(new LinearLayout.LayoutParams(dp(40), dp(4)));
        hw.addView(handle); raiz.addView(hw);

        // Título
        TextView tvTitulo = new TextView(this);
        tvTitulo.setText("Inscribir cliente");
        tvTitulo.setTextSize(20f); tvTitulo.setTextColor(TEXT_D);
        tvTitulo.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams titP = new LinearLayout.LayoutParams(-1, -2);
        titP.setMargins(dp(20), dp(12), dp(20), dp(4));
        tvTitulo.setLayoutParams(titP);
        raiz.addView(tvTitulo);

        TextView tvSub = new TextView(this);
        tvSub.setText("Crea una membresía mensual y genera el cobro automáticamente");
        tvSub.setTextSize(12f); tvSub.setTextColor(TEXT_L);
        LinearLayout.LayoutParams subP = new LinearLayout.LayoutParams(-1, -2);
        subP.setMargins(dp(20), 0, dp(20), dp(16));
        tvSub.setLayoutParams(subP);
        raiz.addView(tvSub);

        // Divisor
        android.view.View div0 = new android.view.View(this);
        div0.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(1)));
        div0.setBackgroundColor(BORDER);
        raiz.addView(div0);

        // ScrollView para el resto
        androidx.core.widget.NestedScrollView nsv = new androidx.core.widget.NestedScrollView(this);
        nsv.setLayoutParams(new LinearLayout.LayoutParams(-1, 0, 1f));
        nsv.setFillViewport(true);

        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(20), dp(16), dp(20), dp(40));
        nsv.addView(form);
        raiz.addView(nsv);

        // ── Estado compartido ─────────────────────────────────────
        final String[] clienteIdSel   = {null};
        final String[] clienteNomSel  = {null};
        final double[] precioSel      = {35.0}; // ← precio inicial actualizado a 2h

        // Referencias a los EditText de fecha
        final android.widget.EditText[] etDiaRef  = {null};
        final android.widget.EditText[] etMesRef  = {null};
        final android.widget.EditText[] etAnioRef = {null};

        // ── Campo cliente con buscador ────────────────────────────
        lbl(form, "Cliente *");
        android.widget.EditText etCliente = new android.widget.EditText(this);
        etCliente.setHint("Busca un cliente...");
        etCliente.setTextSize(14f); etCliente.setTextColor(TEXT_D);
        etCliente.setBackground(cardBg());
        etCliente.setPadding(dp(14), dp(14), dp(14), dp(14));
        LinearLayout.LayoutParams etP = new LinearLayout.LayoutParams(-1, -2);
        etP.bottomMargin = dp(4);
        etCliente.setLayoutParams(etP);
        form.addView(etCliente);

        // Lista sugerencias
        LinearLayout layoutSug = new LinearLayout(this);
        layoutSug.setOrientation(LinearLayout.VERTICAL);
        layoutSug.setVisibility(android.view.View.GONE);
        layoutSug.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
        form.addView(layoutSug);

        // TextWatcher buscador
        final boolean[] seleccionandoCliente = {false};
        etCliente.addTextChangedListener(new android.text.TextWatcher() {
            private final android.os.Handler h = new android.os.Handler();
            private Runnable r;
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            public void onTextChanged(CharSequence s, int a, int b, int c) {
                if (seleccionandoCliente[0]) return;
                clienteIdSel[0] = null; clienteNomSel[0] = null;
                if (r != null) h.removeCallbacks(r);
            }
            public void afterTextChanged(android.text.Editable s) {
                if (seleccionandoCliente[0]) { seleccionandoCliente[0] = false; return; }
                String txt = s.toString().trim();
                if (txt.length() < 2) {
                    layoutSug.setVisibility(android.view.View.GONE);
                    layoutSug.removeAllViews(); return;
                }
                r = () -> buscarClientesInscripcion(txt, layoutSug, etCliente,
                        clienteIdSel, clienteNomSel, seleccionandoCliente);
                h.postDelayed(r, 350);
            }
        });

        // ── Tipo de membresía ────────────────────────────────────
        lbl(form, "Tipo de membresía");

        // Fila 1: Mensual 2h | Mensual 3h
        LinearLayout rowTipo1 = new LinearLayout(this);
        rowTipo1.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rowT1P = new LinearLayout.LayoutParams(-1, -2);
        rowT1P.bottomMargin = dp(8);
        rowTipo1.setLayoutParams(rowT1P);
        form.addView(rowTipo1);

        // Fila 2: Mensual 4h | Mensual personalizado
        LinearLayout rowTipo2 = new LinearLayout(this);
        rowTipo2.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rowT2P = new LinearLayout.LayoutParams(-1, -2);
        rowT2P.bottomMargin = dp(16);
        rowTipo2.setLayoutParams(rowT2P);
        form.addView(rowTipo2);

        final String[] tipoSel = {"mensual_2h"}; // ← tipo inicial actualizado

        // Fila 1: 2h y 3h
        String[] tiposFila1   = {"mensual_2h", "mensual_3h"};
        String[] tiposLabels1 = {"Mensual 2h", "Mensual 3h"};
        // Fila 2: 4h y personalizado
        String[] tiposFila2   = {"mensual_4h", "mensual_personalizado"};
        String[] tiposLabels2 = {"Mensual 4h", "Mensual personalizado"};

        android.view.View[] todosChips = new android.view.View[4];

        for (int i = 0; i < tiposFila1.length; i++) {
            final String t = tiposFila1[i];
            TextView chip = chipTv(tiposLabels1[i], t.equals(tipoSel[0]));
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0, dp(44), 1f);
            if (i < tiposFila1.length - 1) cp.setMarginEnd(dp(6));
            chip.setLayoutParams(cp);
            todosChips[i] = chip;
            rowTipo1.addView(chip);
        }
        for (int i = 0; i < tiposFila2.length; i++) {
            final String t = tiposFila2[i];
            TextView chip = chipTv(tiposLabels2[i], t.equals(tipoSel[0]));
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0, dp(44), 1f);
            if (i < tiposFila2.length - 1) cp.setMarginEnd(dp(6));
            chip.setLayoutParams(cp);
            todosChips[2 + i] = chip;
            rowTipo2.addView(chip);
        }

        precioSel[0] = 35; // ← precio inicial 2h

        final TextView[] tvPrecioRef = {null};
        // ← 4 tipos con precios actualizados
        String[] todosTipos     = {"mensual_2h", "mensual_3h", "mensual_4h", "mensual_personalizado"};
        double[] preciosPorTipo = {35, 45, 55, 0};

        for (int i = 0; i < todosChips.length; i++) {
            final String t          = todosTipos[i];
            final double precioAuto = preciosPorTipo[i];
            todosChips[i].setOnClickListener(v -> {
                tipoSel[0]   = t;
                precioSel[0] = precioAuto;
                for (android.view.View chip : todosChips) {
                    boolean sel = chip == v;
                    ((TextView) chip).setBackground(sel ? chipActiveBg() : chipInactiveBg());
                    ((TextView) chip).setTextColor(sel ? WHITE : TEXT_M);
                }
                tvPrecioRef[0].setText(precioAuto == 0 ? "0€" : (int) precioAuto + "€");
            });
        }

        // ── Precio ───────────────────────────────────────────────
        lbl(form, "Cuota (€)");
        LinearLayout rowPrecio = new LinearLayout(this);
        rowPrecio.setOrientation(LinearLayout.HORIZONTAL);
        rowPrecio.setGravity(android.view.Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rpP = new LinearLayout.LayoutParams(-1, -2);
        rpP.bottomMargin = dp(16);
        rowPrecio.setLayoutParams(rpP);
        form.addView(rowPrecio);

        TextView btnMenos = spinBtn("−");
        final TextView tvPrecio = new TextView(this);
        tvPrecioRef[0] = tvPrecio;
        tvPrecio.setText("35€"); // ← precio inicial actualizado
        tvPrecio.setTextSize(32f); tvPrecio.setTextColor(BLUE);
        tvPrecio.setTypeface(Typeface.DEFAULT_BOLD);
        tvPrecio.setGravity(android.view.Gravity.CENTER);
        tvPrecio.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
        tvPrecio.setClickable(true); tvPrecio.setFocusable(true);
        android.graphics.drawable.GradientDrawable precioBg = new android.graphics.drawable.GradientDrawable();
        precioBg.setColor(BLUE_XL); precioBg.setCornerRadius(dp(12));
        precioBg.setStroke(dp(1), adjustAlpha(BLUE, 0.3f));
        tvPrecio.setBackground(precioBg);
        tvPrecio.setPadding(dp(12), dp(8), dp(12), dp(8));
        TextView btnMas = spinBtn("+");

        btnMenos.setOnClickListener(v -> {
            if (precioSel[0] > 1) { precioSel[0] = Math.max(1, precioSel[0] - 5);
                tvPrecio.setText((int) precioSel[0] + "€"); }
        });
        btnMas.setOnClickListener(v -> {
            if (precioSel[0] < 9999) { precioSel[0] = Math.min(9999, precioSel[0] + 5);
                tvPrecio.setText((int) precioSel[0] + "€"); }
        });
        tvPrecio.setOnClickListener(v -> showPrecioPicker(precioSel, tvPrecio));

        rowPrecio.addView(btnMenos); rowPrecio.addView(tvPrecio); rowPrecio.addView(btnMas);

        // ── Fecha de inicio ──────────────────────────────────────────
        lbl(form, "Fecha de inicio *");

        java.util.Calendar cal = java.util.Calendar.getInstance();

        LinearLayout rowFecha = new LinearLayout(this);
        rowFecha.setOrientation(LinearLayout.HORIZONTAL);
        rowFecha.setGravity(android.view.Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rfP = new LinearLayout.LayoutParams(-1, -2);
        rfP.bottomMargin = dp(16);
        rowFecha.setLayoutParams(rfP);
        form.addView(rowFecha);

        // Día
        android.widget.EditText etDia = new android.widget.EditText(this);
        etDia.setText(String.valueOf(cal.get(java.util.Calendar.DAY_OF_MONTH)));
        etDia.setHint("DD");
        etDia.setTextSize(16f); etDia.setTextColor(TEXT_D);
        etDia.setBackground(cardBg());
        etDia.setPadding(dp(14), dp(14), dp(14), dp(14));
        etDia.setGravity(android.view.Gravity.CENTER);
        etDia.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        etDia.setMaxLines(1);
        LinearLayout.LayoutParams diaP = new LinearLayout.LayoutParams(0, -2, 1f);
        diaP.setMarginEnd(dp(6));
        etDia.setLayoutParams(diaP);
        etDiaRef[0] = etDia;
        rowFecha.addView(etDia);

        TextView tvSlash1 = new TextView(this);
        tvSlash1.setText("/");
        tvSlash1.setTextSize(20f); tvSlash1.setTextColor(TEXT_L);
        tvSlash1.setGravity(android.view.Gravity.CENTER);
        tvSlash1.setLayoutParams(new LinearLayout.LayoutParams(-2, -2));
        rowFecha.addView(tvSlash1);

        // Mes
        android.widget.EditText etMes = new android.widget.EditText(this);
        etMes.setText(String.format(java.util.Locale.getDefault(), "%02d",
                cal.get(java.util.Calendar.MONTH) + 1));
        etMes.setHint("MM");
        etMes.setTextSize(16f); etMes.setTextColor(TEXT_D);
        etMes.setBackground(cardBg());
        etMes.setPadding(dp(14), dp(14), dp(14), dp(14));
        etMes.setGravity(android.view.Gravity.CENTER);
        etMes.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        etMes.setMaxLines(1);
        LinearLayout.LayoutParams mesP = new LinearLayout.LayoutParams(0, -2, 1f);
        mesP.setMarginStart(dp(6)); mesP.setMarginEnd(dp(6));
        etMes.setLayoutParams(mesP);
        etMesRef[0] = etMes;
        rowFecha.addView(etMes);

        TextView tvSlash2 = new TextView(this);
        tvSlash2.setText("/");
        tvSlash2.setTextSize(20f); tvSlash2.setTextColor(TEXT_L);
        tvSlash2.setGravity(android.view.Gravity.CENTER);
        tvSlash2.setLayoutParams(new LinearLayout.LayoutParams(-2, -2));
        rowFecha.addView(tvSlash2);

        // Año
        android.widget.EditText etAnio = new android.widget.EditText(this);
        etAnio.setText(String.valueOf(cal.get(java.util.Calendar.YEAR)));
        etAnio.setHint("AAAA");
        etAnio.setTextSize(16f); etAnio.setTextColor(TEXT_D);
        etAnio.setBackground(cardBg());
        etAnio.setPadding(dp(14), dp(14), dp(14), dp(14));
        etAnio.setGravity(android.view.Gravity.CENTER);
        etAnio.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        etAnio.setMaxLines(1);
        LinearLayout.LayoutParams anioP = new LinearLayout.LayoutParams(0, -2, 1.5f);
        anioP.setMarginStart(dp(6));
        etAnio.setLayoutParams(anioP);
        etAnioRef[0] = etAnio;
        rowFecha.addView(etAnio);

        // ── Notas ────────────────────────────────────────────────
        lbl(form, "Notas (opcional)");
        android.widget.EditText etNotas = new android.widget.EditText(this);
        etNotas.setHint("Observaciones...");
        etNotas.setTextSize(13f); etNotas.setTextColor(TEXT_D);
        etNotas.setBackground(cardBg());
        etNotas.setPadding(dp(14), dp(14), dp(14), dp(14));
        etNotas.setMinLines(2); etNotas.setGravity(android.view.Gravity.TOP);
        etNotas.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        LinearLayout.LayoutParams notasP = new LinearLayout.LayoutParams(-1, -2);
        notasP.bottomMargin = dp(24);
        etNotas.setLayoutParams(notasP);
        form.addView(etNotas);

        // ── Botón guardar ────────────────────────────────────────
        android.graphics.drawable.GradientDrawable btnSaveBg = new android.graphics.drawable.GradientDrawable();
        btnSaveBg.setColor(BLUE); btnSaveBg.setCornerRadius(dp(16));
        TextView btnGuardar = new TextView(this);
        btnGuardar.setText("🎫 Inscribir y generar cobro");
        btnGuardar.setTextSize(15f); btnGuardar.setTextColor(WHITE);
        btnGuardar.setTypeface(Typeface.DEFAULT_BOLD);
        btnGuardar.setGravity(android.view.Gravity.CENTER);
        btnGuardar.setPadding(0, dp(18), 0, dp(18));
        btnGuardar.setBackground(btnSaveBg);
        btnGuardar.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
        btnGuardar.setClickable(true);
        form.addView(btnGuardar);

        // ── Lógica de guardar ─────────────────────────────────────
        btnGuardar.setOnClickListener(v -> {
            String nombre = etCliente.getText().toString().trim();
            if (nombre.isEmpty()) { etCliente.setError("Elige un cliente"); return; }
            if (clienteIdSel[0] == null) { etCliente.setError("Selecciona de la lista"); return; }

            btnGuardar.setEnabled(false);
            btnGuardar.setText("Guardando...");

            int dia  = 1, mes = 1, anio = 2026;
            try { dia  = Integer.parseInt(etDiaRef[0].getText().toString().trim()); }  catch (Exception ignored) {}
            try { mes  = Integer.parseInt(etMesRef[0].getText().toString().trim()); }  catch (Exception ignored) {}
            try { anio = Integer.parseInt(etAnioRef[0].getText().toString().trim()); } catch (Exception ignored) {}

            dia  = Math.max(1, Math.min(dia,  31));
            mes  = Math.max(1, Math.min(mes,  12));
            anio = Math.max(2020, Math.min(anio, 2100));

            final String fechaInicio = String.format(java.util.Locale.getDefault(),
                    "%04d-%02d-%02d", anio, mes, dia);

            final String clienteId  = clienteIdSel[0];
            final String clienteNom = clienteNomSel[0] != null ? clienteNomSel[0] : nombre;
            final double precio     = precioSel[0];
            final String tipo       = tipoSel[0];
            final String notas      = etNotas.getText().toString().trim();

            inscribirConChequeo(clienteId, clienteNom, tipo, precio, fechaInicio, notas,
                    () -> sheet.dismiss(),
                    () -> {
                        btnGuardar.setEnabled(true);
                        btnGuardar.setText("🎫 Inscribir y generar cobro");
                    });
        });

        sheet.setContentView(raiz);
        sheet.setOnShowListener(d -> {
            com.google.android.material.bottomsheet.BottomSheetBehavior<?> beh =
                    com.google.android.material.bottomsheet.BottomSheetBehavior.from(
                            (android.view.View) raiz.getParent());
            beh.setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED);
            beh.setSkipCollapsed(true);
        });
        sheet.show();
    }

    // ── Buscador de clientes para inscripción ─────────────────────
    private void buscarClientesInscripcion(String texto, LinearLayout layoutSug,
                                           android.widget.EditText etCliente, String[] clienteIdSel,
                                           String[] clienteNomSel, boolean[] seleccionando) {
        SupabaseRepository.get().getClientes(null,
                new SupabaseRepository.Callback<List<ClienteModel>>() {
                    @Override public void onSuccess(List<ClienteModel> data) {
                        runOnUiThread(() -> {
                            layoutSug.removeAllViews();
                            String q = texto.toLowerCase().trim();
                            List<ClienteModel> hits = new ArrayList<>();
                            for (ClienteModel c : data) {
                                if (hits.size() >= 6) break;
                                String nom = (c.nombre != null ? c.nombre.trim() : "");
                                String ape = (c.apellidos != null ? c.apellidos.trim() : "");
                                String full = ape.isEmpty() ? nom : nom + " " + ape;
                                String tel  = c.telefono != null ? c.telefono.trim() : "";
                                if (full.toLowerCase().contains(q) || tel.contains(q))
                                    hits.add(c);
                            }
                            if (hits.isEmpty()) { layoutSug.setVisibility(android.view.View.GONE); return; }

                            androidx.cardview.widget.CardView cont = new androidx.cardview.widget.CardView(MainActivity.this);
                            cont.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
                            cont.setRadius(dp(14)); cont.setCardElevation(dp(5));
                            cont.setCardBackgroundColor(WHITE);
                            LinearLayout inner = new LinearLayout(MainActivity.this);
                            inner.setOrientation(LinearLayout.VERTICAL);
                            cont.addView(inner);

                            for (int idx = 0; idx < hits.size(); idx++) {
                                ClienteModel c = hits.get(idx);
                                String nom  = c.nombre   != null ? c.nombre.trim()   : "";
                                String ape  = c.apellidos!= null ? c.apellidos.trim(): "";
                                String full = ape.isEmpty() ? nom : nom + " " + ape;
                                if (full.isEmpty()) full = "Cliente";
                                String tel  = c.telefono != null ? c.telefono.trim() : "";

                                LinearLayout fila = new LinearLayout(MainActivity.this);
                                fila.setOrientation(LinearLayout.HORIZONTAL);
                                fila.setGravity(android.view.Gravity.CENTER_VERTICAL);
                                fila.setPadding(dp(14), dp(12), dp(14), dp(12));
                                fila.setClickable(true); fila.setFocusable(true);

                                // Avatar
                                androidx.cardview.widget.CardView av = new androidx.cardview.widget.CardView(MainActivity.this);
                                LinearLayout.LayoutParams avP = new LinearLayout.LayoutParams(dp(36), dp(36));
                                avP.setMarginEnd(dp(10)); av.setLayoutParams(avP);
                                av.setRadius(dp(10)); av.setCardElevation(0);
                                av.setCardBackgroundColor(BLUE);
                                TextView tvI = new TextView(MainActivity.this);
                                tvI.setText(full.isEmpty() ? "?" : String.valueOf(full.charAt(0)).toUpperCase());
                                tvI.setTextSize(14f); tvI.setTextColor(WHITE);
                                tvI.setTypeface(Typeface.DEFAULT_BOLD);
                                tvI.setGravity(android.view.Gravity.CENTER);
                                tvI.setLayoutParams(new LinearLayout.LayoutParams(-1, -1));
                                av.addView(tvI); fila.addView(av);

                                // Info
                                LinearLayout info = new LinearLayout(MainActivity.this);
                                info.setOrientation(LinearLayout.VERTICAL);
                                info.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
                                TextView tvN = new TextView(MainActivity.this);
                                tvN.setText(full); tvN.setTextSize(13f);
                                tvN.setTextColor(TEXT_D); tvN.setTypeface(Typeface.DEFAULT_BOLD);
                                info.addView(tvN);
                                if (!tel.isEmpty()) {
                                    TextView tvT = new TextView(MainActivity.this);
                                    tvT.setText(tel); tvT.setTextSize(11f); tvT.setTextColor(TEXT_L);
                                    info.addView(tvT);
                                }
                                fila.addView(info);

                                TextView arrow = new TextView(MainActivity.this);
                                arrow.setText("›"); arrow.setTextSize(20f); arrow.setTextColor(BLUE);
                                arrow.setPadding(dp(8), 0, 0, 0); fila.addView(arrow);

                                final String idF  = c.id;
                                final String nomF = full;
                                fila.setOnClickListener(vv -> {
                                    seleccionando[0]  = true;
                                    clienteIdSel[0]   = idF;
                                    clienteNomSel[0]  = nomF;
                                    etCliente.setText(nomF);
                                    etCliente.setSelection(nomF.length());
                                    layoutSug.setVisibility(android.view.View.GONE);
                                    layoutSug.removeAllViews();
                                    android.view.inputmethod.InputMethodManager imm =
                                            (android.view.inputmethod.InputMethodManager)
                                                    getSystemService(INPUT_METHOD_SERVICE);
                                    imm.hideSoftInputFromWindow(etCliente.getWindowToken(), 0);
                                });

                                inner.addView(fila);
                                if (idx < hits.size() - 1) {
                                    android.view.View sep = new android.view.View(MainActivity.this);
                                    LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1, dp(1));
                                    sp.setMargins(dp(14), 0, dp(14), 0); sep.setLayoutParams(sp);
                                    sep.setBackgroundColor(BORDER); inner.addView(sep);
                                }
                            }
                            layoutSug.addView(cont);
                            layoutSug.setVisibility(android.view.View.VISIBLE);
                        });
                    }
                    @Override public void onError(String e) {}
                });
    }

    // ── Helpers de UI para el sheet ───────────────────────────────
    private void lbl(LinearLayout parent, String txt) {
        TextView tv = new TextView(this);
        tv.setText(txt); tv.setTextSize(11f); tv.setTextColor(TEXT_L);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.topMargin = dp(14); p.bottomMargin = dp(6);
        tv.setLayoutParams(p); parent.addView(tv);
    }

    private android.graphics.drawable.GradientDrawable cardBg() {
        android.graphics.drawable.GradientDrawable d = new android.graphics.drawable.GradientDrawable();
        d.setColor(0xFFF7F9FF); d.setCornerRadius(dp(14));
        d.setStroke(dp(1), BORDER); return d;
    }

    private TextView chipTv(String txt, boolean activo) {
        TextView tv = new TextView(this);
        tv.setText(txt); tv.setTextSize(12f);
        tv.setTextColor(activo ? WHITE : TEXT_M);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setGravity(android.view.Gravity.CENTER);
        tv.setBackground(activo ? chipActiveBg() : chipInactiveBg());
        tv.setClickable(true); tv.setFocusable(true);
        return tv;
    }

    private android.graphics.drawable.GradientDrawable chipActiveBg() {
        android.graphics.drawable.GradientDrawable d = new android.graphics.drawable.GradientDrawable();
        d.setColor(BLUE); d.setCornerRadius(dp(10)); return d;
    }

    private android.graphics.drawable.GradientDrawable chipInactiveBg() {
        android.graphics.drawable.GradientDrawable d = new android.graphics.drawable.GradientDrawable();
        d.setColor(BLUE_XL); d.setCornerRadius(dp(10)); return d;
    }

    private TextView spinBtn(String txt) {
        TextView tv = new TextView(this);
        tv.setText(txt); tv.setTextSize(26f); tv.setTextColor(BLUE);
        tv.setTypeface(Typeface.DEFAULT_BOLD); tv.setGravity(android.view.Gravity.CENTER);
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setColor(BLUE_XL); bg.setCornerRadius(dp(12));
        tv.setBackground(bg); tv.setPadding(dp(20), dp(8), dp(20), dp(8));
        tv.setClickable(true); tv.setFocusable(true);
        return tv;
    }


    // ════════════════════════════════════════════════════════════════
    //  RENOVACIÓN DE MEMBRESÍAS
    //  Al abrir la app comprueba membresías activas vencidas y
    //  pregunta si renovar. Usa SharedPreferences para no preguntar
    //  más de una vez al día por la misma membresía.
    // ════════════════════════════════════════════════════════════════
    private static final String PREFS_RENOV   = "kau_renovaciones";
    private static final String KEY_LAST_CHECK = "last_check_";

    /**
     * Inscribe una membresía SÓLO si el cliente no tiene ya una activa.
     * Hace primero una petición a la base de datos; si ya está inscrito,
     * muestra un mensaje y no crea nada.
     *
     * @param onTerminado   se ejecuta tras inscribir correctamente (p. ej. cerrar el sheet)
     * @param onNoInscrito  se ejecuta si NO se inscribe (ya activo o error) para reactivar la UI
     */
    private void inscribirConChequeo(final String clienteId, final String clienteNom,
                                     final String tipo, final double precio,
                                     final String fechaInicio, final String notas,
                                     final Runnable onTerminado, final Runnable onNoInscrito) {
        SupabaseRepository.get().tieneMembresiaActiva(clienteId,
                new SupabaseRepository.Callback<Boolean>() {
                    @Override public void onSuccess(Boolean yaActiva) {
                        if (Boolean.TRUE.equals(yaActiva)) {
                            runOnUiThread(() -> {
                                android.widget.Toast.makeText(MainActivity.this,
                                        "⚠️ " + clienteNom + " ya tiene una membresía activa. "
                                                + "Cancélala antes de volver a inscribirlo.",
                                        android.widget.Toast.LENGTH_LONG).show();
                                if (onNoInscrito != null) onNoInscrito.run();
                            });
                            return;
                        }
                        // No tiene membresía activa → crear membresía + cobro
                        SupabaseRepository.get().crearMembresia(clienteId, tipo, precio, fechaInicio, notas,
                                new SupabaseRepository.Callback<MembresiaModel>() {
                                    @Override public void onSuccess(MembresiaModel mem) {
                                        // Aseguramos los datos necesarios para sincronizar
                                        mem.clienteId   = clienteId;
                                        mem.tipo        = tipo;
                                        mem.precio      = precio;
                                        mem.fechaInicio = fechaInicio;
                                        mem.activa      = true;

                                        String concepto = "Membresía " + tipo + " - " + clienteNom;
                                        // El primer cobro va fechado en la fecha de inicio (su mes correcto)
                                        SupabaseRepository.get().crearCobroConFecha(
                                                clienteId, clienteNom, concepto,
                                                precio, "Efectivo", "pendiente", notas, fechaInicio,
                                                new SupabaseRepository.Callback<CobroModel>() {
                                                    @Override public void onSuccess(CobroModel cobro) {
                                                        // Pone al día los cobros de los meses ya transcurridos
                                                        SupabaseRepository.get().sincronizarMembresia(mem, clienteNom,
                                                                new SupabaseRepository.Callback<Boolean>() {
                                                                    @Override public void onSuccess(Boolean x) { terminarInscripcion(clienteNom, (int) precio, onTerminado); }
                                                                    @Override public void onError(String e)  { terminarInscripcion(clienteNom, (int) precio, onTerminado); }
                                                                });
                                                    }
                                                    @Override public void onError(String e) {
                                                        runOnUiThread(() -> {
                                                            android.widget.Toast.makeText(MainActivity.this,
                                                                    "Inscrito, pero error al generar cobro: " + e,
                                                                    android.widget.Toast.LENGTH_LONG).show();
                                                            if (onTerminado != null) onTerminado.run();
                                                        });
                                                    }
                                                });
                                    }
                                    @Override public void onError(String e) {
                                        runOnUiThread(() -> {
                                            android.widget.Toast.makeText(MainActivity.this,
                                                    "Error: " + e, android.widget.Toast.LENGTH_SHORT).show();
                                            if (onNoInscrito != null) onNoInscrito.run();
                                        });
                                    }
                                });
                    }
                    @Override public void onError(String e) {
                        runOnUiThread(() -> {
                            android.widget.Toast.makeText(MainActivity.this,
                                    "Error al comprobar la membresía: " + e,
                                    android.widget.Toast.LENGTH_SHORT).show();
                            if (onNoInscrito != null) onNoInscrito.run();
                        });
                    }
                });
    }

    private void terminarInscripcion(String nombre, int precio, Runnable onTerminado) {
        runOnUiThread(() -> {
            android.widget.Toast.makeText(MainActivity.this,
                    "✅ " + nombre + " inscrito · cobro de " + precio + "€ generado",
                    android.widget.Toast.LENGTH_LONG).show();
            if (onTerminado != null) onTerminado.run();
            cargarDatos();
        });
    }

    private void comprobarRenovaciones() {
        android.content.SharedPreferences p = getSharedPreferences(PREFS_RENOV, MODE_PRIVATE);
        String hoy = new java.text.SimpleDateFormat("yyyy-MM-dd",
                java.util.Locale.getDefault()).format(new java.util.Date());

        SupabaseRepository.get().getMembresias(null, true,
                new SupabaseRepository.Callback<List<MembresiaModel>>() {
                    @Override public void onSuccess(List<MembresiaModel> mems) {
                        // Necesitamos los nombres de los clientes para el concepto del cobro
                        SupabaseRepository.get().getClientes(null,
                                new SupabaseRepository.Callback<List<ClienteModel>>() {
                                    @Override public void onSuccess(List<ClienteModel> clientes) {
                                        java.util.Map<String, String> nombres = new java.util.HashMap<>();
                                        for (ClienteModel c : clientes) {
                                            if (c.id != null) {
                                                nombres.put(c.id, c.nombre
                                                        + (c.apellidos != null ? " " + c.apellidos : ""));
                                            }
                                        }
                                        procesarRenovaciones(mems, nombres, p, hoy);
                                    }
                                    @Override public void onError(String e) {
                                        // Sin nombres también podemos renovar
                                        procesarRenovaciones(mems, new java.util.HashMap<>(), p, hoy);
                                    }
                                });
                    }
                    @Override public void onError(String e) {}
                });
    }

    /**
     * Pone al día TODAS las membresías activas (sin preguntar): genera los cobros
     * de cada mes que falte y avanza la fecha de la cuota al periodo actual.
     * La única forma de detenerlo es cancelar la membresía con su botón.
     */
    private void procesarRenovaciones(List<MembresiaModel> mems,
                                      java.util.Map<String, String> nombres,
                                      android.content.SharedPreferences p, String hoy) {
        for (MembresiaModel mem : mems) {
            if (mem.id == null || mem.fechaInicio == null) continue;

            // Evitar procesar la misma membresía dos veces el mismo día
            String key = KEY_LAST_CHECK + mem.id;
            if (hoy.equals(p.getString(key, ""))) continue;
            p.edit().putString(key, hoy).apply();

            String nombre = nombres.containsKey(mem.clienteId) ? nombres.get(mem.clienteId) : "Cliente";
            sincronizarMembresiaUI(mem, nombre);
        }
    }

    private void sincronizarMembresiaUI(MembresiaModel mem, String nombre) {
        SupabaseRepository.get().sincronizarMembresia(mem, nombre,
                new SupabaseRepository.Callback<Boolean>() {
                    @Override public void onSuccess(Boolean huboCambios) {
                        if (Boolean.TRUE.equals(huboCambios)) {
                            runOnUiThread(() -> {
                                android.widget.Toast.makeText(MainActivity.this,
                                        "🔄 Cobros de " + nombre + " puestos al día",
                                        android.widget.Toast.LENGTH_SHORT).show();
                                cargarDatos();
                            });
                        }
                    }
                    @Override public void onError(String e) {}
                });
    }

    private java.util.Date calcularVencimiento(String fechaInicio, String tipo) {
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(
                    "yyyy-MM-dd", java.util.Locale.getDefault());
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.setTime(sdf.parse(fechaInicio));

            String t = tipo != null ? tipo.toLowerCase() : "mensual";
            switch (t) {
                case "mensual":
                case "mensual_3h":
                case "mensual_4h":
                case "mensual_personalizado": cal.add(java.util.Calendar.MONTH, 1); break;
                case "trimestral":            cal.add(java.util.Calendar.MONTH, 3); break;
                case "anual":                 cal.add(java.util.Calendar.YEAR,  1); break;
                default:                      cal.add(java.util.Calendar.MONTH, 1); break;
            }
            return cal.getTime();
        } catch (Exception e) { return null; }
    }

    private boolean esHoyOMañana(java.util.Date fecha) {
        java.util.Calendar hoy = java.util.Calendar.getInstance();
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.setTime(fecha);
        int diffDias = (int)((cal.getTimeInMillis() - hoy.getTimeInMillis())
                / (1000 * 60 * 60 * 24));
        return diffDias <= 1; // vence hoy o mañana
    }

    private void mostrarDialogoRenovacion(MembresiaModel mem) {
        String tipo = mem.tipo != null ? mem.tipo : "mensual";
        String precio = String.format(java.util.Locale.US, "%.2f", mem.precio).replace(".", ",") + "€";

        String tipoMay;
        switch (tipo.toLowerCase()) {
            case "mensual_3h":
                tipoMay = "Mensual 3h";
                break;
            case "mensual_4h":
                tipoMay = "Mensual 4h";
                break;
            case "mensual_personalizado":
                tipoMay = "Mensual personalizado";
                break;
            default:
                tipoMay = tipo.substring(0, 1).toUpperCase() + tipo.substring(1);
                break;
        }

        // ── 1. Buscar nombre del cliente primero, luego mostrar diálogo ──
        SupabaseRepository.get().getClientes(null,
                new SupabaseRepository.Callback<List<ClienteModel>>() {
                    @Override
                    public void onSuccess(List<ClienteModel> clientes) {
                        String nomCliente = "Cliente";
                        for (ClienteModel c : clientes) {
                            if (c.id != null && c.id.equals(mem.clienteId)) {
                                nomCliente = c.nombre
                                        + (c.apellidos != null ? " " + c.apellidos : "");
                                break;
                            }
                        }
                        final String nomFinal = nomCliente;

                        runOnUiThread(() -> {
                            android.app.AlertDialog.Builder b =
                                    new android.app.AlertDialog.Builder(MainActivity.this);
                            b.setTitle("🎫 Renovación de membresía");
                            b.setMessage("La membresía " + tipoMay + " de "
                                    + nomFinal + " (" + precio
                                    + "/mes) está a punto de vencer. ¿Renovar automáticamente?");

                            // ── Sí, renovar ──────────────────────────────────────
                            b.setPositiveButton("✅ Sí, renovar", (d, w) -> {
                                // La nueva fecha de inicio conserva SIEMPRE el día original
                                // de la primera inscripción (p. ej. el día 12), sin importar
                                // qué día se pulse renovar.
                                String nuevaFechaInicio = mem.calcularFechaInicioRenovacion();
                                if (nuevaFechaInicio == null || nuevaFechaInicio.isEmpty()) {
                                    nuevaFechaInicio = new java.text.SimpleDateFormat("yyyy-MM-dd",
                                            java.util.Locale.getDefault()).format(new java.util.Date());
                                }

                                // 1. Actualizar fecha_inicio + activa=true en Supabase
                                SupabaseRepository.get().renovarMembresia(mem.id, nuevaFechaInicio,
                                        new SupabaseRepository.Callback<Void>() {
                                            @Override
                                            public void onSuccess(Void v) {
                                                // 2. Crear cobro pendiente
                                                String concepto = "Renovación " + tipoMay + " - " + nomFinal;
                                                SupabaseRepository.get().crearCobro(
                                                        mem.clienteId, nomFinal, concepto,
                                                        mem.precio, "Efectivo", "pendiente", "",
                                                        new SupabaseRepository.Callback<CobroModel>() {
                                                            @Override
                                                            public void onSuccess(CobroModel c) {
                                                                runOnUiThread(() -> {
                                                                    android.widget.Toast.makeText(
                                                                            MainActivity.this,
                                                                            "✅ " + nomFinal + " renovado · cobro de "
                                                                                    + precio + " generado",
                                                                            android.widget.Toast.LENGTH_LONG).show();
                                                                    cargarDatos();
                                                                });
                                                            }

                                                            @Override
                                                            public void onError(String e) {
                                                                runOnUiThread(() ->
                                                                        android.widget.Toast.makeText(MainActivity.this,
                                                                                "Renovado, error al generar cobro: " + e,
                                                                                android.widget.Toast.LENGTH_LONG).show());
                                                            }
                                                        });
                                            }

                                            @Override
                                            public void onError(String e) {
                                                runOnUiThread(() ->
                                                        android.widget.Toast.makeText(MainActivity.this,
                                                                "Error al renovar: " + e,
                                                                android.widget.Toast.LENGTH_SHORT).show());
                                            }
                                        });
                            });

                            // ── Posponer 7 días ──────────────────────────────────
                            b.setNeutralButton("⏭ Posponer 7 días", (d, w) -> {
                                android.content.SharedPreferences p2 =
                                        getSharedPreferences(PREFS_RENOV, MODE_PRIVATE);
                                java.util.Calendar en7 = java.util.Calendar.getInstance();
                                en7.add(java.util.Calendar.DAY_OF_MONTH, 7);
                                String fecha7 = new java.text.SimpleDateFormat("yyyy-MM-dd",
                                        java.util.Locale.getDefault()).format(en7.getTime());
                                p2.edit().putString(KEY_LAST_CHECK + mem.id, fecha7).apply();
                            });

                            // ── Cancelar membresía ───────────────────────────────
                            b.setNegativeButton("❌ Cancelar membresía", (d, w) -> {
                                String hoy2 = new java.text.SimpleDateFormat("yyyy-MM-dd",
                                        java.util.Locale.getDefault()).format(new java.util.Date());
                                SupabaseRepository.get().cancelarMembresia(mem.id, hoy2,
                                        new SupabaseRepository.Callback<Void>() {
                                            @Override
                                            public void onSuccess(Void v) {
                                                runOnUiThread(() -> {
                                                    android.widget.Toast.makeText(MainActivity.this,
                                                            "Membresía de " + nomFinal + " cancelada",
                                                            android.widget.Toast.LENGTH_SHORT).show();
                                                    cargarDatos();
                                                });
                                            }

                                            @Override
                                            public void onError(String e) {
                                            }
                                        });
                            });

                            b.setCancelable(false);
                            b.show();
                        });
                    }

                    @Override
                    public void onError(String e) {
                        // Si falla la carga de clientes, mostramos el diálogo sin nombre
                        runOnUiThread(() -> {
                            android.app.AlertDialog.Builder b =
                                    new android.app.AlertDialog.Builder(MainActivity.this);
                            b.setTitle("🎫 Renovación de membresía");
                            b.setMessage("La membresía " + tipoMay + " de "
                                    + precio + "/mes está a punto de vencer. ¿Renovar automáticamente?");
                            b.setPositiveButton("✅ Sí, renovar", null);
                            b.setNegativeButton("Cancelar", null);
                            b.setCancelable(false);
                            b.show();
                        });
                    }
                });
    }

    // ════════════════════════════════════════════════════════════════
    //  PRECIO PICKER — 3 ruedas estilo iOS (centenas, decenas, unidades)
    // ════════════════════════════════════════════════════════════════
    private void showPrecioPicker(double[] precioSel, TextView tvPrecio) {
        int valorInicial = Math.max(0, Math.min(999, (int) precioSel[0]));
        final int[] vals = {valorInicial / 100, (valorInicial % 100) / 10, valorInicial % 10};

        com.google.android.material.bottomsheet.BottomSheetDialog sheet =
                new com.google.android.material.bottomsheet.BottomSheetDialog(this,
                        com.google.android.material.R.style.Theme_Material3_Light_BottomSheetDialog);

        // ── Root ─────────────────────────────────────────────────────
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(WHITE);
        root.setPadding(0, 0, 0, dp(52));

        // Handle
        LinearLayout hw = new LinearLayout(this);
        hw.setGravity(Gravity.CENTER_HORIZONTAL);
        LinearLayout.LayoutParams hwP = new LinearLayout.LayoutParams(-1, -2);
        hwP.topMargin = dp(12); hwP.bottomMargin = dp(20);
        hw.setLayoutParams(hwP);
        View handle = new View(this);
        GradientDrawable hBg2 = new GradientDrawable();
        hBg2.setColor(BORDER); hBg2.setCornerRadius(dp(4));
        handle.setBackground(hBg2);
        handle.setLayoutParams(new LinearLayout.LayoutParams(dp(40), dp(5)));
        hw.addView(handle);
        root.addView(hw);

        // ── Cabecera: label + precio preview ─────────────────────────
        TextView tvLabel = new TextView(this);
        tvLabel.setText("Cuota mensual");
        tvLabel.setTextSize(13f); tvLabel.setTextColor(TEXT_L);
        tvLabel.setTypeface(Typeface.DEFAULT_BOLD);
        tvLabel.setGravity(Gravity.CENTER);
        tvLabel.setLetterSpacing(0.04f);
        tvLabel.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
        root.addView(tvLabel);

        // Preview grande — animado con overshoot
        final TextView tvPreview = new TextView(this);
        int initVal = vals[0]*100 + vals[1]*10 + vals[2];
        tvPreview.setText(initVal == 0 ? "1€" : initVal + "€");
        tvPreview.setTextSize(80f);
        tvPreview.setTextColor(BLUE);
        tvPreview.setTypeface(Typeface.DEFAULT_BOLD);
        tvPreview.setGravity(Gravity.CENTER);
        tvPreview.setIncludeFontPadding(false);
        LinearLayout.LayoutParams pvP = new LinearLayout.LayoutParams(-1, -2);
        pvP.topMargin = dp(4); pvP.bottomMargin = dp(4);
        tvPreview.setLayoutParams(pvP);
        root.addView(tvPreview);

        TextView tvEuroLabel = new TextView(this);
        tvEuroLabel.setText("por período · toca las ruedas para cambiar");
        tvEuroLabel.setTextSize(10f); tvEuroLabel.setTextColor(TEXT_L);
        tvEuroLabel.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams elP = new LinearLayout.LayoutParams(-1, -2);
        elP.bottomMargin = dp(20);
        tvEuroLabel.setLayoutParams(elP);
        root.addView(tvEuroLabel);

        // ── Línea divisora superior ───────────────────────────────────
        View divTop = new View(this);
        divTop.setBackgroundColor(BORDER);
        divTop.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(1)));
        root.addView(divTop);

        // ── Zona de pickers con highlight ────────────────────────────
        android.widget.FrameLayout pickerZone = new android.widget.FrameLayout(this);
        pickerZone.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(200)));
        pickerZone.setBackgroundColor(WHITE);

        // Highlight central — franja azul muy suave
        View hlMid = new View(this);
        GradientDrawable hlBg = new GradientDrawable();
        hlBg.setColor(BLUE_XL);
        hlBg.setCornerRadius(dp(12));
        hlMid.setBackground(hlBg);
        android.widget.FrameLayout.LayoutParams hlP =
                new android.widget.FrameLayout.LayoutParams(
                        android.widget.FrameLayout.LayoutParams.MATCH_PARENT, dp(54));
        hlP.gravity = Gravity.CENTER_VERTICAL;
        hlP.leftMargin = dp(12); hlP.rightMargin = dp(12);
        hlMid.setLayoutParams(hlP);

        // Línea azul arriba y abajo del highlight
        View lineA = new View(this);
        lineA.setBackgroundColor(adjustAlpha(BLUE, 0.18f));
        android.widget.FrameLayout.LayoutParams laP =
                new android.widget.FrameLayout.LayoutParams(-1, dp(1));
        laP.gravity = Gravity.CENTER_VERTICAL;
        laP.topMargin = -dp(27);
        laP.leftMargin = dp(12); laP.rightMargin = dp(12);
        lineA.setLayoutParams(laP);

        View lineB = new View(this);
        lineB.setBackgroundColor(adjustAlpha(BLUE, 0.18f));
        android.widget.FrameLayout.LayoutParams lbP2 =
                new android.widget.FrameLayout.LayoutParams(-1, dp(1));
        lbP2.gravity = Gravity.CENTER_VERTICAL;
        lbP2.topMargin = dp(27);
        lbP2.leftMargin = dp(12); lbP2.rightMargin = dp(12);
        lineB.setLayoutParams(lbP2);

        // Fila de 3 pickers
        LinearLayout pickerRow = new LinearLayout(this);
        pickerRow.setOrientation(LinearLayout.HORIZONTAL);
        pickerRow.setGravity(Gravity.CENTER);
        pickerRow.setLayoutParams(
                new android.widget.FrameLayout.LayoutParams(-1, -1, Gravity.CENTER));

        // Multiplicadores sobre cada rueda
        String[] mults = {"×100", "×10", "×1"};
        android.widget.NumberPicker[] pickers = new android.widget.NumberPicker[3];
        String[] digitos = {"0","1","2","3","4","5","6","7","8","9"};

        // tamaños en px para reflexión
        float pxSel = android.util.TypedValue.applyDimension(
                android.util.TypedValue.COMPLEX_UNIT_SP, 38f, getResources().getDisplayMetrics());
        float pxRest = android.util.TypedValue.applyDimension(
                android.util.TypedValue.COMPLEX_UNIT_SP, 20f, getResources().getDisplayMetrics());

        for (int idx2 = 0; idx2 < 3; idx2++) {
            LinearLayout col = new LinearLayout(this);
            col.setOrientation(LinearLayout.VERTICAL);
            col.setGravity(Gravity.CENTER);
            col.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));

            // Label multiplicador
            TextView multTv = new TextView(this);
            multTv.setText(mults[idx2]);
            multTv.setTextSize(9f);
            multTv.setTextColor(adjustAlpha(BLUE, 0.5f));
            multTv.setTypeface(Typeface.DEFAULT_BOLD);
            multTv.setGravity(Gravity.CENTER);
            multTv.setLetterSpacing(0.12f);
            LinearLayout.LayoutParams mP = new LinearLayout.LayoutParams(-1, -2);
            mP.bottomMargin = dp(2);
            multTv.setLayoutParams(mP);
            col.addView(multTv);

            android.widget.NumberPicker np = new android.widget.NumberPicker(this);
            np.setMinValue(0); np.setMaxValue(9);
            np.setValue(vals[idx2]);
            np.setWrapSelectorWheel(true);
            np.setDisplayedValues(digitos);
            np.setBackground(null);

            // Quitar divisores nativos
            try {
                java.lang.reflect.Field fd = android.widget.NumberPicker.class.getDeclaredField("mSelectionDivider");
                fd.setAccessible(true); fd.set(np, null);
            } catch (Exception ignored) {}

            // Colores y tamaños via reflexión
            try {
                // Seleccionado: BLUE bold grande
                java.lang.reflect.Field selColor = android.widget.NumberPicker.class.getDeclaredField("mSelectedTextColor");
                selColor.setAccessible(true); selColor.set(np, BLUE);
                // No seleccionado: gris claro pequeño
                java.lang.reflect.Field txtColor = android.widget.NumberPicker.class.getDeclaredField("mTextColor");
                txtColor.setAccessible(true); txtColor.set(np, TEXT_L);
                // Tamaño seleccionado
                java.lang.reflect.Field selSize = android.widget.NumberPicker.class.getDeclaredField("mSelectedTextSize");
                selSize.setAccessible(true); selSize.set(np, pxSel);
                // Tamaño resto
                java.lang.reflect.Field txtSize = android.widget.NumberPicker.class.getDeclaredField("mTextSize");
                txtSize.setAccessible(true); txtSize.set(np, pxRest);
            } catch (Exception ignored) {}

            final int fi = idx2;
            pickers[idx2] = np;
            col.addView(np);
            pickerRow.addView(col);

            // Separador entre ruedas — punto azul suave
            if (idx2 < 2) {
                TextView dot = new TextView(this);
                dot.setText("·");
                dot.setTextSize(30f);
                dot.setTextColor(adjustAlpha(BLUE, 0.2f));
                dot.setGravity(Gravity.CENTER);
                LinearLayout.LayoutParams dotP = new LinearLayout.LayoutParams(-2, -2);
                dotP.topMargin = dp(30);
                dot.setLayoutParams(dotP);
                pickerRow.addView(dot);
            }
        }



        // Fade blanco top/bottom
        View fadeTop = new View(this);
        fadeTop.setBackground(new android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0xFFFFFFFF, 0x00FFFFFF}));
        fadeTop.setLayoutParams(
                new android.widget.FrameLayout.LayoutParams(-1, dp(72), Gravity.TOP));

        View fadeBot = new View(this);
        fadeBot.setBackground(new android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.BOTTOM_TOP,
                new int[]{0xFFFFFFFF, 0x00FFFFFF}));
        fadeBot.setLayoutParams(
                new android.widget.FrameLayout.LayoutParams(-1, dp(72), Gravity.BOTTOM));

        pickerZone.addView(hlMid);
        pickerZone.addView(lineA);
        pickerZone.addView(lineB);
        pickerZone.addView(pickerRow);
        pickerZone.addView(fadeTop);
        pickerZone.addView(fadeBot);
        root.addView(pickerZone);

        // ── Línea divisora inferior ───────────────────────────────────
        View divBot = new View(this);
        divBot.setBackgroundColor(BORDER);
        divBot.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(1)));
        root.addView(divBot);

        // ── Listeners con animación overshoot ────────────────────────
        for (int k = 0; k < pickers.length; k++) {
            final int ki = k;
            pickers[ki].setOnValueChangedListener((p, o, n) -> {
                vals[ki] = n;
                int total = vals[0]*100 + vals[1]*10 + vals[2];
                final int disp = total == 0 ? 1 : total;
                tvPreview.animate().cancel();
                tvPreview.animate()
                        .scaleX(0.72f).scaleY(0.72f)
                        .setDuration(60)
                        .setInterpolator(new android.view.animation.AccelerateInterpolator(1.5f))
                        .withEndAction(() -> {
                            tvPreview.setText(disp + "€");
                            tvPreview.animate()
                                    .scaleX(1f).scaleY(1f)
                                    .setDuration(320)
                                    .setInterpolator(new android.view.animation.OvershootInterpolator(4f))
                                    .start();
                        }).start();
            });
        }

        // ── BOTÓN — solo "Confirmar" ──────────────────────────────────
        GradientDrawable btnBg = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{0xFF1D4ED8, 0xFF2563EB});
        btnBg.setCornerRadius(dp(22));

        TextView btnOk = new TextView(this);
        btnOk.setText("Confirmar");
        btnOk.setTextSize(17f);
        btnOk.setTextColor(WHITE);
        btnOk.setTypeface(Typeface.DEFAULT_BOLD);
        btnOk.setLetterSpacing(0.03f);
        btnOk.setGravity(Gravity.CENTER);
        btnOk.setPadding(0, dp(18), 0, dp(18));
        btnOk.setBackground(btnBg);
        btnOk.setClickable(true);
        btnOk.setFocusable(true);

        LinearLayout.LayoutParams okP = new LinearLayout.LayoutParams(-1, -2);
        okP.setMargins(dp(24), dp(16), dp(24), 0);
        btnOk.setLayoutParams(okP);

        btnOk.setOnClickListener(v -> {
            int raw = vals[0]*100 + vals[1]*10 + vals[2];
            final int totalFinal = raw == 0 ? 1 : raw;
            precioSel[0] = totalFinal;
            btnOk.animate().scaleX(0.96f).scaleY(0.96f).setDuration(70)
                    .withEndAction(() -> {
                        btnOk.animate().scaleX(1f).scaleY(1f).setDuration(150)
                                .setInterpolator(new android.view.animation.OvershootInterpolator(2f)).start();
                        tvPrecio.setText(totalFinal + "€");
                        tvPrecio.animate().alpha(0.2f).setDuration(60)
                                .withEndAction(() -> tvPrecio.animate().alpha(1f).setDuration(180).start())
                                .start();
                        sheet.dismiss();
                    }).start();
        });
        root.addView(btnOk);

        sheet.setContentView(root);
        sheet.setOnShowListener(d -> {
            com.google.android.material.bottomsheet.BottomSheetBehavior<?> beh =
                    com.google.android.material.bottomsheet.BottomSheetBehavior.from((View) root.getParent());
            beh.setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED);
            beh.setSkipCollapsed(true);
        });
        sheet.show();
    }

}