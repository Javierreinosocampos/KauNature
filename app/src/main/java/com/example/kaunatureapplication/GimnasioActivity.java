package com.example.kaunatureapplication;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GimnasioActivity extends AppCompatActivity {

    // ── Paleta ───────────────────────────────────────────────────────
    static final int BG      = 0xFFF7F9FF;
    static final int WHITE   = 0xFFFFFFFF;
    static final int BLUE    = 0xFF2563EB;
    static final int BLUE_L  = 0xFF3B82F6;
    static final int BLUE_XL = 0xFFEEF4FF;
    static final int BLUE_XX = 0xFFD6E4FF;
    static final int TEXT_D  = 0xFF0D1B3E;
    static final int TEXT_M  = 0xFF4B5563;
    static final int TEXT_L  = 0xFF9CA3AF;
    static final int GREEN   = 0xFF10B981;
    static final int YELLOW  = 0xFFF59E0B;
    static final int RED     = 0xFFEF4444;
    static final int BORDER  = 0xFFE5EDFF;

    // ── Constantes ────────────────────────────────────────────────────
    static final String[] DIAS       = {"Lunes","Martes","Miércoles","Jueves","Viernes","Sábado","Domingo"};
    static final String[] DIAS_CORTO = {"Lun","Mar","Mié","Jue","Vie","Sáb","Dom"};
    static final String[] DIAS_MIN   = {"L","M","X","J","V","S","D"};
    static final String[] MESES      = {"Enero","Febrero","Marzo","Abril","Mayo","Junio",
            "Julio","Agosto","Septiembre","Octubre","Noviembre","Diciembre"};

    // ── Modelo local (combina FranjaModel + asistentes) ──────────────
    static class FranjaLocal {
        String       id;          // UUID de horario_semanal en Supabase
        String       hora;        // "HH:mm"
        int          aforoMax;
        List<AsistenciaModel> asistentes = new ArrayList<>();

        FranjaLocal(FranjaModel m) {
            this.id       = m.id;
            this.hora     = m.horaDisplay();
            this.aforoMax = m.aforoMax;
        }

        int     ocupacion()     { return asistentes.size(); }
        boolean llena()         { return asistentes.size() >= aforoMax; }
        float   pct()           { return aforoMax > 0 ? Math.min(1f, (float) asistentes.size() / aforoMax) : 0f; }

        int colorEstado() {
            if (llena())      return RED;
            if (pct() > 0.65f) return YELLOW;
            return GREEN;
        }
        String etiquetaEstado() {
            if (llena())      return "LLENO";
            if (pct() > 0.65f) return "CASI";
            return "OK";
        }
    }

    // ── Estado local: mapa fecha → lista de FranjaLocal ─────────────
    // Se construye al cargar de Supabase y se actualiza en memoria.
    private final Map<String, List<FranjaLocal>> franjasPorFecha = new HashMap<>();

    // Pool de nombres para el buscador (se rellena desde clientes)
    private final List<String> clientesPool = new ArrayList<>();

    // ── Estado UI ─────────────────────────────────────────────────────
    private String  vista                  = "dia";
    private String  selectedFecha          = "";
    // Nombre preseleccionado desde ClientesActivity (solo para el buscador)
    private String  clientePreseleccionado = null;
    private int    semanaOffset   = 0;
    private int    mesOffset      = 0;
    private int    diaIdx         = 0;

    // ── Views ─────────────────────────────────────────────────────────
    private LinearLayout        llDias, llContenido;
    private TextView            tvTituloDia, tvSubDia;
    private TextView            tvKpiPersonas, tvKpiPct, tvKpiLibres, tvKpiSlots;
    private TextView            btnVistaDia, btnVistaSemana, btnVistaMes;
    private HorizontalScrollView hsvDias;

    // ════════════════════════════════════════════════════════════════
    //  onCreate
    // ════════════════════════════════════════════════════════════════
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getIntent() != null) {
            clientePreseleccionado = getIntent().getStringExtra("CLIENTE_NOMBRE");
        }

        if (selectedFecha.isEmpty()) selectedFecha = hoy();
        diaIdx = diaSemanaIdx(selectedFecha);

        // ── Root ─────────────────────────────────────────────────────
        FrameLayout rootFrame = new FrameLayout(this);
        rootFrame.setBackgroundColor(BG);

        ScrollView svMain = new ScrollView(this);
        svMain.setVerticalScrollBarEnabled(false);
        svMain.setFillViewport(true);
        svMain.setBackgroundColor(BG);
        svMain.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        rootFrame.addView(svMain);

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        svMain.addView(col);

        col.addView(buildHeader());
        col.addView(buildVistaTabs());
        col.addView(buildDiaSelector());
        col.addView(buildKpiStrip());
        col.addView(buildSeparador());

        llContenido = new LinearLayout(this);
        llContenido.setOrientation(LinearLayout.VERTICAL);
        llContenido.setPadding(dp(16), dp(14), dp(16), dp(110));
        col.addView(llContenido);

        rootFrame.addView(buildFabAnadir());
        rootFrame.addView(buildNavBar());
        setContentView(rootFrame);

        // Carga inicial
        cargarTodo();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Solo re-renderizar si ya hay datos (no recargar todo al volver)
        renderDias();
        renderContenido();
    }

    // ════════════════════════════════════════════════════════════════
    //  CARGA DESDE SUPABASE
    // ════════════════════════════════════════════════════════════════

    /**
     * 1. Carga todas las franjas activas del horario semanal.
     * 2. Para la semana visible, carga la asistencia de cada día.
     * 3. También carga el pool de clientes para el buscador.
     */
    private void cargarTodo() {
        mostrarCargando();

        // Cargar clientes para el buscador (en paralelo)
        SupabaseRepository.get().getClientes(null,
                new SupabaseRepository.Callback<List<ClienteModel>>() {
                    @Override public void onSuccess(List<ClienteModel> data) {
                        clientesPool.clear();
                        for (ClienteModel c : data)
                            clientesPool.add(c.nombre + (c.apellidos != null && !c.apellidos.isEmpty()
                                    ? " " + c.apellidos : ""));
                    }
                    @Override public void onError(String e) { /* no crítico */ }
                });

        // Cargar franjas del horario semanal
        SupabaseRepository.get().getFranjas(new SupabaseRepository.Callback<List<FranjaModel>>() {
            @Override public void onSuccess(List<FranjaModel> franjas) {
                // Para cada día de la semana visible + anterior/siguiente,
                // distribuir las franjas por su dia_semana
                // y cargar la asistencia de hoy/semana visible
                cargarAsistenciaSemana(franjas, semanaOffset);
            }
            @Override public void onError(String e) {
                runOnUiThread(() -> {
                    Toast.makeText(GimnasioActivity.this,
                            "Error cargando franjas: " + e, Toast.LENGTH_LONG).show();
                    renderDias();
                    renderContenido();
                });
            }
        });
    }

    /**
     * Para cada día de la semana visible (lunes→domingo),
     * crea FranjaLocal a partir del horario semanal y carga la asistencia real.
     */
    private void cargarAsistenciaSemana(List<FranjaModel> todasFranjas, int semOffset) {
        String lunes = lunesDeSemana(semOffset);

        // Limpiar solo las fechas de esta semana para no borrar otras semanas cargadas
        for (int d = 0; d < 7; d++) {
            String fecha = sumarDias(lunes, d);
            franjasPorFecha.put(fecha, new ArrayList<>());
        }

        // Para cada día calculamos qué franjas del horario semanal aplican
        // dia_semana en BD: 1=Lun…7=Dom
        final int[] pendientes = {7}; // 7 días, contamos cuando todos acaban

        for (int d = 0; d < 7; d++) {
            final String fecha = sumarDias(lunes, d);
            final int diaSemBD = d + 1; // 1=Lun…7=Dom

            List<FranjaModel> franjasDelDia = new ArrayList<>();
            for (FranjaModel fm : todasFranjas) {
                if (fm.diaSemana == diaSemBD) franjasDelDia.add(fm);
            }

            if (franjasDelDia.isEmpty()) {
                synchronized (this) { pendientes[0]--; }
                if (pendientes[0] <= 0) renderFinal();
                continue;
            }

            // Crear FranjaLocal para cada franja
            List<FranjaLocal> locales = new ArrayList<>();
            for (FranjaModel fm : franjasDelDia) locales.add(new FranjaLocal(fm));
            franjasPorFecha.put(fecha, locales);

            // Contar cuántas peticiones de asistencia quedan para este día
            final int[] pendAsist = {locales.size()};

            for (FranjaLocal fl : locales) {
                SupabaseRepository.get().getAsistencia(fecha, fl.id,
                        new SupabaseRepository.Callback<List<AsistenciaModel>>() {
                            @Override public void onSuccess(List<AsistenciaModel> asistentes) {
                                fl.asistentes.addAll(asistentes);
                                synchronized (GimnasioActivity.this) {
                                    pendAsist[0]--;
                                    if (pendAsist[0] <= 0) {
                                        pendientes[0]--;
                                        if (pendientes[0] <= 0) renderFinal();
                                    }
                                }
                            }
                            @Override public void onError(String e) {
                                synchronized (GimnasioActivity.this) {
                                    pendAsist[0]--;
                                    if (pendAsist[0] <= 0) {
                                        pendientes[0]--;
                                        if (pendientes[0] <= 0) renderFinal();
                                    }
                                }
                            }
                        });
            }
        }
    }

    private void renderFinal() {
        runOnUiThread(() -> {
            renderDias();
            renderContenido();


        });
    }

    private void mostrarCargando() {
        llContenido.removeAllViews();
        TextView tv = new TextView(this);
        tv.setText("Cargando...");
        tv.setTextSize(14f);
        tv.setTextColor(TEXT_L);
        tv.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        p.topMargin = dp(40);
        tv.setLayoutParams(p);
        llContenido.addView(tv);
    }

    // ── Acceso a franjas del día ──────────────────────────────────────
    private List<FranjaLocal> getFranjasDia(String fecha) {
        if (!franjasPorFecha.containsKey(fecha)) franjasPorFecha.put(fecha, new ArrayList<>());
        return franjasPorFecha.get(fecha);
    }

    // ════════════════════════════════════════════════════════════════
    //  HEADER
    // ════════════════════════════════════════════════════════════════
    private View buildHeader() {
        LinearLayout h = new LinearLayout(this);
        h.setOrientation(LinearLayout.VERTICAL);
        h.setBackgroundColor(WHITE);
        h.setPadding(dp(22), dp(52), dp(22), dp(14));

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout tCol = new LinearLayout(this);
        tCol.setOrientation(LinearLayout.VERTICAL);
        tCol.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));

        TextView tvSup = new TextView(this);
        tvSup.setText("KAU NATURE · GIMNASIO");
        tvSup.setTextSize(9.5f);
        tvSup.setTextColor(BLUE);
        tvSup.setTypeface(Typeface.DEFAULT_BOLD);
        tvSup.setLetterSpacing(0.18f);
        setMB(tvSup, 2);
        tCol.addView(tvSup);

        tvTituloDia = new TextView(this);
        tvTituloDia.setText("");
        tvTituloDia.setTextSize(30f);
        tvTituloDia.setTextColor(TEXT_D);
        tvTituloDia.setTypeface(Typeface.DEFAULT_BOLD);
        tCol.addView(tvTituloDia);

        tvSubDia = new TextView(this);
        tvSubDia.setText(fmtLargo(selectedFecha));
        tvSubDia.setTextSize(12f);
        tvSubDia.setTextColor(TEXT_L);
        tCol.addView(tvSubDia);

        row.addView(tCol);

        TextView btnCfg = new TextView(this);
        btnCfg.setText("⚙");
        btnCfg.setTextSize(20f);
        btnCfg.setTextColor(BLUE);
        btnCfg.setGravity(Gravity.CENTER);
        btnCfg.setPadding(dp(10), dp(8), dp(10), dp(8));
        GradientDrawable cfgBg = new GradientDrawable();
        cfgBg.setColor(BLUE_XL);
        cfgBg.setCornerRadius(dp(14));
        btnCfg.setBackground(cfgBg);
        btnCfg.setOnClickListener(v -> showGestionarFranjasSheet(selectedFecha));
        LinearLayout.LayoutParams cfgP = new LinearLayout.LayoutParams(-2, -2);
        cfgP.setMarginStart(dp(10));
        btnCfg.setLayoutParams(cfgP);
        row.addView(btnCfg);

        h.addView(row);
        return h;
    }

    // ════════════════════════════════════════════════════════════════
    //  TABS VISTA
    // ════════════════════════════════════════════════════════════════
    private View buildVistaTabs() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setBackgroundColor(WHITE);
        row.setPadding(dp(20), dp(4), dp(20), dp(12));

        btnVistaDia    = mkTabBtn("Día",    "dia");
        btnVistaSemana = mkTabBtn("Semana", "semana");
        btnVistaMes    = mkTabBtn("Mes",    "mes");

        for (TextView b : new TextView[]{btnVistaDia, btnVistaSemana}) {
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-2, -2);
            p.setMarginEnd(dp(8));
            b.setLayoutParams(p);
        }
        row.addView(btnVistaDia);
        row.addView(btnVistaSemana);
        row.addView(btnVistaMes);
        return row;
    }

    private TextView mkTabBtn(String label, String key) {
        TextView tv = new TextView(this);
        tv.setText(label);
        tv.setTextSize(12f);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(dp(18), dp(8), dp(18), dp(8));
        tv.setLayoutParams(new LinearLayout.LayoutParams(-2, -2));
        refreshTab(tv, key.equals(vista));
        tv.setOnClickListener(v -> {
            vista = key;
            refreshTab(btnVistaDia,    "dia".equals(vista));
            refreshTab(btnVistaSemana, "semana".equals(vista));
            refreshTab(btnVistaMes,    "mes".equals(vista));
            renderDias();
            renderContenido();
        });
        return tv;
    }

    private void refreshTab(TextView tv, boolean on) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(on ? BLUE : BLUE_XL);
        bg.setCornerRadius(dp(20));
        tv.setBackground(bg);
        tv.setTextColor(on ? WHITE : TEXT_M);
        tv.setTypeface(on ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
    }

    // ════════════════════════════════════════════════════════════════
    //  SELECTOR DÍA
    // ════════════════════════════════════════════════════════════════
    private View buildDiaSelector() {
        hsvDias = new HorizontalScrollView(this);
        hsvDias.setHorizontalScrollBarEnabled(false);
        hsvDias.setBackgroundColor(WHITE);
        llDias = new LinearLayout(this);
        llDias.setOrientation(LinearLayout.HORIZONTAL);
        llDias.setPadding(dp(14), dp(6), dp(14), dp(14));
        hsvDias.addView(llDias);
        return hsvDias;
    }

    private void renderDias() {
        hsvDias.setVisibility("dia".equals(vista) ? View.VISIBLE : View.GONE);
        if (!"dia".equals(vista)) {
            if ("semana".equals(vista)) {
                String lunes = lunesDeSemana(semanaOffset);
                tvTituloDia.setText("Semana");
                tvSubDia.setText(fmtCorto(lunes) + " – " + fmtCorto(sumarDias(lunes, 6)));
            } else {
                java.util.Calendar cal = java.util.Calendar.getInstance();
                cal.add(java.util.Calendar.MONTH, mesOffset);
                tvTituloDia.setText(MESES[cal.get(java.util.Calendar.MONTH)].toUpperCase());
                tvSubDia.setText(String.valueOf(cal.get(java.util.Calendar.YEAR)));
            }
            return;
        }
        String lunesBase = lunesDeSemana(semanaOffset);
        tvTituloDia.setText(DIAS[diaSemanaIdx(selectedFecha)].toUpperCase());
        tvSubDia.setText(fmtLargo(selectedFecha));
        llDias.removeAllViews();

        for (int i = 0; i < 7; i++) {
            final int idx = i;
            String fecha = sumarDias(lunesBase, i);
            final String fechaFinal = fecha;
            int tot = 0;
            for (FranjaLocal fl : getFranjasDia(fecha)) tot += fl.ocupacion();
            boolean act = fecha.equals(selectedFecha);
            int dNum = Integer.parseInt(fecha.substring(8));

            LinearLayout pill = new LinearLayout(this);
            pill.setOrientation(LinearLayout.VERTICAL);
            pill.setGravity(Gravity.CENTER);
            pill.setPadding(dp(12), dp(8), dp(12), dp(8));
            LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(-2, -2);
            pp.setMarginEnd(dp(8));
            pill.setLayoutParams(pp);

            GradientDrawable pbg = new GradientDrawable();
            pbg.setColor(act ? BLUE : BLUE_XL);
            pbg.setCornerRadius(dp(18));
            if (!act) pbg.setStroke(dp(1), BORDER);
            pill.setBackground(pbg);

            TextView tvD = new TextView(this);
            tvD.setText(DIAS_CORTO[i]);
            tvD.setTextSize(10f);
            tvD.setTextColor(act ? WHITE : TEXT_M);
            tvD.setTypeface(Typeface.DEFAULT_BOLD);
            tvD.setLetterSpacing(0.04f);
            tvD.setGravity(Gravity.CENTER);
            pill.addView(tvD);

            TextView tvDNum = new TextView(this);
            tvDNum.setText(String.valueOf(dNum));
            tvDNum.setTextSize(14f);
            tvDNum.setTextColor(act ? WHITE : TEXT_M);
            tvDNum.setTypeface(Typeface.DEFAULT_BOLD);
            tvDNum.setGravity(Gravity.CENTER);
            pill.addView(tvDNum);

            TextView tvN = new TextView(this);
            tvN.setText(tot > 0 ? tot + " pers." : "·");
            tvN.setTextSize(8f);
            tvN.setTextColor(act ? Color.parseColor("#AACCFF") : TEXT_L);
            tvN.setGravity(Gravity.CENTER);
            pill.addView(tvN);

            pill.setOnClickListener(v -> {
                selectedFecha = fechaFinal;
                diaIdx = idx;
                // Si no tenemos datos de esa fecha, cargar
                if (!franjasPorFecha.containsKey(fechaFinal) ||
                        franjasPorFecha.get(fechaFinal).isEmpty()) {
                    recargarFecha(fechaFinal);
                } else {
                    renderDias();
                    renderContenido();
                }
            });
            llDias.addView(pill);
        }
    }

    /** Recarga franjas + asistencia para una fecha concreta */
    private void recargarFecha(String fecha) {
        SupabaseRepository.get().getFranjas(new SupabaseRepository.Callback<List<FranjaModel>>() {
            @Override public void onSuccess(List<FranjaModel> franjas) {
                int diaSemBD = diaSemanaIdx(fecha) + 1; // 0-based → 1-based
                List<FranjaLocal> locales = new ArrayList<>();
                for (FranjaModel fm : franjas)
                    if (fm.diaSemana == diaSemBD) locales.add(new FranjaLocal(fm));

                franjasPorFecha.put(fecha, locales);
                if (locales.isEmpty()) { renderFinal(); return; }

                final int[] pend = {locales.size()};
                for (FranjaLocal fl : locales) {
                    SupabaseRepository.get().getAsistencia(fecha, fl.id,
                            new SupabaseRepository.Callback<List<AsistenciaModel>>() {
                                @Override public void onSuccess(List<AsistenciaModel> a) {
                                    fl.asistentes.addAll(a);
                                    synchronized (GimnasioActivity.this) {
                                        if (--pend[0] <= 0) renderFinal();
                                    }
                                }
                                @Override public void onError(String e) {
                                    synchronized (GimnasioActivity.this) {
                                        if (--pend[0] <= 0) renderFinal();
                                    }
                                }
                            });
                }
            }
            @Override public void onError(String e) { renderFinal(); }
        });
    }

    // ════════════════════════════════════════════════════════════════
    //  KPI STRIP
    // ════════════════════════════════════════════════════════════════
    private View buildKpiStrip() {
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setBackgroundColor(WHITE);

        View top = new View(this);
        top.setBackgroundColor(BORDER);
        top.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(1)));
        col.addView(top);

        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setPadding(dp(16), dp(14), dp(16), dp(14));
        col.addView(bar);

        tvKpiPersonas = new TextView(this);
        tvKpiPct      = new TextView(this);
        tvKpiLibres   = new TextView(this);
        tvKpiSlots    = new TextView(this);

        bar.addView(mkKpiItem(tvKpiPersonas, "PERSONAS",  Color.parseColor("#2563EB")));
        bar.addView(mkKpiSep());
        bar.addView(mkKpiItem(tvKpiPct,      "OCUPACIÓN", YELLOW));
        bar.addView(mkKpiSep());
        bar.addView(mkKpiItem(tvKpiLibres,   "LIBRES",    GREEN));
        bar.addView(mkKpiSep());
        bar.addView(mkKpiItem(tvKpiSlots,    "FRANJAS",   TEXT_M));

        View bot = new View(this);
        bot.setBackgroundColor(BORDER);
        bot.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(1)));
        col.addView(bot);

        updateKpis();
        return col;
    }

    private View mkKpiItem(TextView vVal, String label, int color) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(Gravity.CENTER);
        item.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
        vVal.setText("—");
        vVal.setTextSize(24f);
        vVal.setTextColor(color);
        vVal.setTypeface(Typeface.DEFAULT_BOLD);
        vVal.setGravity(Gravity.CENTER);
        item.addView(vVal);
        TextView tvL = new TextView(this);
        tvL.setText(label);
        tvL.setTextSize(8f);
        tvL.setTextColor(TEXT_L);
        tvL.setTypeface(Typeface.DEFAULT_BOLD);
        tvL.setLetterSpacing(0.1f);
        tvL.setGravity(Gravity.CENTER);
        item.addView(tvL);
        return item;
    }

    private View mkKpiSep() {
        View v = new View(this);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(dp(1), dp(32));
        p.gravity = Gravity.CENTER_VERTICAL;
        v.setBackgroundColor(BORDER);
        v.setLayoutParams(p);
        return v;
    }

    private void updateKpis() {
        int tot = 0, totMax = 0, slots = 0;
        for (FranjaLocal fl : getFranjasDia(selectedFecha)) {
            tot    += fl.ocupacion();
            totMax += fl.aforoMax;
            slots++;
        }
        tvKpiPersonas.setText(String.valueOf(tot));
        int pct = totMax > 0 ? Math.round(100f * tot / totMax) : 0;
        tvKpiPct.setText(pct + "%");
        tvKpiLibres.setText(String.valueOf(totMax - tot));
        tvKpiSlots.setText(String.valueOf(slots));
        tvKpiPct.setTextColor(pct > 80 ? RED : pct > 50 ? YELLOW : GREEN);
    }

    private View buildSeparador() {
        View v = new View(this);
        v.setBackgroundColor(BG);
        v.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(2)));
        return v;
    }

    // ════════════════════════════════════════════════════════════════
    //  RENDER CONTENIDO
    // ════════════════════════════════════════════════════════════════
    private void renderContenido() {
        llContenido.removeAllViews();
        updateKpis();
        switch (vista) {
            case "dia":    renderDia();    break;
            case "semana": renderSemana(); break;
            case "mes":    renderMes();    break;
        }
    }

    // ── Vista día ────────────────────────────────────────────────────
    private void renderDia() {
        List<FranjaLocal> fs = getFranjasDia(selectedFecha);
        if (fs.isEmpty()) {
            llContenido.addView(buildEmpty("Sin franjas para " + fmtLargo(selectedFecha),
                    "Toca ＋ para añadir una franja horaria"));
            return;
        }
        List<FranjaLocal> manana = new ArrayList<>(), tarde = new ArrayList<>();
        for (FranjaLocal fl : fs) {
            int hr = Integer.parseInt(fl.hora.split(":")[0]);
            if (hr < 14) manana.add(fl); else tarde.add(fl);
        }
        if (!manana.isEmpty()) {
            llContenido.addView(mkSecLabel("☀  MAÑANA  ·  " + manana.size() + " franjas", BLUE));
            for (FranjaLocal fl : manana) llContenido.addView(buildFranjaCard(fl, selectedFecha));
        }
        if (!tarde.isEmpty()) {
            View sp = new View(this);
            sp.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(6)));
            llContenido.addView(sp);
            llContenido.addView(mkSecLabel("🌙  TARDE  ·  " + tarde.size() + " franjas",
                    Color.parseColor("#7C3AED")));
            for (FranjaLocal fl : tarde) llContenido.addView(buildFranjaCard(fl, selectedFecha));
        }
        llContenido.addView(buildResumenDia(selectedFecha));
    }

    // ════════════════════════════════════════════════════════════════
    //  FRANJA CARD
    // ════════════════════════════════════════════════════════════════
    private View buildFranjaCard(FranjaLocal fl, String dia) {
        CardView card = mkCard(22);
        LinearLayout.LayoutParams cardP = new LinearLayout.LayoutParams(-1, -2);
        cardP.bottomMargin = dp(12);
        card.setLayoutParams(cardP);
        card.setCardBackgroundColor(WHITE);

        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setOrientation(LinearLayout.HORIZONTAL);

        View bar = new View(this);
        GradientDrawable barBg = new GradientDrawable();
        barBg.setColor(fl.colorEstado());
        barBg.setCornerRadii(new float[]{dp(22), dp(22), 0, 0, 0, 0, dp(22), dp(22)});
        bar.setBackground(barBg);
        bar.setLayoutParams(new LinearLayout.LayoutParams(dp(5), -1));
        wrapper.addView(bar);

        LinearLayout inner = new LinearLayout(this);
        inner.setOrientation(LinearLayout.VERTICAL);
        inner.setPadding(dp(16), dp(16), dp(16), dp(14));
        inner.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));

        // Fila hora + chip + círculo
        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams trP = new LinearLayout.LayoutParams(-1, -2);
        trP.bottomMargin = dp(fl.ocupacion() > 0 ? 12 : 6);
        topRow.setLayoutParams(trP);

        TextView tvH = new TextView(this);
        tvH.setText(fl.hora);
        tvH.setTextSize(26f);
        tvH.setTextColor(TEXT_D);
        tvH.setTypeface(Typeface.DEFAULT_BOLD);
        tvH.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
        topRow.addView(tvH);

        TextView tvEstado = new TextView(this);
        tvEstado.setText(fl.etiquetaEstado());
        tvEstado.setTextSize(9f);
        tvEstado.setTextColor(WHITE);
        tvEstado.setTypeface(Typeface.DEFAULT_BOLD);
        tvEstado.setLetterSpacing(0.08f);
        tvEstado.setPadding(dp(10), dp(4), dp(10), dp(4));
        GradientDrawable estBg = new GradientDrawable();
        estBg.setColor(fl.colorEstado());
        estBg.setCornerRadius(dp(20));
        tvEstado.setBackground(estBg);
        LinearLayout.LayoutParams ep = new LinearLayout.LayoutParams(-2, -2);
        ep.setMarginEnd(dp(10));
        tvEstado.setLayoutParams(ep);
        topRow.addView(tvEstado);

        // Círculo contador
        FrameLayout circFrame = new FrameLayout(this);
        GradientDrawable circBg = new GradientDrawable();
        circBg.setShape(GradientDrawable.OVAL);
        circBg.setColor(BLUE_XL);
        circBg.setStroke(dp(3), Color.parseColor(String.format("#%06X", 0xFFFFFF & fl.colorEstado())));
        circFrame.setBackground(circBg);
        circFrame.setLayoutParams(new LinearLayout.LayoutParams(dp(58), dp(58)));
        LinearLayout circInner = new LinearLayout(this);
        circInner.setOrientation(LinearLayout.VERTICAL);
        circInner.setGravity(Gravity.CENTER);
        circInner.setLayoutParams(new FrameLayout.LayoutParams(-1, -1));
        TextView tvNum = new TextView(this);
        tvNum.setText(String.valueOf(fl.ocupacion()));
        tvNum.setTextSize(18f);
        tvNum.setTextColor(TEXT_D);
        tvNum.setTypeface(Typeface.DEFAULT_BOLD);
        tvNum.setGravity(Gravity.CENTER);
        circInner.addView(tvNum);
        TextView tvMax = new TextView(this);
        tvMax.setText("/" + fl.aforoMax);
        tvMax.setTextSize(9f);
        tvMax.setTextColor(TEXT_L);
        tvMax.setGravity(Gravity.CENTER);
        circInner.addView(tvMax);
        circFrame.addView(circInner);
        topRow.addView(circFrame);
        inner.addView(topRow);

        // Barra de progreso
        FrameLayout barraFrame = new FrameLayout(this);
        LinearLayout.LayoutParams bfP = new LinearLayout.LayoutParams(-1, dp(6));
        bfP.bottomMargin = dp(fl.ocupacion() > 0 ? 12 : 2);
        barraFrame.setLayoutParams(bfP);
        View fondo = new View(this);
        GradientDrawable fondoBg = new GradientDrawable();
        fondoBg.setColor(BLUE_XL);
        fondoBg.setCornerRadius(dp(6));
        fondo.setBackground(fondoBg);
        fondo.setLayoutParams(new FrameLayout.LayoutParams(-1, dp(6)));
        barraFrame.addView(fondo);
        if (fl.ocupacion() > 0) {
            View fill = new View(this);
            GradientDrawable fillBg = new GradientDrawable();
            fillBg.setColor(fl.colorEstado());
            fillBg.setCornerRadius(dp(6));
            FrameLayout.LayoutParams fillP = new FrameLayout.LayoutParams(0, dp(6));
            fill.setLayoutParams(fillP);
            fill.setBackground(fillBg);
            barraFrame.addView(fill);
            final float fpct = fl.pct();
            barraFrame.post(() -> {
                FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) fill.getLayoutParams();
                int target = (int) (barraFrame.getWidth() * fpct);
                ObjectAnimator.ofInt(fill, "right", 0, target).setDuration(500).start();
                lp.width = target;
                fill.setLayoutParams(lp);
            });
        }
        inner.addView(barraFrame);

        // Avatares personas
        if (!fl.asistentes.isEmpty()) {
            HorizontalScrollView hsv = new HorizontalScrollView(this);
            hsv.setHorizontalScrollBarEnabled(false);
            LinearLayout avatRow = new LinearLayout(this);
            avatRow.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams arp = new LinearLayout.LayoutParams(-1, -2);
            arp.bottomMargin = dp(10);
            hsv.setLayoutParams(arp);
            int[] avColors = {BLUE, 0xFFEC4899, GREEN, YELLOW, 0xFF8B5CF6, RED, 0xFF06B6D4, 0xFF64748B};
            int limit = Math.min(fl.asistentes.size(), 11);
            for (int i = 0; i < limit; i++) {
                final AsistenciaModel a = fl.asistentes.get(i);
                FrameLayout av = mkAvatar(a.clienteNombre, avColors[i % avColors.length], 36);
                LinearLayout.LayoutParams avp = new LinearLayout.LayoutParams(dp(36), dp(36));
                avp.setMarginEnd(dp(5));
                av.setLayoutParams(avp);
                av.setOnClickListener(v -> showQuitarConfirm(fl, a, dia));
                avatRow.addView(av);
            }
            if (fl.asistentes.size() > 11) {
                TextView mas = new TextView(this);
                mas.setText("+" + (fl.asistentes.size() - 11));
                mas.setTextSize(11f);
                mas.setTextColor(TEXT_L);
                mas.setTypeface(Typeface.DEFAULT_BOLD);
                mas.setPadding(dp(4), 0, 0, 0);
                mas.setGravity(Gravity.CENTER_VERTICAL);
                avatRow.addView(mas);
            }
            hsv.addView(avatRow);
            inner.addView(hsv);
        }

        // Banner FRANJA COMPLETA cuando está llena
        if (fl.llena()) {
            LinearLayout bannerLleno = new LinearLayout(this);
            bannerLleno.setOrientation(LinearLayout.HORIZONTAL);
            bannerLleno.setGravity(Gravity.CENTER);
            bannerLleno.setPadding(dp(14), dp(10), dp(14), dp(10));
            android.graphics.drawable.GradientDrawable bannerBg = new android.graphics.drawable.GradientDrawable();
            bannerBg.setColor(0x1AEF4444);
            bannerBg.setCornerRadius(dp(12));
            bannerBg.setStroke(dp(1), 0x44EF4444);
            bannerLleno.setBackground(bannerBg);
            LinearLayout.LayoutParams bannerP = new LinearLayout.LayoutParams(-1, -2);
            bannerP.topMargin = dp(6);
            bannerP.bottomMargin = dp(4);
            bannerLleno.setLayoutParams(bannerP);

            TextView tvLleno = new TextView(this);
            tvLleno.setText("🔴  FRANJA COMPLETA");
            tvLleno.setTextSize(13f);
            tvLleno.setTextColor(RED);
            tvLleno.setTypeface(Typeface.DEFAULT_BOLD);
            tvLleno.setLetterSpacing(0.06f);
            tvLleno.setGravity(Gravity.CENTER);
            bannerLleno.addView(tvLleno);
            inner.addView(bannerLleno);
        }

        TextView tvHint = new TextView(this);
        tvHint.setText(fl.llena() ? "Toca para gestionar o quitar personas"
                : fl.asistentes.isEmpty() ? "Toca para apuntar personas"
                : "Toca para ver y gestionar ›");
        tvHint.setTextSize(10.5f);
        tvHint.setTextColor(fl.llena() ? RED : fl.asistentes.isEmpty() ? TEXT_L : BLUE);
        tvHint.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(-1, -2);
        hp.topMargin = dp(2);
        tvHint.setLayoutParams(hp);
        inner.addView(tvHint);

        wrapper.addView(inner);
        card.addView(wrapper);
        card.setOnClickListener(v -> showListaSheet(fl, dia));
        return card;
    }

    // ── Resumen día ───────────────────────────────────────────────────
    private View buildResumenDia(String dia) {
        List<FranjaLocal> fs = getFranjasDia(dia);
        int tot = 0, totMax = 0, llenas = 0;
        for (FranjaLocal fl : fs) { tot += fl.ocupacion(); totMax += fl.aforoMax; if (fl.llena()) llenas++; }

        CardView card = mkCard(22);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2);
        cp.topMargin = dp(6);
        card.setLayoutParams(cp);
        GradientDrawable gradBg = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{Color.parseColor("#1D4ED8"), Color.parseColor("#2563EB")});
        gradBg.setCornerRadius(dp(22));
        card.setBackground(gradBg);
        card.setCardBackgroundColor(Color.TRANSPARENT);

        LinearLayout inner = new LinearLayout(this);
        inner.setOrientation(LinearLayout.HORIZONTAL);
        inner.setPadding(dp(22), dp(20), dp(22), dp(20));
        inner.addView(mkResKpi("Total\npersonas", String.valueOf(tot)));
        inner.addView(mkResKpi("Ocupación", totMax > 0 ? Math.round(100f * tot / totMax) + "%" : "—"));
        inner.addView(mkResKpi("Franjas\nllenas", String.valueOf(llenas)));
        inner.addView(mkResKpi("Plazas\nlibres", String.valueOf(totMax - tot)));
        card.addView(inner);
        return card;
    }

    private LinearLayout mkResKpi(String label, String val) {
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER);
        col.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
        TextView tvV = new TextView(this);
        tvV.setText(val);
        tvV.setTextSize(26f);
        tvV.setTextColor(WHITE);
        tvV.setTypeface(Typeface.DEFAULT_BOLD);
        tvV.setGravity(Gravity.CENTER);
        col.addView(tvV);
        TextView tvL = new TextView(this);
        tvL.setText(label);
        tvL.setTextSize(8.5f);
        tvL.setTextColor(Color.parseColor("#99CCFF"));
        tvL.setGravity(Gravity.CENTER);
        col.addView(tvL);
        return col;
    }

    // ════════════════════════════════════════════════════════════════
    //  VISTA SEMANA
    // ════════════════════════════════════════════════════════════════
    private void renderSemana() {
        llContenido.addView(buildNavSemana());
        llContenido.addView(mkSecLabel("RESUMEN SEMANAL", BLUE));

        String lunes = lunesDeSemana(semanaOffset);
        for (int i = 0; i < 7; i++) {
            final int dIdx = i;
            String fecha = sumarDias(lunes, i);
            final String fechaFinal = fecha;
            List<FranjaLocal> fs = getFranjasDia(fecha);
            int tot = 0, totMax = 0;
            for (FranjaLocal fl : fs) { tot += fl.ocupacion(); totMax += fl.aforoMax; }
            float pctDia = totMax > 0 ? (float) tot / totMax : 0f;
            String cBar = pctDia > 0.8f ? "#EF4444" : pctDia > 0.5f ? "#F59E0B" : "#2563EB";
            boolean esHoy = fecha.equals(hoy());
            boolean esSel = fecha.equals(selectedFecha);

            CardView card = mkCard(18);
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2);
            cp.bottomMargin = dp(10);
            card.setLayoutParams(cp);
            card.setCardBackgroundColor(esSel ? BLUE_XL : WHITE);

            LinearLayout inner = new LinearLayout(this);
            inner.setOrientation(LinearLayout.VERTICAL);
            inner.setPadding(dp(18), dp(14), dp(18), dp(14));

            LinearLayout top = new LinearLayout(this);
            top.setOrientation(LinearLayout.HORIZONTAL);
            top.setGravity(Gravity.CENTER_VERTICAL);
            setMB(top, 10);

            LinearLayout pillContent = new LinearLayout(this);
            pillContent.setOrientation(LinearLayout.VERTICAL);
            pillContent.setGravity(Gravity.CENTER);
            pillContent.setPadding(dp(12), dp(6), dp(12), dp(6));
            GradientDrawable diaBg = new GradientDrawable();
            diaBg.setColor(esHoy ? BLUE : (esSel ? BLUE_L : BLUE_XL));
            diaBg.setCornerRadius(dp(20));
            pillContent.setBackground(diaBg);
            LinearLayout.LayoutParams dpp = new LinearLayout.LayoutParams(-2, -2);
            dpp.setMarginEnd(dp(12));
            pillContent.setLayoutParams(dpp);

            TextView tvDiaLabel = new TextView(this);
            tvDiaLabel.setText(DIAS_CORTO[i]);
            tvDiaLabel.setTextSize(10f);
            tvDiaLabel.setTextColor(esHoy || esSel ? WHITE : TEXT_D);
            tvDiaLabel.setTypeface(Typeface.DEFAULT_BOLD);
            tvDiaLabel.setGravity(Gravity.CENTER);
            pillContent.addView(tvDiaLabel);
            TextView tvDNum = new TextView(this);
            tvDNum.setText(fecha.substring(8));
            tvDNum.setTextSize(14f);
            tvDNum.setTextColor(esHoy || esSel ? WHITE : TEXT_M);
            tvDNum.setTypeface(Typeface.DEFAULT_BOLD);
            tvDNum.setGravity(Gravity.CENTER);
            pillContent.addView(tvDNum);
            if (esHoy) {
                TextView tvH2 = new TextView(this);
                tvH2.setText("HOY");
                tvH2.setTextSize(7f);
                tvH2.setTextColor(Color.parseColor("#AACCFF"));
                tvH2.setGravity(Gravity.CENTER);
                tvH2.setLetterSpacing(0.1f);
                pillContent.addView(tvH2);
            }
            top.addView(pillContent);

            FrameLayout bFrame = new FrameLayout(this);
            bFrame.setLayoutParams(new LinearLayout.LayoutParams(0, dp(8), 1f));
            View bBg = new View(this);
            GradientDrawable bbg = new GradientDrawable();
            bbg.setColor(BLUE_XL);
            bbg.setCornerRadius(dp(8));
            bBg.setBackground(bbg);
            bBg.setLayoutParams(new FrameLayout.LayoutParams(-1, dp(8)));
            bFrame.addView(bBg);
            if (tot > 0) {
                View bFill = new View(this);
                GradientDrawable bFillBg = new GradientDrawable();
                bFillBg.setColor(Color.parseColor(cBar));
                bFillBg.setCornerRadius(dp(8));
                bFill.setLayoutParams(new FrameLayout.LayoutParams(0, dp(8)));
                bFill.setBackground(bFillBg);
                bFrame.addView(bFill);
                final float fpct = pctDia;
                bFrame.post(() -> {
                    FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) bFill.getLayoutParams();
                    lp.width = (int) (bFrame.getWidth() * Math.min(1f, fpct));
                    bFill.setLayoutParams(lp);
                });
            }
            top.addView(bFrame);

            TextView tvNum2 = new TextView(this);
            tvNum2.setText(tot + "/" + totMax);
            tvNum2.setTextSize(13f);
            tvNum2.setTextColor(Color.parseColor(cBar));
            tvNum2.setTypeface(Typeface.DEFAULT_BOLD);
            LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(-2, -2);
            np.setMarginStart(dp(12));
            tvNum2.setLayoutParams(np);
            top.addView(tvNum2);
            inner.addView(top);

            if (!fs.isEmpty()) {
                HorizontalScrollView hsv = new HorizontalScrollView(this);
                hsv.setHorizontalScrollBarEnabled(false);
                LinearLayout chips = new LinearLayout(this);
                chips.setOrientation(LinearLayout.HORIZONTAL);
                chips.setPadding(0, 0, dp(8), 0);
                for (FranjaLocal fl : fs) {
                    TextView chip = new TextView(this);
                    chip.setText(fl.hora + " (" + fl.ocupacion() + "/" + fl.aforoMax + ")");
                    chip.setTextSize(10f);
                    chip.setTextColor(BLUE);
                    chip.setTypeface(Typeface.DEFAULT_BOLD);
                    chip.setPadding(dp(10), dp(4), dp(10), dp(4));
                    GradientDrawable cBg2 = new GradientDrawable();
                    cBg2.setColor(BLUE_XL);
                    cBg2.setCornerRadius(dp(20));
                    chip.setBackground(cBg2);
                    LinearLayout.LayoutParams cpp = new LinearLayout.LayoutParams(-2, -2);
                    cpp.setMarginEnd(dp(6));
                    chip.setLayoutParams(cpp);
                    chips.addView(chip);
                }
                hsv.addView(chips);
                inner.addView(hsv);
            } else {
                TextView tvVac = new TextView(this);
                tvVac.setText("Sin franjas — toca para añadir");
                tvVac.setTextSize(11f);
                tvVac.setTextColor(TEXT_L);
                inner.addView(tvVac);
            }

            card.addView(inner);
            card.setOnClickListener(v -> {
                selectedFecha = fechaFinal;
                diaIdx = dIdx;
                vista = "dia";
                refreshTab(btnVistaDia, true);
                refreshTab(btnVistaSemana, false);
                refreshTab(btnVistaMes, false);
                renderDias();
                renderContenido();
            });
            llContenido.addView(card);
        }
    }

    private View buildNavSemana() {
        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(-1, -2);
        np.bottomMargin = dp(14);
        nav.setLayoutParams(np);

        String lunes = lunesDeSemana(semanaOffset);
        String labelSemana = fmtCorto(lunes) + " – " + fmtCorto(sumarDias(lunes, 6));

        TextView btnPrev = mkNavPill("‹");
        btnPrev.setOnClickListener(v -> {
            semanaOffset--;
            cargarAsistenciaSemanaAsync();
        });
        LinearLayout.LayoutParams prevP = new LinearLayout.LayoutParams(-2, -2);
        prevP.setMarginEnd(dp(8));
        btnPrev.setLayoutParams(prevP);
        nav.addView(btnPrev);

        LinearLayout centerCol = new LinearLayout(this);
        centerCol.setOrientation(LinearLayout.VERTICAL);
        centerCol.setGravity(Gravity.CENTER);
        centerCol.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
        centerCol.setPadding(dp(4), dp(8), dp(4), dp(8));
        GradientDrawable centerBg = new GradientDrawable();
        centerBg.setColor(BLUE_XL);
        centerBg.setCornerRadius(dp(14));
        centerCol.setBackground(centerBg);
        TextView tvLabel = new TextView(this);
        tvLabel.setText(labelSemana);
        tvLabel.setTextSize(13f);
        tvLabel.setTextColor(TEXT_D);
        tvLabel.setTypeface(Typeface.DEFAULT_BOLD);
        tvLabel.setGravity(Gravity.CENTER);
        centerCol.addView(tvLabel);
        TextView tvRel = new TextView(this);
        tvRel.setText(semanaOffset == 0 ? "Esta semana"
                : semanaOffset < 0 ? "Hace " + Math.abs(semanaOffset) + " semana(s)"
                : "En " + semanaOffset + " semana(s)");
        tvRel.setTextSize(9f);
        tvRel.setTextColor(semanaOffset == 0 ? BLUE : TEXT_L);
        tvRel.setTypeface(semanaOffset == 0 ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        tvRel.setGravity(Gravity.CENTER);
        centerCol.addView(tvRel);
        nav.addView(centerCol);

        TextView btnNext = mkNavPill("›");
        btnNext.setOnClickListener(v -> {
            semanaOffset++;
            cargarAsistenciaSemanaAsync();
        });
        LinearLayout.LayoutParams nextP = new LinearLayout.LayoutParams(-2, -2);
        nextP.setMarginStart(dp(8));
        btnNext.setLayoutParams(nextP);
        nav.addView(btnNext);
        return nav;
    }

    private TextView mkNavPill(String txt) {
        TextView tv = new TextView(this);
        tv.setText(txt);
        tv.setTextSize(22f);
        tv.setTextColor(BLUE);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(dp(16), dp(10), dp(16), dp(10));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(BLUE_XL);
        bg.setCornerRadius(dp(14));
        tv.setBackground(bg);
        return tv;
    }

    /** Recarga la asistencia de la semana actual (semanaOffset) */
    private void cargarAsistenciaSemanaAsync() {
        mostrarCargando();
        SupabaseRepository.get().getFranjas(new SupabaseRepository.Callback<List<FranjaModel>>() {
            @Override public void onSuccess(List<FranjaModel> franjas) {
                cargarAsistenciaSemana(franjas, semanaOffset);
            }
            @Override public void onError(String e) { renderFinal(); }
        });
    }

    // ════════════════════════════════════════════════════════════════
    //  VISTA MES
    // ════════════════════════════════════════════════════════════════
    private void renderMes() {
        llContenido.addView(buildNavMes());
        llContenido.addView(buildCalHeader());
        llContenido.addView(buildCalGrid());
        llContenido.addView(buildCalLeyenda());
    }

    private View buildNavMes() {
        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(-1, -2);
        np.bottomMargin = dp(16);
        nav.setLayoutParams(np);

        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.add(java.util.Calendar.MONTH, mesOffset);
        String mesNombre = MESES[cal.get(java.util.Calendar.MONTH)].toUpperCase();
        int anio = cal.get(java.util.Calendar.YEAR);

        TextView btnPrev = mkNavPill("‹");
        btnPrev.setOnClickListener(v -> { mesOffset--; renderContenido(); });
        nav.addView(btnPrev);

        TextView tvMes = new TextView(this);
        tvMes.setText(mesNombre + "  " + anio);
        tvMes.setTextSize(16f);
        tvMes.setTextColor(TEXT_D);
        tvMes.setTypeface(Typeface.DEFAULT_BOLD);
        tvMes.setLetterSpacing(0.04f);
        tvMes.setGravity(Gravity.CENTER);
        tvMes.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
        nav.addView(tvMes);

        TextView btnNext = mkNavPill("›");
        btnNext.setOnClickListener(v -> { mesOffset++; renderContenido(); });
        nav.addView(btnNext);
        return nav;
    }

    private View buildCalHeader() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-1, -2);
        rp.bottomMargin = dp(6);
        row.setLayoutParams(rp);
        for (String d : DIAS_MIN) {
            TextView tv = new TextView(this);
            tv.setText(d);
            tv.setTextSize(11f);
            tv.setTextColor(d.equals("S") || d.equals("D") ? BLUE_L : TEXT_L);
            tv.setTypeface(Typeface.DEFAULT_BOLD);
            tv.setGravity(Gravity.CENTER);
            tv.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
            row.addView(tv);
        }
        return row;
    }

    private View buildCalGrid() {
        LinearLayout grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);

        java.util.Calendar calMes = java.util.Calendar.getInstance();
        calMes.set(java.util.Calendar.DAY_OF_MONTH, 1);
        calMes.add(java.util.Calendar.MONTH, mesOffset);
        int diasEnMes = calMes.getActualMaximum(java.util.Calendar.DAY_OF_MONTH);
        int anioMes = calMes.get(java.util.Calendar.YEAR);
        int mesNum  = calMes.get(java.util.Calendar.MONTH) + 1;
        int dowPrimero = calMes.get(java.util.Calendar.DAY_OF_WEEK);
        int primerDiaSemana = (dowPrimero == java.util.Calendar.SUNDAY) ? 6 : dowPrimero - java.util.Calendar.MONDAY;

        LinearLayout semana = new LinearLayout(this);
        semana.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams smp = new LinearLayout.LayoutParams(-1, -2);
        smp.bottomMargin = dp(6);
        semana.setLayoutParams(smp);

        for (int i = 0; i < primerDiaSemana; i++) {
            View empty = new View(this);
            empty.setLayoutParams(new LinearLayout.LayoutParams(0, dp(60), 1f));
            semana.addView(empty);
        }

        int col = primerDiaSemana;
        for (int d = 1; d <= diasEnMes; d++) {
            String fecha = String.format(java.util.Locale.getDefault(), "%04d-%02d-%02d", anioMes, mesNum, d);
            boolean esHoy = fecha.equals(hoy());
            boolean esSel = fecha.equals(selectedFecha);
            List<FranjaLocal> fs = getFranjasDia(fecha);
            int tot = 0, totMax = 0;
            for (FranjaLocal fl : fs) { tot += fl.ocupacion(); totMax += fl.aforoMax; }
            float pctD = totMax > 0 ? (float) tot / totMax : 0f;

            FrameLayout dayFrame = new FrameLayout(this);
            LinearLayout.LayoutParams dfp = new LinearLayout.LayoutParams(0, dp(60), 1f);
            dfp.setMargins(dp(2), 0, dp(2), dp(4));
            dayFrame.setLayoutParams(dfp);

            GradientDrawable dayBg = new GradientDrawable();
            dayBg.setCornerRadius(dp(14));
            if (esHoy) dayBg.setColor(BLUE);
            else if (esSel) dayBg.setColor(BLUE_L);
            else if (tot > 0) dayBg.setColor(Color.argb((int) (40 + 180 * pctD), 37, 99, 235));
            else dayBg.setColor(BLUE_XL);
            dayFrame.setBackground(dayBg);

            LinearLayout dayInner = new LinearLayout(this);
            dayInner.setOrientation(LinearLayout.VERTICAL);
            dayInner.setGravity(Gravity.CENTER);
            dayInner.setLayoutParams(new FrameLayout.LayoutParams(-1, -1));

            TextView tvDay = new TextView(this);
            tvDay.setText(String.valueOf(d));
            tvDay.setTextSize(15f);
            tvDay.setTextColor(esHoy || esSel ? WHITE : tot > 0 ? Color.parseColor("#1D4ED8") : TEXT_L);
            tvDay.setTypeface(tot > 0 || esHoy || esSel ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
            tvDay.setGravity(Gravity.CENTER);
            dayInner.addView(tvDay);

            if (tot > 0 && !esHoy && !esSel) {
                FrameLayout miniBar = new FrameLayout(this);
                LinearLayout.LayoutParams mbp = new LinearLayout.LayoutParams(dp(32), dp(3));
                mbp.gravity = Gravity.CENTER_HORIZONTAL;
                mbp.topMargin = dp(3);
                miniBar.setLayoutParams(mbp);
                View mbBg = new View(this);
                GradientDrawable mbBgd = new GradientDrawable();
                mbBgd.setColor(BLUE_XX);
                mbBgd.setCornerRadius(dp(3));
                mbBg.setBackground(mbBgd);
                mbBg.setLayoutParams(new FrameLayout.LayoutParams(-1, -1));
                miniBar.addView(mbBg);
                View mbFill = new View(this);
                GradientDrawable mbFd = new GradientDrawable();
                mbFd.setColor(pctD > 0.8f ? RED : pctD > 0.5f ? YELLOW : BLUE);
                mbFd.setCornerRadius(dp(3));
                mbFill.setLayoutParams(new FrameLayout.LayoutParams((int) (dp(32) * Math.min(1f, pctD)), -1));
                mbFill.setBackground(mbFd);
                miniBar.addView(mbFill);
                dayInner.addView(miniBar);
            }
            if (tot > 0) {
                TextView tvTot = new TextView(this);
                tvTot.setText(String.valueOf(tot));
                tvTot.setTextSize(8f);
                tvTot.setTextColor(esHoy || esSel ? Color.parseColor("#AACCFF") : Color.parseColor("#3B82F6"));
                tvTot.setGravity(Gravity.CENTER);
                dayInner.addView(tvTot);
            }
            dayFrame.addView(dayInner);

            final String fechaFinal = fecha;
            dayFrame.setOnClickListener(v -> {
                selectedFecha = fechaFinal;
                diaIdx = diaSemanaIdx(fechaFinal);
                // Calcular semanaOffset correcto
                String lunesHoy  = lunesDeSemana(0);
                String lunesFecha = lunesBase(fechaFinal);
                try {
                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
                    long diffMs = sdf.parse(lunesFecha).getTime() - sdf.parse(lunesHoy).getTime();
                    semanaOffset = (int) (diffMs / (7L * 24 * 3600 * 1000));
                } catch (Exception ignored) {}
                vista = "dia";
                refreshTab(btnVistaDia, true);
                refreshTab(btnVistaSemana, false);
                refreshTab(btnVistaMes, false);
                if (!franjasPorFecha.containsKey(fechaFinal) ||
                        franjasPorFecha.get(fechaFinal).isEmpty()) {
                    recargarFecha(fechaFinal);
                } else {
                    renderDias();
                    renderContenido();
                }
            });

            semana.addView(dayFrame);
            col++;
            if (col % 7 == 0) {
                grid.addView(semana);
                semana = new LinearLayout(this);
                semana.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams smp2 = new LinearLayout.LayoutParams(-1, -2);
                smp2.bottomMargin = dp(6);
                semana.setLayoutParams(smp2);
            }
        }
        if (semana.getChildCount() > 0) {
            while (semana.getChildCount() < 7) {
                View empty = new View(this);
                empty.setLayoutParams(new LinearLayout.LayoutParams(0, dp(60), 1f));
                semana.addView(empty);
            }
            grid.addView(semana);
        }
        return grid;
    }

    private View buildCalLeyenda() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-1, -2);
        rp.topMargin = dp(12);
        row.setLayoutParams(rp);
        String[][] items = {{"Sin datos","#EEF4FF"},{"Poca ocupación","#93C5FD"},{"Alta ocupación","#1D4ED8"},{"Hoy","#2563EB"}};
        for (String[] item : items) {
            View dot = new View(this);
            GradientDrawable dBg = new GradientDrawable();
            dBg.setShape(GradientDrawable.OVAL);
            dBg.setColor(Color.parseColor(item[1]));
            dot.setBackground(dBg);
            LinearLayout.LayoutParams dp2 = new LinearLayout.LayoutParams(dp(10), dp(10));
            dp2.gravity = Gravity.CENTER_VERTICAL;
            dp2.setMarginEnd(dp(4));
            dot.setLayoutParams(dp2);
            row.addView(dot);
            TextView tv = new TextView(this);
            tv.setText(item[0]);
            tv.setTextSize(10f);
            tv.setTextColor(TEXT_L);
            LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(-2, -2);
            tp.setMarginEnd(dp(14));
            tv.setLayoutParams(tp);
            row.addView(tv);
        }
        return row;
    }

    // ════════════════════════════════════════════════════════════════
    //  SHEET: LISTA PERSONAS DE UNA FRANJA
    // ════════════════════════════════════════════════════════════════
    private void showListaSheet(FranjaLocal fl, String dia) {
        BottomSheetDialog sheet = mkSheet();

        // NestedScrollView permite scroll dentro del BottomSheet sin conflicto
        androidx.core.widget.NestedScrollView sv = new androidx.core.widget.NestedScrollView(this);
        sv.setFillViewport(true);
        sv.setNestedScrollingEnabled(true);

        // Altura fija para que el sheet tenga espacio real de scroll
        int alturaSheet = (int)(getResources().getDisplayMetrics().heightPixels * 0.88f);
        sv.setLayoutParams(new android.view.ViewGroup.LayoutParams(-1, alturaSheet));

        LinearLayout root = mkSheetRoot();
        sv.addView(root);
        root.addView(mkHandle());

        // Header gradiente
        LinearLayout hdrCard = new LinearLayout(this);
        hdrCard.setOrientation(LinearLayout.VERTICAL);
        hdrCard.setPadding(dp(20), dp(18), dp(20), dp(16));
        GradientDrawable hdrBg = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{Color.parseColor("#1D4ED8"), Color.parseColor("#3B82F6")});
        hdrBg.setCornerRadius(dp(20));
        hdrCard.setBackground(hdrBg);
        LinearLayout.LayoutParams hcp = new LinearLayout.LayoutParams(-1, -2);
        hcp.bottomMargin = dp(20);
        hdrCard.setLayoutParams(hcp);
        root.addView(hdrCard);

        LinearLayout hdrTop = new LinearLayout(this);
        hdrTop.setOrientation(LinearLayout.HORIZONTAL);
        hdrTop.setGravity(Gravity.CENTER_VERTICAL);
        setMB(hdrTop, 14);
        hdrCard.addView(hdrTop);

        LinearLayout hdrTxt = new LinearLayout(this);
        hdrTxt.setOrientation(LinearLayout.VERTICAL);
        hdrTxt.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
        TextView tvSup = new TextView(this);
        tvSup.setText(dia.toUpperCase() + "  ·  FRANJA");
        tvSup.setTextSize(9f);
        tvSup.setTextColor(Color.parseColor("#99CCFF"));
        tvSup.setTypeface(Typeface.DEFAULT_BOLD);
        tvSup.setLetterSpacing(0.15f);
        setMB(tvSup, 2);
        hdrTxt.addView(tvSup);
        TextView tvH2 = new TextView(this);
        tvH2.setText(fl.hora);
        tvH2.setTextSize(44f);
        tvH2.setTextColor(WHITE);
        tvH2.setTypeface(Typeface.DEFAULT_BOLD);
        hdrTxt.addView(tvH2);
        hdrTop.addView(hdrTxt);

        FrameLayout circHdr = new FrameLayout(this);
        GradientDrawable circBg = new GradientDrawable();
        circBg.setShape(GradientDrawable.OVAL);
        circBg.setColor(0x33FFFFFF);
        circHdr.setBackground(circBg);
        circHdr.setLayoutParams(new LinearLayout.LayoutParams(dp(64), dp(64)));
        LinearLayout circIn = new LinearLayout(this);
        circIn.setOrientation(LinearLayout.VERTICAL);
        circIn.setGravity(Gravity.CENTER);
        circIn.setLayoutParams(new FrameLayout.LayoutParams(-1, -1));
        TextView tvCN = new TextView(this);
        tvCN.setText(String.valueOf(fl.ocupacion()));
        tvCN.setTextSize(22f);
        tvCN.setTextColor(WHITE);
        tvCN.setTypeface(Typeface.DEFAULT_BOLD);
        tvCN.setGravity(Gravity.CENTER);
        circIn.addView(tvCN);
        TextView tvCM = new TextView(this);
        tvCM.setText("/" + fl.aforoMax);
        tvCM.setTextSize(10f);
        tvCM.setTextColor(Color.parseColor("#AACCFF"));
        tvCM.setGravity(Gravity.CENTER);
        circIn.addView(tvCM);
        circHdr.addView(circIn);
        hdrTop.addView(circHdr);

        // Botones de acción en header
        LinearLayout accionRow = new LinearLayout(this);
        accionRow.setOrientation(LinearLayout.HORIZONTAL);
        accionRow.setGravity(Gravity.CENTER_VERTICAL);
        hdrCard.addView(accionRow);

        if (!fl.llena()) {
            TextView btnAp = mkPillBtn("＋ Apuntar", WHITE, BLUE);
            LinearLayout.LayoutParams app = new LinearLayout.LayoutParams(-2, -2);
            app.setMarginEnd(dp(8));
            btnAp.setLayoutParams(app);
            btnAp.setOnClickListener(v -> { sheet.dismiss(); showApuntarSheet(fl, dia); });
            accionRow.addView(btnAp);
        }

        TextView btnEdit = mkPillBtn("✏ Editar franja", 0x33FFFFFF, Color.parseColor("#AACCFF"));
        LinearLayout.LayoutParams edp = new LinearLayout.LayoutParams(-2, -2);
        edp.setMarginEnd(dp(8));
        btnEdit.setLayoutParams(edp);
        btnEdit.setOnClickListener(v -> { sheet.dismiss(); showEditarFranjaSheet(fl, dia); });
        accionRow.addView(btnEdit);

        if (!fl.asistentes.isEmpty()) {
            TextView btnVac = mkPillBtn("🗑 Vaciar", 0x33EF4444, Color.parseColor("#FCA5A5"));
            btnVac.setOnClickListener(v -> {
                // Quitar todos de Supabase
                for (AsistenciaModel a : new ArrayList<>(fl.asistentes)) {
                    SupabaseRepository.get().quitarPersona(dia, fl.id, a.clienteNombre,
                            new SupabaseRepository.Callback<Void>() {
                                @Override public void onSuccess(Void d2) {}
                                @Override public void onError(String e) {}
                            });
                }
                fl.asistentes.clear();
                sheet.dismiss();
                renderDias(); renderContenido(); updateKpis();
                Toast.makeText(this, "Franja " + fl.hora + " vaciada", Toast.LENGTH_SHORT).show();
            });
            accionRow.addView(btnVac);
        }

        // Lista personas — contenedor separado para poder actualizarla sin cerrar el sheet
        LinearLayout listaPersonas = new LinearLayout(this);
        listaPersonas.setOrientation(LinearLayout.VERTICAL);
        root.addView(listaPersonas);

        int[] avColors = {BLUE,0xFFEC4899,GREEN,YELLOW,0xFF8B5CF6,RED,0xFF06B6D4,0xFF64748B,0xFF0EA5E9,0xFFD97706,0xFF7C3AED,0xFF059669};

        // Runnable que re-renderiza la lista SIN cerrar el sheet
        Runnable[] refRender = {null};
        Runnable renderPersonas = () -> {
            listaPersonas.removeAllViews();

            // Actualizar el círculo del header con el nuevo conteo
            tvCN.setText(String.valueOf(fl.ocupacion()));
            tvCM.setText("/" + fl.aforoMax);

            if (fl.asistentes.isEmpty()) {
                listaPersonas.addView(buildEmpty("Sin personas apuntadas", "Toca Apuntar para añadir"));
                return;
            }
            for (int i = 0; i < fl.asistentes.size(); i++) {
                final AsistenciaModel a = fl.asistentes.get(i);
                final int ca = avColors[i % avColors.length];
                final int plazaNum = i + 1;

                LinearLayout fila = new LinearLayout(this);
                fila.setOrientation(LinearLayout.HORIZONTAL);
                fila.setGravity(Gravity.CENTER_VERTICAL);
                fila.setPadding(dp(16), dp(14), dp(16), dp(14));
                LinearLayout.LayoutParams fp = new LinearLayout.LayoutParams(-1, -2);
                fp.bottomMargin = dp(8);
                fila.setLayoutParams(fp);
                GradientDrawable filaBg = new GradientDrawable();
                filaBg.setColor(BLUE_XL);
                filaBg.setCornerRadius(dp(18));
                fila.setBackground(filaBg);

                FrameLayout av = mkAvatar(a.clienteNombre, ca, 44);
                LinearLayout.LayoutParams avp = new LinearLayout.LayoutParams(dp(44), dp(44));
                avp.setMarginEnd(dp(14));
                av.setLayoutParams(avp);
                fila.addView(av);

                LinearLayout nc = new LinearLayout(this);
                nc.setOrientation(LinearLayout.VERTICAL);
                nc.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
                TextView tvN = new TextView(this);
                tvN.setText(a.clienteNombre);
                tvN.setTextSize(15f);
                tvN.setTextColor(TEXT_D);
                tvN.setTypeface(Typeface.DEFAULT_BOLD);
                nc.addView(tvN);
                TextView tvPos = new TextView(this);
                tvPos.setText("Plaza #" + plazaNum);
                tvPos.setTextSize(10f);
                tvPos.setTextColor(TEXT_L);
                nc.addView(tvPos);
                fila.addView(nc);

                TextView btnQ = new TextView(this);
                btnQ.setText("✕");
                btnQ.setTextSize(16f);
                btnQ.setTextColor(RED);
                btnQ.setGravity(Gravity.CENTER);
                btnQ.setPadding(dp(10), dp(6), dp(10), dp(6));
                GradientDrawable qBg = new GradientDrawable();
                qBg.setColor(0x1FEF4444);
                qBg.setCornerRadius(dp(12));
                btnQ.setBackground(qBg);
                btnQ.setOnClickListener(v -> {
                    btnQ.setEnabled(false);
                    btnQ.setText("…");
                    SupabaseRepository.get().quitarPersona(dia, fl.id, a.clienteNombre,
                            new SupabaseRepository.Callback<Void>() {
                                @Override public void onSuccess(Void data) {
                                    runOnUiThread(() -> {
                                        fl.asistentes.remove(a);
                                        // Re-renderizar la lista SIN cerrar el sheet
                                        refRender[0].run();
                                        // Actualizar también el fondo y los KPIs
                                        renderDias();
                                        renderContenido();
                                        updateKpis();
                                        Toast.makeText(GimnasioActivity.this,
                                                a.clienteNombre.split(" ")[0] + " eliminado/a",
                                                Toast.LENGTH_SHORT).show();
                                    });
                                }
                                @Override public void onError(String e) {
                                    runOnUiThread(() -> {
                                        btnQ.setEnabled(true);
                                        btnQ.setText("✕");
                                        Toast.makeText(GimnasioActivity.this,
                                                "Error: " + e, Toast.LENGTH_SHORT).show();
                                    });
                                }
                            });
                });
                fila.addView(btnQ);
                listaPersonas.addView(fila);
            }
        };
        refRender[0] = renderPersonas;
        renderPersonas.run();
        sheet.setContentView(sv);
        sheet.setOnShowListener(d -> {
            com.google.android.material.bottomsheet.BottomSheetBehavior<?> beh =
                    com.google.android.material.bottomsheet.BottomSheetBehavior.from(
                            (android.view.View) sv.getParent());
            beh.setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED);
            beh.setSkipCollapsed(true);
        });
        sheet.show();
    }

    // ════════════════════════════════════════════════════════════════
    //  SHEET: APUNTAR PERSONA
    // ════════════════════════════════════════════════════════════════
    private void showApuntarSheet(FranjaLocal fl, String dia) {
        BottomSheetDialog sheet = mkSheet();

        androidx.core.widget.NestedScrollView sv = new androidx.core.widget.NestedScrollView(this);
        sv.setFillViewport(true);
        sv.setNestedScrollingEnabled(true);
        int alturaSheet = (int)(getResources().getDisplayMetrics().heightPixels * 0.92f);
        sv.setLayoutParams(new android.view.ViewGroup.LayoutParams(-1, alturaSheet));

        LinearLayout root = mkSheetRoot();
        sv.addView(root);
        root.addView(mkHandle());

        // Header compacto
        LinearLayout hdr = new LinearLayout(this);
        hdr.setOrientation(LinearLayout.VERTICAL);
        hdr.setPadding(dp(20), dp(18), dp(20), dp(18));
        GradientDrawable hdrBg = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{Color.parseColor("#1D4ED8"), Color.parseColor("#3B82F6")});
        hdrBg.setCornerRadius(dp(20));
        hdr.setBackground(hdrBg);
        setMB(hdr, 20);
        root.addView(hdr);

        TextView tvSup = new TextView(this);
        tvSup.setText("APUNTAR PERSONA");
        tvSup.setTextSize(9f);
        tvSup.setTextColor(Color.parseColor("#99CCFF"));
        tvSup.setTypeface(Typeface.DEFAULT_BOLD);
        tvSup.setLetterSpacing(0.2f);
        hdr.addView(tvSup);
        TextView tvHora = new TextView(this);
        tvHora.setText(fl.hora);
        tvHora.setTextSize(48f);
        tvHora.setTextColor(WHITE);
        tvHora.setTypeface(Typeface.DEFAULT_BOLD);
        hdr.addView(tvHora);
        TextView tvPlazas = new TextView(this);
        tvPlazas.setText((fl.aforoMax - fl.ocupacion()) + " plazas libres");
        tvPlazas.setTextSize(11f);
        tvPlazas.setTextColor(WHITE);
        tvPlazas.setTypeface(Typeface.DEFAULT_BOLD);
        tvPlazas.setPadding(dp(12), dp(5), dp(12), dp(5));
        GradientDrawable plBg = new GradientDrawable();
        plBg.setColor(0x33FFFFFF);
        plBg.setCornerRadius(dp(20));
        tvPlazas.setBackground(plBg);
        hdr.addView(tvPlazas);

        // Buscador — prerellenar con el cliente si venimos de ClientesActivity
        root.addView(mkSheetLabel("BUSCAR CLIENTE"));
        EditText etBuscar = mkEditText("Escribe un nombre...");
        setMB(etBuscar, 10);
        if (clientePreseleccionado != null && !clientePreseleccionado.isEmpty()) {
            etBuscar.setText(clientePreseleccionado);
            etBuscar.setSelection(clientePreseleccionado.length());
        }
        root.addView(etBuscar);

        // Lista de clientes — scroll independiente del sheet principal
        androidx.core.widget.NestedScrollView svLista = new androidx.core.widget.NestedScrollView(this);
        svLista.setNestedScrollingEnabled(true);
        svLista.setFillViewport(true);
        LinearLayout listaView = new LinearLayout(this);
        listaView.setOrientation(LinearLayout.VERTICAL);
        svLista.addView(listaView);
        LinearLayout.LayoutParams svListaP = new LinearLayout.LayoutParams(-1, dp(260));
        svListaP.bottomMargin = dp(8);
        svLista.setLayoutParams(svListaP);
        root.addView(svLista);

        // Separador o
        root.addView(buildOrSep());

        // Nombre libre
        EditText etLibre = mkEditText("O escribe nombre libre...");
        LinearLayout.LayoutParams elp = new LinearLayout.LayoutParams(-1, -2);
        elp.topMargin = dp(10);
        elp.bottomMargin = dp(14);
        etLibre.setLayoutParams(elp);
        root.addView(etLibre);

        TextView btnOk = mkFullBtn("AÑADIR NOMBRE LIBRE", BLUE);
        root.addView(btnOk);

        int[] avCols = {BLUE,0xFFEC4899,GREEN,YELLOW,0xFF8B5CF6,RED,0xFF06B6D4,0xFF64748B};
        Runnable[] renderListaRef = new Runnable[1];

        Runnable renderLista = () -> {
            listaView.removeAllViews();
            // Actualizar contador de plazas en el header
            tvPlazas.setText((fl.aforoMax - fl.ocupacion()) + " plazas libres");
            tvPlazas.setTextColor(fl.llena() ? RED : WHITE);

            // Si la franja está llena, mostrar aviso en lugar de la lista
            if (fl.llena()) {
                LinearLayout bannerLleno2 = new LinearLayout(this);
                bannerLleno2.setOrientation(LinearLayout.VERTICAL);
                bannerLleno2.setGravity(Gravity.CENTER);
                bannerLleno2.setPadding(dp(20), dp(24), dp(20), dp(24));
                android.graphics.drawable.GradientDrawable bBg2 = new android.graphics.drawable.GradientDrawable();
                bBg2.setColor(0x1AEF4444); bBg2.setCornerRadius(dp(16));
                bBg2.setStroke(dp(1), 0x44EF4444);
                bannerLleno2.setBackground(bBg2);
                LinearLayout.LayoutParams bbP2 = new LinearLayout.LayoutParams(-1, -2);
                bbP2.bottomMargin = dp(12);
                bannerLleno2.setLayoutParams(bbP2);
                TextView tvLleno2 = new TextView(this);
                tvLleno2.setText("🔴");
                tvLleno2.setTextSize(36f);
                tvLleno2.setGravity(Gravity.CENTER);
                bannerLleno2.addView(tvLleno2);
                TextView tvLleno3 = new TextView(this);
                tvLleno3.setText("FRANJA COMPLETA");
                tvLleno3.setTextSize(16f);
                tvLleno3.setTextColor(RED);
                tvLleno3.setTypeface(Typeface.DEFAULT_BOLD);
                tvLleno3.setLetterSpacing(0.08f);
                tvLleno3.setGravity(Gravity.CENTER);
                LinearLayout.LayoutParams ll3P = new LinearLayout.LayoutParams(-1, -2);
                ll3P.topMargin = dp(8);
                tvLleno3.setLayoutParams(ll3P);
                bannerLleno2.addView(tvLleno3);
                TextView tvLleno4 = new TextView(this);
                tvLleno4.setText("No hay más plazas disponibles");
                tvLleno4.setTextSize(11f);
                tvLleno4.setTextColor(TEXT_L);
                tvLleno4.setGravity(Gravity.CENTER);
                LinearLayout.LayoutParams ll4P = new LinearLayout.LayoutParams(-1, -2);
                ll4P.topMargin = dp(4);
                tvLleno4.setLayoutParams(ll4P);
                bannerLleno2.addView(tvLleno4);
                listaView.addView(bannerLleno2);
                return;
            }

            String q = etBuscar.getText().toString().toLowerCase().trim();
            List<String> yaApuntados = new ArrayList<>();
            for (AsistenciaModel a : fl.asistentes) yaApuntados.add(a.clienteNombre);
            int ci = 0;
            for (String nombre : clientesPool) {
                if (!q.isEmpty() && !nombre.toLowerCase().contains(q)) continue;
                if (yaApuntados.contains(nombre)) continue;
                int colorAv = avCols[ci % avCols.length];
                final String nomFinal = nombre;
                listaView.addView(buildClienteRow(nombre, colorAv, () -> {
                    if (fl.llena()) { Toast.makeText(this, "Franja llena", Toast.LENGTH_SHORT).show(); return; }
                    SupabaseRepository.get().apuntarPersona(dia, fl.id, null, nomFinal,
                            new SupabaseRepository.Callback<Void>() {
                                @Override public void onSuccess(Void data) {
                                    AsistenciaModel nueva = new AsistenciaModel();
                                    nueva.clienteNombre = nomFinal;
                                    nueva.fecha = dia;
                                    nueva.horarioSemanalId = fl.id;
                                    fl.asistentes.add(nueva);
                                    runOnUiThread(() -> {
                                        renderContenido();
                                        renderDias();
                                        updateKpis();
                                        // Limpiar el buscador → muestra todos los clientes
                                        etBuscar.setText("");
                                        clientePreseleccionado = null;
                                        renderListaRef[0].run();
                                        if (fl.llena()) {
                                            Toast.makeText(GimnasioActivity.this,
                                                    "✓ " + nomFinal.split(" ")[0] + " apuntado/a · Franja completa 🔴",
                                                    Toast.LENGTH_LONG).show();
                                        } else {
                                            Toast.makeText(GimnasioActivity.this,
                                                    "✓ " + nomFinal.split(" ")[0] + " apuntado/a",
                                                    Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                }
                                @Override public void onError(String e) {
                                    runOnUiThread(() -> Toast.makeText(GimnasioActivity.this,
                                            "Error: " + e, Toast.LENGTH_SHORT).show());
                                }
                            });
                }));
                ci++;
            }
            if (listaView.getChildCount() == 0) {
                TextView tvV = new TextView(this);
                tvV.setText(q.isEmpty() ? "Sin clientes disponibles" : "Sin resultados · escribe abajo");
                tvV.setTextSize(12f);
                tvV.setTextColor(TEXT_L);
                tvV.setPadding(dp(4), dp(8), 0, 0);
                listaView.addView(tvV);
            }
        };
        renderListaRef[0] = renderLista;
        renderLista.run();

        etBuscar.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            public void onTextChanged(CharSequence s, int a, int b, int c) { renderLista.run(); }
            public void afterTextChanged(Editable s) {}
        });

        btnOk.setOnClickListener(v -> {
            String n = etLibre.getText().toString().trim();
            if (n.isEmpty()) { Toast.makeText(this, "Escribe el nombre", Toast.LENGTH_SHORT).show(); return; }
            List<String> yaApuntados2 = new ArrayList<>();
            for (AsistenciaModel a : fl.asistentes) yaApuntados2.add(a.clienteNombre);
            if (yaApuntados2.contains(n)) { Toast.makeText(this, n + " ya está apuntado/a", Toast.LENGTH_SHORT).show(); return; }
            if (fl.llena()) { Toast.makeText(this, "Franja llena", Toast.LENGTH_SHORT).show(); return; }

            btnOk.setEnabled(false);
            SupabaseRepository.get().apuntarPersona(dia, fl.id, null, n,
                    new SupabaseRepository.Callback<Void>() {
                        @Override public void onSuccess(Void data) {
                            AsistenciaModel nueva = new AsistenciaModel();
                            nueva.clienteNombre = n;
                            nueva.fecha = dia;
                            nueva.horarioSemanalId = fl.id;
                            fl.asistentes.add(nueva);
                            if (!clientesPool.contains(n)) clientesPool.add(n);
                            runOnUiThread(() -> {
                                btnOk.setEnabled(true);
                                etLibre.setText("");
                                renderContenido(); renderDias(); updateKpis();
                                renderListaRef[0].run();
                                Toast.makeText(GimnasioActivity.this,
                                        "✓ " + n.split(" ")[0] + " apuntado/a", Toast.LENGTH_SHORT).show();
                            });
                        }
                        @Override public void onError(String e) {
                            runOnUiThread(() -> {
                                btnOk.setEnabled(true);
                                Toast.makeText(GimnasioActivity.this, "Error: " + e, Toast.LENGTH_SHORT).show();
                            });
                        }
                    });
        });

        TextView btnCerrar = mkFullBtn("CERRAR", BLUE_XL);
        btnCerrar.setTextColor(TEXT_M);
        LinearLayout.LayoutParams bcp = new LinearLayout.LayoutParams(-1, -2);
        bcp.topMargin = dp(8);
        btnCerrar.setLayoutParams(bcp);
        btnCerrar.setOnClickListener(v -> { sheet.dismiss(); renderContenido(); renderDias(); updateKpis(); });
        root.addView(btnCerrar);

        sheet.setContentView(sv);
        sheet.setOnShowListener(d -> {
            com.google.android.material.bottomsheet.BottomSheetBehavior<?> beh =
                    com.google.android.material.bottomsheet.BottomSheetBehavior.from(
                            (android.view.View) sv.getParent());
            beh.setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED);
            beh.setSkipCollapsed(true);
        });
        sheet.show();
    }

    private View buildClienteRow(String nombre, int avColor, Runnable onAdd) {
        LinearLayout fila = new LinearLayout(this);
        fila.setOrientation(LinearLayout.HORIZONTAL);
        fila.setGravity(Gravity.CENTER_VERTICAL);
        fila.setPadding(dp(14), dp(11), dp(14), dp(11));
        LinearLayout.LayoutParams fp = new LinearLayout.LayoutParams(-1, -2);
        fp.bottomMargin = dp(6);
        fila.setLayoutParams(fp);
        GradientDrawable filaBg = new GradientDrawable();
        filaBg.setColor(BLUE_XL);
        filaBg.setCornerRadius(dp(16));
        fila.setBackground(filaBg);
        FrameLayout av = mkAvatar(nombre, avColor, 38);
        LinearLayout.LayoutParams avp = new LinearLayout.LayoutParams(dp(38), dp(38));
        avp.setMarginEnd(dp(12));
        av.setLayoutParams(avp);
        fila.addView(av);
        TextView tvN = new TextView(this);
        tvN.setText(nombre);
        tvN.setTextSize(14f);
        tvN.setTextColor(TEXT_D);
        tvN.setTypeface(Typeface.DEFAULT_BOLD);
        tvN.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
        fila.addView(tvN);
        TextView btnAdd = new TextView(this);
        btnAdd.setText("＋");
        btnAdd.setTextSize(20f);
        btnAdd.setTextColor(BLUE);
        btnAdd.setTypeface(Typeface.DEFAULT_BOLD);
        btnAdd.setGravity(Gravity.CENTER);
        btnAdd.setPadding(dp(10), dp(4), dp(10), dp(4));
        fila.addView(btnAdd);
        fila.setOnClickListener(v -> onAdd.run());
        btnAdd.setOnClickListener(v -> onAdd.run());
        return fila;
    }

    // ════════════════════════════════════════════════════════════════
    //  SHEET: QUITAR CONFIRMAR
    // ════════════════════════════════════════════════════════════════
    private void showQuitarConfirm(FranjaLocal fl, AsistenciaModel a, String dia) {
        BottomSheetDialog sheet = mkSheet();
        LinearLayout root = mkSheetRoot();
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(mkHandle());

        FrameLayout av = mkAvatar(a.clienteNombre, BLUE, 70);
        LinearLayout.LayoutParams avp = new LinearLayout.LayoutParams(dp(70), dp(70));
        avp.gravity = Gravity.CENTER_HORIZONTAL;
        avp.bottomMargin = dp(14);
        av.setLayoutParams(avp);
        root.addView(av);

        TextView tvN = new TextView(this);
        tvN.setText(a.clienteNombre);
        tvN.setTextSize(22f);
        tvN.setTextColor(TEXT_D);
        tvN.setTypeface(Typeface.DEFAULT_BOLD);
        tvN.setGravity(Gravity.CENTER);
        setMB(tvN, 6);
        root.addView(tvN);

        TextView tvS = new TextView(this);
        tvS.setText("¿Quitar de " + fl.hora + "  ·  " + dia + "?");
        tvS.setTextSize(13f);
        tvS.setTextColor(TEXT_L);
        tvS.setGravity(Gravity.CENTER);
        setMB(tvS, 28);
        root.addView(tvS);

        LinearLayout btns = new LinearLayout(this);
        btns.setOrientation(LinearLayout.HORIZONTAL);
        root.addView(btns);

        TextView btnC = mkBtn("Cancelar", BLUE_XL, TEXT_M, false);
        LinearLayout.LayoutParams bcp = new LinearLayout.LayoutParams(0, -2, 1f);
        bcp.setMarginEnd(dp(10));
        btnC.setLayoutParams(bcp);
        btnC.setGravity(Gravity.CENTER);
        btnC.setPadding(0, dp(14), 0, dp(14));
        btnC.setOnClickListener(v -> sheet.dismiss());
        btns.addView(btnC);

        TextView btnQ = mkBtn("Quitar", RED, WHITE, true);
        btnQ.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
        btnQ.setGravity(Gravity.CENTER);
        btnQ.setPadding(0, dp(14), 0, dp(14));
        btnQ.setOnClickListener(v -> {
            btnQ.setEnabled(false);
            SupabaseRepository.get().quitarPersona(dia, fl.id, a.clienteNombre,
                    new SupabaseRepository.Callback<Void>() {
                        @Override public void onSuccess(Void data) {
                            runOnUiThread(() -> {
                                fl.asistentes.remove(a);
                                sheet.dismiss();
                                renderDias(); renderContenido(); updateKpis();
                                Toast.makeText(GimnasioActivity.this,
                                        a.clienteNombre.split(" ")[0] + " eliminado/a",
                                        Toast.LENGTH_SHORT).show();
                            });
                        }
                        @Override public void onError(String e) {
                            runOnUiThread(() -> {
                                btnQ.setEnabled(true);
                                Toast.makeText(GimnasioActivity.this, "Error: " + e, Toast.LENGTH_SHORT).show();
                            });
                        }
                    });
        });
        btns.addView(btnQ);
        sheet.setContentView(root);
        sheet.show();
    }

    // ════════════════════════════════════════════════════════════════
    //  SHEET: GESTIONAR FRANJAS
    // ════════════════════════════════════════════════════════════════
    private void showGestionarFranjasSheet(String dia) {
        BottomSheetDialog sheet = mkSheet();
        ScrollView sv = new ScrollView(this);
        sv.setVerticalScrollBarEnabled(false);
        LinearLayout root = mkSheetRoot();
        sv.addView(root);
        root.addView(mkHandle());
        root.addView(mkSheetTitle("Franjas del " + fmtLargo(dia)));
        root.addView(mkSheetLabel("Franjas existentes · toca para editar o eliminar"));

        List<FranjaLocal> fs = getFranjasDia(dia);
        if (fs.isEmpty()) {
            root.addView(buildEmpty("Sin franjas", "Añade tu primera franja"));
        } else {
            for (FranjaLocal fl : new ArrayList<>(fs)) {
                LinearLayout fila = new LinearLayout(this);
                fila.setOrientation(LinearLayout.HORIZONTAL);
                fila.setGravity(Gravity.CENTER_VERTICAL);
                fila.setPadding(dp(16), dp(14), dp(16), dp(14));
                LinearLayout.LayoutParams fp = new LinearLayout.LayoutParams(-1, -2);
                fp.bottomMargin = dp(8);
                fila.setLayoutParams(fp);
                GradientDrawable filaBg = new GradientDrawable();
                filaBg.setColor(BLUE_XL);
                filaBg.setCornerRadius(dp(18));
                fila.setBackground(filaBg);

                TextView tvH2 = new TextView(this);
                tvH2.setText(fl.hora);
                tvH2.setTextSize(20f);
                tvH2.setTextColor(TEXT_D);
                tvH2.setTypeface(Typeface.DEFAULT_BOLD);
                tvH2.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
                fila.addView(tvH2);

                TextView tvAf = new TextView(this);
                tvAf.setText(fl.ocupacion() + "/" + fl.aforoMax + " plazas");
                tvAf.setTextSize(12f);
                tvAf.setTextColor(BLUE);
                tvAf.setTypeface(Typeface.DEFAULT_BOLD);
                LinearLayout.LayoutParams afp = new LinearLayout.LayoutParams(-2, -2);
                afp.setMarginEnd(dp(10));
                tvAf.setLayoutParams(afp);
                fila.addView(tvAf);

                TextView btnEd = mkBtn("✏", BLUE_XX, BLUE, false);
                setME(btnEd, 6);
                btnEd.setOnClickListener(v -> { sheet.dismiss(); showEditarFranjaSheet(fl, dia); });
                fila.addView(btnEd);

                TextView btnDel = mkBtn("✕", 0xFFFEF2F2, RED, false);
                btnDel.setOnClickListener(v -> {
                    if (fl.id == null) {
                        getFranjasDia(dia).remove(fl);
                        sheet.dismiss(); renderContenido(); renderDias();
                        return;
                    }
                    btnDel.setEnabled(false);
                    SupabaseRepository.get().eliminarFranja(fl.id,
                            new SupabaseRepository.Callback<Void>() {
                                @Override public void onSuccess(Void data) {
                                    runOnUiThread(() -> {
                                        getFranjasDia(dia).remove(fl);
                                        sheet.dismiss(); renderContenido(); renderDias();
                                        Toast.makeText(GimnasioActivity.this,
                                                "Franja " + fl.hora + " eliminada", Toast.LENGTH_SHORT).show();
                                    });
                                }
                                @Override public void onError(String e) {
                                    runOnUiThread(() -> {
                                        btnDel.setEnabled(true);
                                        Toast.makeText(GimnasioActivity.this, "Error: " + e, Toast.LENGTH_SHORT).show();
                                    });
                                }
                            });
                });
                fila.addView(btnDel);
                root.addView(fila);
            }
        }

        // Separador
        View sep = new View(this);
        sep.setBackgroundColor(BORDER);
        LinearLayout.LayoutParams sp2 = new LinearLayout.LayoutParams(-1, dp(1));
        sp2.topMargin = dp(16); sp2.bottomMargin = dp(16);
        sep.setLayoutParams(sp2);
        root.addView(sep);
        root.addView(mkSheetLabel("AÑADIR NUEVA FRANJA"));

        LinearLayout horaRow = new LinearLayout(this);
        horaRow.setOrientation(LinearLayout.HORIZONTAL);
        horaRow.setGravity(Gravity.CENTER_VERTICAL);
        setMB(horaRow, 12);
        root.addView(horaRow);

        final String[] horaVal = {"09:00"};
        TextView tvHField = new TextView(this);
        tvHField.setText("09:00");
        tvHField.setTextSize(22f);
        tvHField.setTextColor(BLUE);
        tvHField.setTypeface(Typeface.DEFAULT_BOLD);
        tvHField.setGravity(Gravity.CENTER);
        tvHField.setPadding(dp(20), dp(12), dp(20), dp(12));
        GradientDrawable hfBg = new GradientDrawable();
        hfBg.setColor(BLUE_XL); hfBg.setCornerRadius(dp(16)); hfBg.setStroke(dp(2), BLUE_XX);
        tvHField.setBackground(hfBg);
        tvHField.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
        tvHField.setOnClickListener(v -> showTimePicker(tvHField, horaVal));
        horaRow.addView(tvHField);

        TextView tvAfLabel = new TextView(this);
        tvAfLabel.setText("Aforo");
        tvAfLabel.setTextSize(12f);
        tvAfLabel.setTextColor(TEXT_M);
        LinearLayout.LayoutParams alp = new LinearLayout.LayoutParams(-2, -2);
        alp.setMarginStart(dp(16)); alp.setMarginEnd(dp(8));
        tvAfLabel.setLayoutParams(alp);
        horaRow.addView(tvAfLabel);

        final int[] aforoNuevo = {15};
        LinearLayout aforoSel = new LinearLayout(this);
        aforoSel.setOrientation(LinearLayout.HORIZONTAL);
        aforoSel.setGravity(Gravity.CENTER_VERTICAL);
        TextView btnAM = mkBtn("–", BLUE_XL, BLUE, false);
        final TextView tvAforoNum = new TextView(this);
        tvAforoNum.setText("15");
        tvAforoNum.setTextSize(18f);
        tvAforoNum.setTextColor(TEXT_D);
        tvAforoNum.setTypeface(Typeface.DEFAULT_BOLD);
        tvAforoNum.setGravity(Gravity.CENTER);
        tvAforoNum.setMinWidth(dp(40));
        TextView btnAP = mkBtn("+", BLUE_XL, BLUE, false);
        btnAM.setOnClickListener(v -> { if (aforoNuevo[0] > 1) { aforoNuevo[0]--; tvAforoNum.setText(String.valueOf(aforoNuevo[0])); } });
        btnAP.setOnClickListener(v -> { if (aforoNuevo[0] < 100) { aforoNuevo[0]++; tvAforoNum.setText(String.valueOf(aforoNuevo[0])); } });
        aforoSel.addView(btnAM); aforoSel.addView(tvAforoNum); aforoSel.addView(btnAP);
        horaRow.addView(aforoSel);

        TextView btnCrear = mkFullBtn("CREAR FRANJA", BLUE);
        LinearLayout.LayoutParams bcp2 = new LinearLayout.LayoutParams(-1, -2);
        bcp2.topMargin = dp(6);
        btnCrear.setLayoutParams(bcp2);
        btnCrear.setOnClickListener(v -> {
            String hora = horaVal[0];
            for (FranjaLocal ex : getFranjasDia(dia)) {
                if (ex.hora.equals(hora)) {
                    Toast.makeText(this, "Ya existe una franja a las " + hora, Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            btnCrear.setEnabled(false);
            btnCrear.setText("Creando...");
            int diaSemBD = diaSemanaIdx(dia) + 1;
            // hora_fin = hora + 1h
            String horaFin = String.format("%02d:00", (Integer.parseInt(hora.split(":")[0]) + 1) % 24);

            SupabaseRepository.get().crearFranja(diaSemBD, hora, horaFin, aforoNuevo[0],
                    new SupabaseRepository.Callback<FranjaModel>() {
                        @Override public void onSuccess(FranjaModel data) {
                            runOnUiThread(() -> {
                                FranjaLocal nueva = new FranjaLocal(data);
                                getFranjasDia(dia).add(nueva);
                                getFranjasDia(dia).sort((a2, b2) -> a2.hora.compareTo(b2.hora));
                                sheet.dismiss();
                                renderContenido(); renderDias();
                                Toast.makeText(GimnasioActivity.this,
                                        "✓ Franja " + hora + " creada", Toast.LENGTH_SHORT).show();
                            });
                        }
                        @Override public void onError(String e) {
                            runOnUiThread(() -> {
                                btnCrear.setEnabled(true);
                                btnCrear.setText("CREAR FRANJA");
                                Toast.makeText(GimnasioActivity.this, "Error: " + e, Toast.LENGTH_SHORT).show();
                            });
                        }
                    });
        });
        root.addView(btnCrear);

        sheet.setContentView(sv);
        sheet.show();
    }

    // ════════════════════════════════════════════════════════════════
    //  SHEET: EDITAR FRANJA
    // ════════════════════════════════════════════════════════════════
    private void showEditarFranjaSheet(FranjaLocal fl, String dia) {
        BottomSheetDialog sheet = mkSheet();
        LinearLayout root = mkSheetRoot();
        root.addView(mkHandle());
        root.addView(mkSheetTitle("Editar franja · " + fl.hora));
        root.addView(mkSheetLabel("AFORO MÁXIMO"));

        final int[] aforoEdit = {fl.aforoMax};
        LinearLayout aforoRow = new LinearLayout(this);
        aforoRow.setOrientation(LinearLayout.HORIZONTAL);
        aforoRow.setGravity(Gravity.CENTER_VERTICAL);
        setMB(aforoRow, 20);
        root.addView(aforoRow);

        TextView btnAM2 = mkBtn("–", BLUE_XL, BLUE, false);
        LinearLayout.LayoutParams bp2 = new LinearLayout.LayoutParams(dp(50), dp(50));
        bp2.setMarginEnd(dp(16));
        btnAM2.setLayoutParams(bp2);
        btnAM2.setGravity(Gravity.CENTER);
        aforoRow.addView(btnAM2);

        final TextView tvAforoGrande = new TextView(this);
        tvAforoGrande.setText(String.valueOf(fl.aforoMax));
        tvAforoGrande.setTextSize(48f);
        tvAforoGrande.setTextColor(BLUE);
        tvAforoGrande.setTypeface(Typeface.DEFAULT_BOLD);
        tvAforoGrande.setGravity(Gravity.CENTER);
        tvAforoGrande.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
        aforoRow.addView(tvAforoGrande);

        TextView btnAP2 = mkBtn("+", BLUE_XL, BLUE, false);
        LinearLayout.LayoutParams bp3 = new LinearLayout.LayoutParams(dp(50), dp(50));
        bp3.setMarginStart(dp(16));
        btnAP2.setLayoutParams(bp3);
        btnAP2.setGravity(Gravity.CENTER);
        aforoRow.addView(btnAP2);

        btnAM2.setOnClickListener(v -> { if (aforoEdit[0] > 1) { aforoEdit[0]--; tvAforoGrande.setText(String.valueOf(aforoEdit[0])); } });
        btnAP2.setOnClickListener(v -> { if (aforoEdit[0] < 100) { aforoEdit[0]++; tvAforoGrande.setText(String.valueOf(aforoEdit[0])); } });

        TextView tvInfo = new TextView(this);
        tvInfo.setText("Actualmente: " + fl.ocupacion() + " personas apuntadas");
        tvInfo.setTextSize(11f);
        tvInfo.setTextColor(TEXT_L);
        tvInfo.setGravity(Gravity.CENTER);
        setMB(tvInfo, 20);
        root.addView(tvInfo);

        TextView btnOk = mkFullBtn("GUARDAR CAMBIOS", BLUE);
        setMB(btnOk, 10);
        root.addView(btnOk);
        btnOk.setOnClickListener(v -> {
            if (fl.id == null) {
                fl.aforoMax = aforoEdit[0];
                sheet.dismiss(); renderContenido(); renderDias();
                return;
            }
            btnOk.setEnabled(false); btnOk.setText("Guardando...");
            Map<String, Object> campos = new HashMap<>();
            campos.put("aforo_max", aforoEdit[0]);
            SupabaseRepository.get().actualizarFranja(fl.id, campos,
                    new SupabaseRepository.Callback<Void>() {
                        @Override public void onSuccess(Void data) {
                            runOnUiThread(() -> {
                                fl.aforoMax = aforoEdit[0];
                                sheet.dismiss();
                                renderContenido(); renderDias();
                                Toast.makeText(GimnasioActivity.this,
                                        "✓ Franja actualizada — aforo: " + aforoEdit[0],
                                        Toast.LENGTH_SHORT).show();
                            });
                        }
                        @Override public void onError(String e) {
                            runOnUiThread(() -> {
                                btnOk.setEnabled(true); btnOk.setText("GUARDAR CAMBIOS");
                                Toast.makeText(GimnasioActivity.this, "Error: " + e, Toast.LENGTH_SHORT).show();
                            });
                        }
                    });
        });

        TextView btnDel = mkFullBtn("ELIMINAR FRANJA", RED);
        GradientDrawable delBg = new GradientDrawable();
        delBg.setColor(0xFFFEF2F2); delBg.setCornerRadius(dp(16));
        btnDel.setBackground(delBg);
        btnDel.setTextColor(RED);
        root.addView(btnDel);
        btnDel.setOnClickListener(v -> {
            if (fl.id == null) {
                getFranjasDia(dia).remove(fl);
                sheet.dismiss(); renderContenido(); renderDias();
                return;
            }
            btnDel.setEnabled(false);
            SupabaseRepository.get().eliminarFranja(fl.id,
                    new SupabaseRepository.Callback<Void>() {
                        @Override public void onSuccess(Void data) {
                            runOnUiThread(() -> {
                                getFranjasDia(dia).remove(fl);
                                sheet.dismiss(); renderContenido(); renderDias();
                                Toast.makeText(GimnasioActivity.this, "Franja eliminada", Toast.LENGTH_SHORT).show();
                            });
                        }
                        @Override public void onError(String e) {
                            runOnUiThread(() -> {
                                btnDel.setEnabled(true);
                                Toast.makeText(GimnasioActivity.this, "Error: " + e, Toast.LENGTH_SHORT).show();
                            });
                        }
                    });
        });

        sheet.setContentView(root);
        sheet.show();
    }

    // ════════════════════════════════════════════════════════════════
    //  TIME PICKER
    // ════════════════════════════════════════════════════════════════
    private void showTimePicker(TextView display, String[] target) {

        int iH = 9, iM = 0;
        try {
            String[] p = target[0].split(":");
            iH = Integer.parseInt(p[0]);
            iM = Integer.parseInt(p[1]);
        } catch (Exception ignored) {}

        final int[] sH = {iH}, sM = {iM};

        BottomSheetDialog sheet = mkSheet();
        LinearLayout root = mkSheetRoot();

        // 🔥 IMPORTANTE: contenido arriba
        root.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(16), dp(8), dp(16), dp(16));

        root.addView(mkHandle());

        TextView tvTit = mkSheetTitle("Seleccionar hora");
        tvTit.setGravity(Gravity.CENTER);
        setMB(tvTit, 12);
        root.addView(tvTit);

        TextView tvDisp = new TextView(this);
        tvDisp.setText(String.format("%02d:%02d", sH[0], sM[0]));
        tvDisp.setTextSize(56f);
        tvDisp.setTextColor(BLUE);
        tvDisp.setTypeface(Typeface.DEFAULT_BOLD);
        tvDisp.setGravity(Gravity.CENTER);
        setMB(tvDisp, 16);
        root.addView(tvDisp);

        LinearLayout ctrlRow = new LinearLayout(this);
        ctrlRow.setOrientation(LinearLayout.HORIZONTAL);
        ctrlRow.setGravity(Gravity.CENTER);
        setMB(ctrlRow, 12);
        root.addView(ctrlRow);

        // ── HORAS ─────────────────────────────────────────────
        LinearLayout hBlk = mkTimeBlock("HORAS");

        TextView bHP = mkTimBtn("＋");
        TextView tvHV = mkTimVal(String.format("%02d", sH[0]));
        TextView bHM = mkTimBtn("－");

        bHP.setOnClickListener(v -> {
            sH[0] = (sH[0] + 1) % 24;
            tvHV.setText(String.format("%02d", sH[0]));
            tvDisp.setText(String.format("%02d:%02d", sH[0], sM[0]));
        });

        bHM.setOnClickListener(v -> {
            sH[0] = (sH[0] + 23) % 24;
            tvHV.setText(String.format("%02d", sH[0]));
            tvDisp.setText(String.format("%02d:%02d", sH[0], sM[0]));
        });

        hBlk.addView(bHP);
        hBlk.addView(tvHV);
        hBlk.addView(bHM);

        ctrlRow.addView(hBlk);

        // ── SEPARADOR ─────────────────────────────────────────
        TextView sep = new TextView(this);
        sep.setText(":");
        sep.setTextSize(34f);
        sep.setTextColor(BLUE);
        sep.setTypeface(Typeface.DEFAULT_BOLD);
        sep.setGravity(Gravity.CENTER);
        sep.setPadding(dp(8), dp(16), dp(8), dp(0));
        ctrlRow.addView(sep);

        // ── MINUTOS ───────────────────────────────────────────
        LinearLayout mBlk = mkTimeBlock("MINUTOS");

        TextView bMP = mkTimBtn("＋");
        TextView tvMV = mkTimVal(String.format("%02d", sM[0]));
        TextView bMM = mkTimBtn("－");

        bMP.setOnClickListener(v -> {
            sM[0] = (sM[0] + 5) % 60;
            tvMV.setText(String.format("%02d", sM[0]));
            tvDisp.setText(String.format("%02d:%02d", sH[0], sM[0]));
        });

        bMM.setOnClickListener(v -> {
            sM[0] = (sM[0] + 55) % 60;
            tvMV.setText(String.format("%02d", sM[0]));
            tvDisp.setText(String.format("%02d:%02d", sH[0], sM[0]));
        });

        mBlk.addView(bMP);
        mBlk.addView(tvMV);
        mBlk.addView(bMM);

        ctrlRow.addView(mBlk);

        // ── BOTÓN CONFIRMAR (SIEMPRE VISIBLE) ─────────────────
        TextView btnOk = mkFullBtn("CONFIRMAR", BLUE);

        setMB(btnOk, 8);

        btnOk.setOnClickListener(v -> {
            target[0] = String.format("%02d:%02d", sH[0], sM[0]);
            display.setText(target[0]);
            sheet.dismiss();
        });

        root.addView(btnOk);

        sheet.setContentView(root);
        sheet.show();
    }

    // ════════════════════════════════════════════════════════════════
    //  FAB + NAV
    // ════════════════════════════════════════════════════════════════
    private View buildFabAnadir() {
        TextView fab = new TextView(this);
        fab.setText("＋");
        fab.setTextSize(26f);
        fab.setTextColor(WHITE);
        fab.setTypeface(Typeface.DEFAULT_BOLD);
        fab.setGravity(Gravity.CENTER);
        GradientDrawable fbg = new GradientDrawable();
        fbg.setShape(GradientDrawable.OVAL);
        fbg.setColor(BLUE);
        fab.setBackground(fbg);
        fab.setElevation(dp(8));
        FrameLayout.LayoutParams fp = new FrameLayout.LayoutParams(dp(58), dp(58));
        fp.gravity = Gravity.BOTTOM | Gravity.END;
        fp.setMargins(0, 0, dp(20), dp(92));
        fab.setLayoutParams(fp);
        fab.setOnClickListener(v -> showGestionarFranjasSheet(selectedFecha));
        return fab;
    }

    private View buildNavBar() {
        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setBackgroundColor(WHITE);
        nav.setElevation(dp(12));
        FrameLayout.LayoutParams np = new FrameLayout.LayoutParams(-1, dp(74));
        np.gravity = Gravity.BOTTOM;
        nav.setLayoutParams(np);
        nav.setPadding(dp(4), dp(6), dp(4), dp(8));

        nav.addView(mkNavTab3("🏠","Inicio",false, v -> {
            startActivity(new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
            overridePendingTransition(0, 0);
        }));
        nav.addView(mkNavTab3("👥","Clientes",false, v -> {
            startActivity(new Intent(this, ClientesActivity.class));
            overridePendingTransition(0, 0); finish();
        }));
        nav.addView(mkNavTab3("💪","Gimnasio",true, v -> { /* ya estamos aquí */ }));
        nav.addView(mkNavTab3("📅","Agenda",false, v -> {
            startActivity(new Intent(this, AgendaActivity.class));
            overridePendingTransition(0, 0); finish();
        }));
        nav.addView(mkNavTab3("💰","Cobros",false, v -> {
            startActivity(new Intent(this, CobrosActivity.class));
            overridePendingTransition(0, 0); finish();
        }));
        return nav;
    }

    private LinearLayout mkNavTab3(String ico, String lbl, boolean activo, View.OnClickListener l) {
        LinearLayout tab = new LinearLayout(this);
        tab.setOrientation(LinearLayout.VERTICAL);
        tab.setGravity(Gravity.CENTER);
        tab.setLayoutParams(new LinearLayout.LayoutParams(0, -1, 1f));
        if (activo) {
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(Color.parseColor("#0A66FF"));
            bg.setCornerRadius(dp(999));
            tab.setBackground(bg);
        } else {
            tab.setBackgroundColor(Color.TRANSPARENT);
        }
        TextView tvI = new TextView(this);
        tvI.setText(ico);
        tvI.setTextSize(17f);
        tvI.setGravity(Gravity.CENTER);
        tab.addView(tvI);
        TextView tvL = new TextView(this);
        tvL.setText(lbl);
        tvL.setTextSize(9f);
        tvL.setTextColor(activo ? Color.WHITE : Color.parseColor("#6B7FA3"));
        tvL.setTypeface(activo ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        tvL.setGravity(Gravity.CENTER);
        tab.addView(tvL);
        tab.setOnClickListener(l);
        return tab;
    }

    // ════════════════════════════════════════════════════════════════
    //  HELPERS UI
    // ════════════════════════════════════════════════════════════════
    private BottomSheetDialog mkSheet() {
        return new BottomSheetDialog(this, com.google.android.material.R.style.Theme_Material3_Light_BottomSheetDialog);
    }
    private LinearLayout mkSheetRoot() {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.VERTICAL);
        r.setBackgroundColor(WHITE);
        r.setPadding(dp(20), dp(16), dp(20), dp(36));
        return r;
    }
    private View mkHandle() {
        LinearLayout hw = new LinearLayout(this);
        hw.setGravity(Gravity.CENTER_HORIZONTAL);
        setMB(hw, 18);
        View h = new View(this);
        GradientDrawable hbg = new GradientDrawable();
        hbg.setColor(BORDER); hbg.setCornerRadius(dp(3));
        h.setBackground(hbg);
        h.setLayoutParams(new LinearLayout.LayoutParams(dp(40), dp(4)));
        hw.addView(h);
        return hw;
    }
    private TextView mkSheetTitle(String txt) {
        TextView tv = new TextView(this);
        tv.setText(txt); tv.setTextSize(18f); tv.setTextColor(TEXT_D);
        tv.setTypeface(Typeface.DEFAULT_BOLD); setMB(tv, 16);
        return tv;
    }
    private TextView mkSheetLabel(String txt) {
        TextView tv = new TextView(this);
        tv.setText(txt); tv.setTextSize(9f); tv.setTextColor(TEXT_L);
        tv.setTypeface(Typeface.DEFAULT_BOLD); tv.setLetterSpacing(0.15f); setMB(tv, 8);
        return tv;
    }
    private TextView mkSecLabel(String txt, int color) {
        TextView tv = new TextView(this);
        tv.setText(txt); tv.setTextSize(10f); tv.setTextColor(TEXT_L);
        tv.setTypeface(Typeface.DEFAULT_BOLD); tv.setLetterSpacing(0.12f);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.bottomMargin = dp(10); tv.setLayoutParams(p);
        return tv;
    }
    private TextView mkFullBtn(String txt, int bgColor) {
        TextView tv = new TextView(this);
        tv.setText(txt); tv.setTextSize(13f); tv.setTextColor(WHITE);
        tv.setTypeface(Typeface.DEFAULT_BOLD); tv.setLetterSpacing(0.05f);
        tv.setGravity(Gravity.CENTER); tv.setPadding(0, dp(15), 0, dp(15));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(bgColor); bg.setCornerRadius(dp(16));
        tv.setBackground(bg);
        tv.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
        return tv;
    }
    private TextView mkBtn(String txt, int bgColor, int textColor, boolean bold) {
        TextView tv = new TextView(this);
        tv.setText(txt); tv.setTextSize(11f); tv.setTextColor(textColor);
        tv.setTypeface(bold ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        tv.setGravity(Gravity.CENTER); tv.setPadding(dp(14), dp(8), dp(14), dp(8));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(bgColor); bg.setCornerRadius(dp(20));
        tv.setBackground(bg);
        tv.setLayoutParams(new LinearLayout.LayoutParams(-2, -2));
        return tv;
    }
    private TextView mkPillBtn(String txt, int bgColor, int textColor) {
        TextView tv = new TextView(this);
        tv.setText(txt); tv.setTextSize(11f); tv.setTextColor(textColor);
        tv.setTypeface(Typeface.DEFAULT_BOLD); tv.setGravity(Gravity.CENTER);
        tv.setPadding(dp(14), dp(8), dp(14), dp(8));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(bgColor); bg.setCornerRadius(dp(20));
        tv.setBackground(bg);
        tv.setLayoutParams(new LinearLayout.LayoutParams(-2, -2));
        return tv;
    }
    private FrameLayout mkAvatar(String nombre, int color, int sizeDp) {
        FrameLayout av = new FrameLayout(this);
        GradientDrawable avBg = new GradientDrawable();
        avBg.setShape(GradientDrawable.OVAL); avBg.setColor(color);
        av.setBackground(avBg);
        av.setLayoutParams(new LinearLayout.LayoutParams(dp(sizeDp), dp(sizeDp)));
        TextView tv = new TextView(this);
        tv.setText(nombre != null && !nombre.isEmpty() ? String.valueOf(nombre.charAt(0)).toUpperCase() : "?");
        tv.setTextSize(sizeDp * 0.38f); tv.setTextColor(WHITE);
        tv.setTypeface(Typeface.DEFAULT_BOLD); tv.setGravity(Gravity.CENTER);
        tv.setLayoutParams(new FrameLayout.LayoutParams(-1, -1));
        av.addView(tv);
        return av;
    }
    private EditText mkEditText(String hint) {
        EditText et = new EditText(this);
        et.setHint(hint); et.setTextSize(15f); et.setTextColor(TEXT_D); et.setHintTextColor(TEXT_L);
        et.setPadding(dp(18), dp(14), dp(18), dp(14));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(BLUE_XL); bg.setCornerRadius(dp(16)); bg.setStroke(dp(1), BORDER);
        et.setBackground(bg);
        et.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
        return et;
    }
    private CardView mkCard(int radiusDp) {
        CardView c = new CardView(this);
        c.setRadius(dp(radiusDp)); c.setCardElevation(dp(2)); c.setCardBackgroundColor(WHITE);
        return c;
    }
    private View buildEmpty(String titulo, String sub) {
        LinearLayout v = new LinearLayout(this);
        v.setOrientation(LinearLayout.VERTICAL); v.setGravity(Gravity.CENTER);
        v.setPadding(dp(32), dp(56), dp(32), dp(56));
        TextView tvI = new TextView(this); tvI.setText("🏋️"); tvI.setTextSize(48f); tvI.setGravity(Gravity.CENTER);
        v.addView(tvI);
        TextView tvT = new TextView(this); tvT.setText(titulo); tvT.setTextSize(16f);
        tvT.setTextColor(TEXT_D); tvT.setTypeface(Typeface.DEFAULT_BOLD); tvT.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(-1, -2);
        tp.topMargin = dp(12); tp.bottomMargin = dp(8); tvT.setLayoutParams(tp);
        v.addView(tvT);
        TextView tvS = new TextView(this); tvS.setText(sub); tvS.setTextSize(13f);
        tvS.setTextColor(TEXT_L); tvS.setGravity(Gravity.CENTER);
        v.addView(tvS);
        return v;
    }
    private View buildOrSep() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL); row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-1, -2);
        rp.topMargin = dp(4); row.setLayoutParams(rp);
        View l1 = new View(this); l1.setBackgroundColor(BORDER);
        l1.setLayoutParams(new LinearLayout.LayoutParams(0, dp(1), 1f)); row.addView(l1);
        TextView tvO = new TextView(this); tvO.setText("  o  "); tvO.setTextSize(11f); tvO.setTextColor(TEXT_L); row.addView(tvO);
        View l2 = new View(this); l2.setBackgroundColor(BORDER);
        l2.setLayoutParams(new LinearLayout.LayoutParams(0, dp(1), 1f)); row.addView(l2);
        return row;
    }

    // ── Time picker helpers ───────────────────────────────────────────
    private LinearLayout mkTimeBlock(String label) {
        LinearLayout b = new LinearLayout(this);
        b.setOrientation(LinearLayout.VERTICAL); b.setGravity(Gravity.CENTER);
        b.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
        TextView lbl = new TextView(this);
        lbl.setText(label); lbl.setTextSize(9f); lbl.setTextColor(TEXT_L);
        lbl.setTypeface(Typeface.DEFAULT_BOLD); lbl.setLetterSpacing(0.1f); lbl.setGravity(Gravity.CENTER);
        setMB(lbl, 10); b.addView(lbl);
        return b;
    }
    private TextView mkTimBtn(String s) {
        TextView tv = new TextView(this);
        tv.setText(s); tv.setTextSize(22f); tv.setTextColor(BLUE);
        tv.setTypeface(Typeface.DEFAULT_BOLD); tv.setGravity(Gravity.CENTER);
        GradientDrawable bg = new GradientDrawable(); bg.setColor(BLUE_XL); bg.setCornerRadius(dp(14));
        tv.setBackground(bg);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(dp(68), dp(52));
        p.gravity = Gravity.CENTER_HORIZONTAL; p.bottomMargin = dp(8); tv.setLayoutParams(p);
        return tv;
    }
    private TextView mkTimVal(String val) {
        TextView tv = new TextView(this);
        tv.setText(val); tv.setTextSize(40f); tv.setTextColor(TEXT_D);
        tv.setTypeface(Typeface.DEFAULT_BOLD); tv.setGravity(Gravity.CENTER);
        GradientDrawable bg = new GradientDrawable(); bg.setColor(BLUE_XL); bg.setCornerRadius(dp(18));
        tv.setBackground(bg);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(dp(90), dp(80));
        p.gravity = Gravity.CENTER_HORIZONTAL; p.bottomMargin = dp(8); tv.setLayoutParams(p);
        return tv;
    }

    // ── Fecha helpers ─────────────────────────────────────────────────
    static String hoy() {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        return String.format(java.util.Locale.getDefault(), "%04d-%02d-%02d",
                cal.get(java.util.Calendar.YEAR),
                cal.get(java.util.Calendar.MONTH) + 1,
                cal.get(java.util.Calendar.DAY_OF_MONTH));
    }
    static String lunesDeSemana(int offsetSemanas) {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        int dow = cal.get(java.util.Calendar.DAY_OF_WEEK);
        int diasHastaLunes = (dow == java.util.Calendar.SUNDAY) ? -6 : -(dow - java.util.Calendar.MONDAY);
        cal.add(java.util.Calendar.DAY_OF_YEAR, diasHastaLunes + offsetSemanas * 7);
        return String.format(java.util.Locale.getDefault(), "%04d-%02d-%02d",
                cal.get(java.util.Calendar.YEAR),
                cal.get(java.util.Calendar.MONTH) + 1,
                cal.get(java.util.Calendar.DAY_OF_MONTH));
    }
    static String sumarDias(String fecha, int dias) {
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.setTime(sdf.parse(fecha));
            cal.add(java.util.Calendar.DAY_OF_YEAR, dias);
            return sdf.format(cal.getTime());
        } catch (Exception e) { return fecha; }
    }
    static int diaSemanaIdx(String fecha) {
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.setTime(sdf.parse(fecha));
            int dow = cal.get(java.util.Calendar.DAY_OF_WEEK);
            return (dow == java.util.Calendar.SUNDAY) ? 6 : dow - java.util.Calendar.MONDAY;
        } catch (Exception e) { return 0; }
    }
    private static String lunesBase(String fecha) {
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.setTime(sdf.parse(fecha));
            int dow = cal.get(java.util.Calendar.DAY_OF_WEEK);
            int diff = (dow == java.util.Calendar.SUNDAY) ? -6 : -(dow - java.util.Calendar.MONDAY);
            cal.add(java.util.Calendar.DAY_OF_YEAR, diff);
            return sdf.format(cal.getTime());
        } catch (Exception e) { return fecha; }
    }
    private String fmtLargo(String fecha) {
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.setTime(sdf.parse(fecha));
            int dw = cal.get(java.util.Calendar.DAY_OF_WEEK);
            int di = (dw == java.util.Calendar.SUNDAY) ? 6 : dw - java.util.Calendar.MONDAY;
            int dia = cal.get(java.util.Calendar.DAY_OF_MONTH);
            int mes = cal.get(java.util.Calendar.MONTH);
            int anio = cal.get(java.util.Calendar.YEAR);
            String[] mc = {"Ene","Feb","Mar","Abr","May","Jun","Jul","Ago","Sep","Oct","Nov","Dic"};
            return DIAS_CORTO[di] + ", " + dia + " " + mc[mes] + " " + anio;
        } catch (Exception e) { return fecha; }
    }
    private String fmtCorto(String fecha) {
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.setTime(sdf.parse(fecha));
            String[] mc = {"Ene","Feb","Mar","Abr","May","Jun","Jul","Ago","Sep","Oct","Nov","Dic"};
            return cal.get(java.util.Calendar.DAY_OF_MONTH) + " " + mc[cal.get(java.util.Calendar.MONTH)];
        } catch (Exception e) { return fecha; }
    }

    // ── Layout helpers ────────────────────────────────────────────────
    private void setMB(View v, int dpVal) {
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) v.getLayoutParams();
        if (lp == null) lp = new LinearLayout.LayoutParams(-2, -2);
        lp.bottomMargin = dp(dpVal); v.setLayoutParams(lp);
    }
    private void setME(View v, int dpVal) {
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) v.getLayoutParams();
        if (lp == null) lp = new LinearLayout.LayoutParams(-2, -2);
        lp.setMarginEnd(dp(dpVal)); v.setLayoutParams(lp);
    }
    private int dp(int val) {
        return Math.round(val * getResources().getDisplayMetrics().density);
    }
}