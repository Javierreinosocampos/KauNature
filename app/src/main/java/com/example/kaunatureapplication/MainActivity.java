package com.example.kaunatureapplication;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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
        String tipo, titulo, desc, hora;
        boolean leida;
        Notif(String t, String ti, String d, String h) {
            tipo = t; titulo = ti; desc = d; hora = h;
        }
    }
    private final List<Notif> notificaciones = new ArrayList<>();

    // ════════════════════════════════════════════════════════════════
    //  Lifecycle
    // ════════════════════════════════════════════════════════════════
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_main);
        bind();
        setupGreeting();
        setupFecha();
        NavHelper.setup(this, "home");
        bindButtons();

        // KPIs y listas: mostrar placeholders y cargar de Supabase
        mostrarKpisVacios();
        cargarDatos();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Al volver a la pantalla, refrescar datos
        cargarDatos();
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
        tvKpiIngresos.setText(String.format("%.0f€", totalDeuda));
    }

    // ════════════════════════════════════════════════════════════════
    //  LISTAS
    // ════════════════════════════════════════════════════════════════
    private void buildListaCitas() {
        if (listaCitas == null) return;

        // 🔥 FORZAR SEPARACIÓN REAL (NO DEPENDE DE LAYOUTS)
        listaCitas.setPadding(
                listaCitas.getPaddingLeft(),
                dp(20), // 👈 margen superior REAL
                listaCitas.getPaddingRight(),
                listaCitas.getPaddingBottom()
        );

        listaCitas.removeAllViews();

        if (citasHoy.isEmpty()) {
            listaCitas.addView(rowVacio("Sin citas para hoy"));
            return;
        }

        int limit = Math.min(citasHoy.size(), 3);

        for (int i = 0; i < limit; i++) {

            CitaModel c = citasHoy.get(i);

            String titulo = c.horaDisplay() + "  " +
                    (c.clienteNombre != null ? c.clienteNombre : "");

            String sub = (c.servicioNombre != null ? c.servicioNombre : "")
                    + " · " + c.precioDisplay();

            View row = rowItem(titulo, sub, c.precioDisplay(), GREEN);

            row.setOnClickListener(v ->
                    startActivity(new Intent(this, AgendaActivity.class))
            );

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
        // Quitar notifs de tipo "cita" anteriores
        notificaciones.removeIf(n -> "cita".equals(n.tipo));
        for (CitaModel c : citasHoy) {
            String titulo = "Cita · " +
                    (c.clienteNombre != null ? c.clienteNombre : "");

            String desc = (c.servicioNombre != null ? c.servicioNombre : "")
                    + " a las " + c.horaDisplay() + "h";
            notificaciones.add(new Notif("cita", titulo, desc, c.horaDisplay()));
        }
    }

    private void generarNotifsDeudas() {
        notificaciones.removeIf(n -> "pago".equals(n.tipo) || "deuda".equals(n.tipo));
        for (CobroModel c : cobrosPendientes) {
            String cliente = c.clienteNombre != null ? c.clienteNombre : "";
            String titulo  = "Pago pendiente · " + cliente;
            String desc    = (c.concepto != null ? c.concepto : "") + " · " + c.importeFormateado();
            notificaciones.add(new Notif("pago", titulo, desc, "Hoy"));
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
        View view = getLayoutInflater().inflate(R.layout.sheet_notificaciones, null);
        sheet.setContentView(view);

        LinearLayout lista = view.findViewById(R.id.listaNotificaciones);
        LinearLayout vacio = view.findViewById(R.id.layoutVacio);
        TextView tvSub     = view.findViewById(R.id.tvNotifSubtitle);
        TextView btnM      = view.findViewById(R.id.btnMarcarLeido);

        Runnable refresh = () -> {
            long nl = notificaciones.stream().filter(n -> !n.leida).count();
            tvSub.setText(nl > 0 ? nl + " alerta" + (nl > 1 ? "s" : "") + " sin leer" : "Todo al día");
            lista.removeAllViews();
            for (Notif n : notificaciones) lista.addView(notifRow(n, tvSub));
        };

        if (notificaciones.isEmpty()) {
            lista.setVisibility(View.GONE);
            vacio.setVisibility(View.VISIBLE);
            tvSub.setText("No hay notificaciones");
        } else {
            refresh.run();
        }

        btnM.setOnClickListener(v -> {
            for (Notif n : notificaciones) n.leida = true;
            refreshBadge();
            refresh.run();
        });
        sheet.show();
    }

    private View notifRow(Notif n, TextView tvSub) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), dp(14), dp(14), dp(14));
        GradientDrawable rBg = new GradientDrawable();
        rBg.setColor(n.leida ? 0xFFF9FAFB : BLUE_XL);
        rBg.setCornerRadius(dp(16));
        row.setBackground(rBg);
        LinearLayout.LayoutParams rP = new LinearLayout.LayoutParams(-1, -2);
        rP.bottomMargin = dp(8);
        row.setLayoutParams(rP);

        LinearLayout ic = new LinearLayout(this);
        ic.setGravity(Gravity.CENTER);
        GradientDrawable iBg = new GradientDrawable();
        iBg.setShape(GradientDrawable.OVAL);
        iBg.setColor("cita".equals(n.tipo) ? 0xFFE8F0FF : "pago".equals(n.tipo) ? 0xFFFFF8E8 : 0xFFFFE8E8);
        ic.setBackground(iBg);
        LinearLayout.LayoutParams iP = new LinearLayout.LayoutParams(dp(40), dp(40));
        iP.setMarginEnd(dp(12));
        ic.setLayoutParams(iP);
        TextView tvI = new TextView(this);
        tvI.setText("cita".equals(n.tipo) ? "📅" : "pago".equals(n.tipo) ? "💰" : "⚠️");
        tvI.setTextSize(17f);
        tvI.setGravity(Gravity.CENTER);
        tvI.setLayoutParams(new LinearLayout.LayoutParams(-1, -1));
        ic.addView(tvI);
        row.addView(ic);

        LinearLayout txt = new LinearLayout(this);
        txt.setOrientation(LinearLayout.VERTICAL);
        txt.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
        TextView tvT = new TextView(this);
        tvT.setText(n.titulo);
        tvT.setTextSize(12.5f);
        tvT.setTextColor(TEXT_D);
        tvT.setTypeface(Typeface.DEFAULT_BOLD);
        tvT.setAlpha(n.leida ? 0.45f : 1f);
        tvT.setSingleLine(true);
        tvT.setEllipsize(android.text.TextUtils.TruncateAt.END);
        txt.addView(tvT);
        TextView tvD2 = new TextView(this);
        tvD2.setText(n.desc);
        tvD2.setTextSize(11f);
        tvD2.setTextColor(TEXT_L);
        tvD2.setAlpha(n.leida ? 0.45f : 1f);
        tvD2.setSingleLine(true);
        tvD2.setEllipsize(android.text.TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams dP = new LinearLayout.LayoutParams(-2, -2);
        dP.topMargin = dp(2);
        tvD2.setLayoutParams(dP);
        txt.addView(tvD2);
        row.addView(txt);

        TextView tvH = new TextView(this);
        tvH.setText(n.hora);
        tvH.setTextSize(10f);
        tvH.setTextColor(TEXT_L);
        LinearLayout.LayoutParams hP = new LinearLayout.LayoutParams(-2, -2);
        hP.setMarginStart(dp(8));
        hP.gravity = Gravity.CENTER_VERTICAL;
        tvH.setLayoutParams(hP);
        row.addView(tvH);

        row.setClickable(true);
        row.setFocusable(true);
        row.setOnClickListener(v -> {
            n.leida = true;
            refreshBadge();
            rBg.setColor(0xFFF9FAFB);
            tvT.setAlpha(0.45f);
            tvD2.setAlpha(0.45f);
            long nl = notificaciones.stream().filter(x -> !x.leida).count();
            tvSub.setText(nl > 0 ? nl + " alerta" + (nl > 1 ? "s" : "") + " sin leer" : "Todo al día");
        });
        return row;
    }

    // ════════════════════════════════════════════════════════════════
    //  UI BUILDERS
    // ════════════════════════════════════════════════════════════════
    private void setupGreeting() {
        int h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        tvGreeting.setText(h < 13 ? "Buenos días ☀️" : h < 20 ? "Buenas tardes 👋" : "Buenas noches 🌙");
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
        tv1.setTextSize(13f);
        tv1.setTextColor(TEXT_D);
        tv1.setTypeface(Typeface.DEFAULT_BOLD);
        tv1.setSingleLine(true);
        tv1.setEllipsize(android.text.TextUtils.TruncateAt.END);
        txt.addView(tv1);
        TextView tv2 = new TextView(this);
        tv2.setText(sub);
        tv2.setTextSize(11f);
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
        chip.setTextSize(12f);
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
        safeClick(R.id.fabQuick,        v -> showQuickSheet());
        safeClick(R.id.btnAccionCobrar, v -> startActivity(new Intent(this, CobrosActivity.class)));
        safeClick(R.id.btnAccionCita,   v -> startActivity(new Intent(this, AgendaActivity.class)));
        safeClick(R.id.btnAccionGym,    v -> startActivity(new Intent(this, GimnasioActivity.class)));
        safeClick(R.id.btnAccionClientes,v -> startActivity(new Intent(this, ClientesActivity.class)));
        safeClick(R.id.tvVerCitas,      v -> startActivity(new Intent(this, AgendaActivity.class)));
        safeClick(R.id.tvVerPagos,      v -> startActivity(new Intent(this, CobrosActivity.class)));
        safeClick(R.id.tvKpiGym,        v -> startActivity(new Intent(this, GimnasioActivity.class)));
    }

    private void safeClick(int id, View.OnClickListener l) {
        View v = findViewById(id);
        if (v != null) v.setOnClickListener(l);
    }

    // ════════════════════════════════════════════════════════════════
    //  SHEET: ACCIONES RÁPIDAS
    // ════════════════════════════════════════════════════════════════
    private void showQuickSheet() {
        BottomSheetDialog sheet = new BottomSheetDialog(this,
                com.google.android.material.R.style.Theme_Material3_Light_BottomSheetDialog);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(WHITE);
        root.setPadding(dp(20), dp(10), dp(20), dp(44));

        // Handle
        LinearLayout hw = new LinearLayout(this);
        hw.setGravity(Gravity.CENTER_HORIZONTAL);
        LinearLayout.LayoutParams hwP = new LinearLayout.LayoutParams(-1, -2);
        hwP.topMargin = dp(8);
        hwP.bottomMargin = dp(24);
        hw.setLayoutParams(hwP);
        View h2 = new View(this);
        GradientDrawable hBg = new GradientDrawable();
        hBg.setColor(BORDER);
        hBg.setCornerRadius(dp(3));
        h2.setBackground(hBg);
        h2.setLayoutParams(new LinearLayout.LayoutParams(dp(40), dp(4)));
        hw.addView(h2);
        root.addView(hw);

        // Título
        TextView tvT = new TextView(this);
        tvT.setText("Acciones rápidas");
        tvT.setTextSize(24f);
        tvT.setTextColor(TEXT_D);
        tvT.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams tP = new LinearLayout.LayoutParams(-1, -2);
        tP.bottomMargin = dp(4);
        tvT.setLayoutParams(tP);
        root.addView(tvT);

        TextView tvS = new TextView(this);
        tvS.setText("¿Qué hacemos ahora?");
        tvS.setTextSize(13f);
        tvS.setTextColor(TEXT_L);
        LinearLayout.LayoutParams sP = new LinearLayout.LayoutParams(-1, -2);
        sP.bottomMargin = dp(28);
        tvS.setLayoutParams(sP);
        root.addView(tvS);

        // Acción principal: Cobrar
        LinearLayout mainCard = buildQACard("💰","Cobrar","Registrar un pago",BLUE,WHITE,0x25FFFFFF,0xBBFFFFFF);
        LinearLayout.LayoutParams mcP = new LinearLayout.LayoutParams(-1, -2);
        mcP.bottomMargin = dp(12);
        mainCard.setLayoutParams(mcP);
        mainCard.setOnClickListener(v -> { sheet.dismiss(); startActivity(new Intent(this, CobrosActivity.class)); });
        root.addView(mainCard);

        // Fila secundaria: 3 acciones
        LinearLayout fila = new LinearLayout(this);
        fila.setOrientation(LinearLayout.HORIZONTAL);

        String[][] sec    = {{"📅","Nueva\ncita"},{"💪","Gimna-\nsio"},{"👥","Clien-\ntes"}};
        Runnable[] secCbs = {
                () -> { sheet.dismiss(); startActivity(new Intent(this, AgendaActivity.class)); },
                () -> { sheet.dismiss(); startActivity(new Intent(this, GimnasioActivity.class)); },
                () -> { sheet.dismiss(); startActivity(new Intent(this, ClientesActivity.class)); },
        };

        for (int i = 0; i < 3; i++) {
            final Runnable cb = secCbs[i];
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setGravity(Gravity.CENTER);
            card.setPadding(dp(12), dp(20), dp(12), dp(20));
            GradientDrawable cBg = new GradientDrawable();
            cBg.setColor(BLUE_XL);
            cBg.setCornerRadius(dp(20));
            card.setBackground(cBg);
            card.setClickable(true);
            card.setFocusable(true);
            card.setOnClickListener(v -> cb.run());
            LinearLayout.LayoutParams cP2 = new LinearLayout.LayoutParams(0, -2, 1f);
            if (i > 0) cP2.setMarginStart(dp(10));
            card.setLayoutParams(cP2);

            // Círculo emoji
            LinearLayout eCircle = new LinearLayout(this);
            eCircle.setGravity(Gravity.CENTER);
            GradientDrawable eBg = new GradientDrawable();
            eBg.setShape(GradientDrawable.OVAL);
            eBg.setColor(adjustAlpha(BLUE, 0.12f));
            eCircle.setBackground(eBg);
            LinearLayout.LayoutParams eP = new LinearLayout.LayoutParams(dp(52), dp(52));
            eP.bottomMargin = dp(10);
            eP.gravity = Gravity.CENTER_HORIZONTAL;
            eCircle.setLayoutParams(eP);
            TextView tvE = new TextView(this);
            tvE.setText(sec[i][0]);
            tvE.setTextSize(24f);
            tvE.setGravity(Gravity.CENTER);
            tvE.setLayoutParams(new LinearLayout.LayoutParams(-1, -1));
            eCircle.addView(tvE);
            card.addView(eCircle);

            TextView tvLbl = new TextView(this);
            tvLbl.setText(sec[i][1]);
            tvLbl.setTextSize(12f);
            tvLbl.setTextColor(TEXT_D);
            tvLbl.setTypeface(Typeface.DEFAULT_BOLD);
            tvLbl.setGravity(Gravity.CENTER);
            card.addView(tvLbl);
            fila.addView(card);
        }
        root.addView(fila);
        sheet.setContentView(root);
        sheet.show();
    }

    private LinearLayout buildQACard(String emoji, String titulo, String desc,
                                     int bg, int textColor, int emojiCircleBg, int descColor) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(20), dp(20), dp(20), dp(20));
        GradientDrawable cBg = new GradientDrawable();
        cBg.setColor(bg);
        cBg.setCornerRadius(dp(22));
        card.setBackground(cBg);
        card.setClickable(true);
        card.setFocusable(true);

        LinearLayout eC = new LinearLayout(this);
        eC.setGravity(Gravity.CENTER);
        GradientDrawable eCBg = new GradientDrawable();
        eCBg.setShape(GradientDrawable.OVAL);
        eCBg.setColor(emojiCircleBg);
        eC.setBackground(eCBg);
        LinearLayout.LayoutParams eCp = new LinearLayout.LayoutParams(dp(56), dp(56));
        eCp.setMarginEnd(dp(18));
        eCp.gravity = Gravity.CENTER_VERTICAL;
        eC.setLayoutParams(eCp);
        TextView tvE = new TextView(this);
        tvE.setText(emoji);
        tvE.setTextSize(26f);
        tvE.setGravity(Gravity.CENTER);
        tvE.setLayoutParams(new LinearLayout.LayoutParams(-1, -1));
        eC.addView(tvE);
        card.addView(eC);

        LinearLayout txt = new LinearLayout(this);
        txt.setOrientation(LinearLayout.VERTICAL);
        txt.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
        TextView tvTi = new TextView(this);
        tvTi.setText(titulo);
        tvTi.setTextSize(18f);
        tvTi.setTextColor(textColor);
        tvTi.setTypeface(Typeface.DEFAULT_BOLD);
        txt.addView(tvTi);
        TextView tvD = new TextView(this);
        tvD.setText(desc);
        tvD.setTextSize(12f);
        tvD.setTextColor(descColor);
        LinearLayout.LayoutParams dP = new LinearLayout.LayoutParams(-2, -2);
        dP.topMargin = dp(2);
        tvD.setLayoutParams(dP);
        txt.addView(tvD);
        card.addView(txt);

        TextView arr = new TextView(this);
        arr.setText("›");
        arr.setTextSize(26f);
        arr.setTextColor(textColor);
        arr.setAlpha(0.6f);
        arr.setGravity(Gravity.CENTER);
        card.addView(arr);
        return card;
    }

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
}