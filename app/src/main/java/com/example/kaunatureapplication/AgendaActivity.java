package com.example.kaunatureapplication;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;


public class AgendaActivity extends AppCompatActivity {

    interface KauCallback { void call(String value); }


    static class Cita {
        String id;
        String clienteId;    // UUID cliente (para vincular cobros)
        String cliente;
        String servicio;
        String fecha;        // dd/MM/yyyy  (display)
        String fechaBD;      // yyyy-MM-dd  (Supabase)
        String hora;         // HH:mm
        String precio;
        String notas;
        String estado;

        int diaSemana;
        int diaMes;
        int mes;
        int anio;

        Cita(CitaModel m) {
            this.id        = m.id;
            this.clienteId = m.clienteId;
            this.cliente   = m.clienteNombre  != null ? m.clienteNombre.trim()  : "";
            this.servicio = m.servicioNombre != null ? m.servicioNombre.trim() : "";
            this.fechaBD  = m.fecha    != null ? m.fecha    : "";
            this.hora     = m.horaDisplay();
            this.notas    = m.notas    != null ? m.notas    : "";
            this.estado   = m.estado   != null ? m.estado   : "pendiente";
            this.precio   = m.precioDisplay();

            if (fechaBD.length() >= 10) {
                this.anio     = Integer.parseInt(fechaBD.substring(0, 4));
                this.mes      = Integer.parseInt(fechaBD.substring(5, 7));
                this.diaMes   = Integer.parseInt(fechaBD.substring(8, 10));
                this.fecha    = String.format("%02d/%02d/%04d", diaMes, mes, anio);
                Calendar cal  = Calendar.getInstance();
                cal.set(anio, mes - 1, diaMes);
                int dow = cal.get(Calendar.DAY_OF_WEEK);
                this.diaSemana = (dow == Calendar.SUNDAY) ? 7 : dow - 1;
            }
        }

        Cita(String cliente, String servicio, String fecha, String hora,
             String precio, String notas, String estado,
             int diaSemana, int diaMes, int mes, int anio) {
            this.id        = null;
            this.cliente   = cliente;
            this.servicio  = servicio;
            this.fecha     = fecha;
            this.fechaBD   = String.format("%04d-%02d-%02d", anio, mes, diaMes);
            this.hora      = hora;
            this.precio    = precio;
            this.notas     = notas;
            this.estado    = estado;
            this.diaSemana = diaSemana;
            this.diaMes    = diaMes;
            this.mes       = mes;
            this.anio      = anio;
        }

        String inicial() {
            return cliente != null && !cliente.isEmpty()
                    ? String.valueOf(cliente.charAt(0)).toUpperCase() : "?";
        }
    }


    private final String[] SERVICIOS = {
            "Masaje"
    };

    private final List<Cita> todasLasCitas = new ArrayList<>();
    private String   vistaActual    = "diaria";
    private String   filtroServicio = "Todos";
    private Calendar fechaSeleccionada;
    private boolean  cargando       = false;

    private FrameLayout contenedor;
    private TextView    tabDiaria, tabSemanal, tabMensual;
    private TextView    tvSubtitle;


    private String  citaIdPendiente  = null;
    private String  clienteFiltroId  = null;  // viene de ClientesActivity
    private String  clienteFiltroNom = null;
    private boolean abrirNuevaCita   = false; // abrir sheet directo

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().getDecorView().setSystemUiVisibility(
                android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                        android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        );
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        setContentView(R.layout.activity_agenda);

        fechaSeleccionada = Calendar.getInstance();

        if (getIntent() != null) {
            citaIdPendiente  = getIntent().getStringExtra("CITA_ID");
            clienteFiltroId  = getIntent().getStringExtra("CLIENTE_ID");
            clienteFiltroNom = getIntent().getStringExtra("CLIENTE_NOMBRE");
            abrirNuevaCita   = getIntent().getBooleanExtra("ABRIR_NUEVA_CITA", false);

            String citaFecha = getIntent().getStringExtra("CITA_FECHA");
            if (citaFecha != null && citaFecha.length() >= 10) {
                try {
                    int anio = Integer.parseInt(citaFecha.substring(0, 4));
                    int mes  = Integer.parseInt(citaFecha.substring(5, 7)) - 1;
                    int dia  = Integer.parseInt(citaFecha.substring(8, 10));
                    fechaSeleccionada.set(anio, mes, dia);
                } catch (Exception ignored) {}
            }
        }

        bindViews();
        setupTabs();
        setupBotones();
        setupBottomNav();
        tabDiaria.setBackground(getDrawable(R.drawable.shape_tab_active));
        tabDiaria.setTextColor(Color.WHITE);
        cargarCitas();
    }

    private boolean primeraVez = true;

    @Override
    protected void onResume() {
        super.onResume();
        if (primeraVez) {
            primeraVez = false;
            return;
        }

        cargando = false;
        cargarCitas();
    }


    private void bindViews() {
        contenedor = findViewById(R.id.contenedorVista);
        tabDiaria  = findViewById(R.id.tabDiaria);
        tabSemanal = findViewById(R.id.tabSemanal);
        tabMensual = findViewById(R.id.tabMensual);
        tvSubtitle = findViewById(R.id.tvAgendaSubtitle);
    }



    private void cargarCitas() {
        if (cargando) return;
        cargando = true;

        Calendar desde = (Calendar) fechaSeleccionada.clone();
        desde.set(Calendar.DAY_OF_MONTH, 1);
        desde.add(Calendar.MONTH, -1);

        Calendar hasta = (Calendar) fechaSeleccionada.clone();
        hasta.set(Calendar.DAY_OF_MONTH, 1);
        hasta.add(Calendar.MONTH, 2);
        hasta.add(Calendar.DAY_OF_MONTH, -1);

        String desdeStr = fmt(desde);
        String hastaStr = fmt(hasta);

        SupabaseRepository.get().getCitasRango(desdeStr, hastaStr,
                new SupabaseRepository.Callback<List<CitaModel>>() {
                    @Override public void onSuccess(List<CitaModel> data) {
                        runOnUiThread(() -> {
                            cargando = false;
                            todasLasCitas.clear();
                            for (CitaModel m : data) {
                                if (m.clienteNombre == null) m.clienteNombre = "";
                                todasLasCitas.add(new Cita(m));
                            }
                            renderVista();

                            // Abrir cita concreta (desde MainActivity)
                            if (citaIdPendiente != null) {
                                for (Cita c : todasLasCitas) {
                                    if (citaIdPendiente.equals(c.id)) {
                                        citaIdPendiente = null;
                                        showDetalleCita(c);
                                        break;
                                    }
                                }
                                citaIdPendiente = null;
                            }

                            // Abrir sheet nueva cita directo (desde ClientesActivity)
                            if (abrirNuevaCita) {
                                abrirNuevaCita = false; // solo una vez
                                showNuevaCitaSheet(null);
                            }
                        });
                    }
                    @Override public void onError(String e) {
                        runOnUiThread(() -> {
                            cargando = false;
                            Toast.makeText(AgendaActivity.this,
                                    "Error cargando citas: " + e, Toast.LENGTH_SHORT).show();
                            renderVista();
                        });
                    }
                });
    }


    private void setupTabs() {
        tabDiaria.setOnClickListener(v  -> setVista("diaria",  tabDiaria));
        tabSemanal.setOnClickListener(v -> setVista("semanal", tabSemanal));
        tabMensual.setOnClickListener(v -> setVista("mensual", tabMensual));
    }

    private void setVista(String vista, TextView tab) {
        vistaActual = vista;
        for (TextView t : new TextView[]{tabDiaria, tabSemanal, tabMensual}) {
            t.setBackground(getDrawable(R.drawable.shape_tab_inactive));
            t.setTextColor(Color.parseColor("#6B7FA3"));
        }
        tab.setBackground(getDrawable(R.drawable.shape_tab_active));
        tab.setTextColor(Color.WHITE);
        renderVista();
    }


    private void renderVista() {
        // Restaurar siempre el estado visual de los tabs
        for (TextView t : new TextView[]{tabDiaria, tabSemanal, tabMensual}) {
            t.setBackground(getDrawable(R.drawable.shape_tab_inactive));
            t.setTextColor(Color.parseColor("#6B7FA3"));
        }
        TextView tabActivo = vistaActual.equals("semanal") ? tabSemanal
                : vistaActual.equals("mensual") ? tabMensual : tabDiaria;
        tabActivo.setBackground(getDrawable(R.drawable.shape_tab_active));
        tabActivo.setTextColor(Color.WHITE);

        contenedor.removeAllViews();
        switch (vistaActual) {
            case "diaria":  renderDiaria();  break;
            case "semanal": renderSemanal(); break;
            case "mensual": renderMensual(); break;
        }
    }

    // ── Vista diaria ─────────────────────────────────────────────────
    private void renderDiaria() {
        String fechaStr   = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                .format(fechaSeleccionada.getTime());
        String fechaLabel = new SimpleDateFormat("EEEE dd/MM/yyyy", new Locale("es","ES"))
                .format(fechaSeleccionada.getTime());
        fechaLabel = Character.toUpperCase(fechaLabel.charAt(0)) + fechaLabel.substring(1);
        tvSubtitle.setText(fechaLabel);

        ScrollView scroll = new ScrollView(this);
        scroll.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dpToPx(16), 0, dpToPx(16), dpToPx(16));
        scroll.addView(layout);
        layout.addView(buildNavFecha());

        List<Cita> citasDelDia = new ArrayList<>();
        for (Cita c : todasLasCitas) {
            if (c.fecha.equals(fechaStr) &&
                    (filtroServicio.equals("Todos") || c.servicio.equals(filtroServicio)))
                citasDelDia.add(c);
        }

        if (cargando) {
            layout.addView(buildVacioView("Cargando citas...", ""));
        } else if (citasDelDia.isEmpty()) {
            layout.addView(buildVacioView("Sin citas este día", "Pulsa ＋ para añadir una cita"));
        } else {
            for (Cita c : citasDelDia) layout.addView(buildCitaCard(c, false));
        }
        contenedor.addView(scroll);
    }

    // ── Vista semanal ────────────────────────────────────────────────
    private void renderSemanal() {
        Calendar inicioSemana = (Calendar) fechaSeleccionada.clone();
        int dow  = inicioSemana.get(Calendar.DAY_OF_WEEK);
        int diff = (dow == Calendar.SUNDAY) ? -6 : (Calendar.MONDAY - dow);
        inicioSemana.add(Calendar.DAY_OF_MONTH, diff);

        Calendar finSemana = (Calendar) inicioSemana.clone();
        finSemana.add(Calendar.DAY_OF_MONTH, 6);

        String label = new SimpleDateFormat("dd/MM", Locale.getDefault()).format(inicioSemana.getTime())
                + " – " + new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(finSemana.getTime());
        tvSubtitle.setText("Semana: " + label);

        ScrollView scroll = new ScrollView(this);
        scroll.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dpToPx(16), 0, dpToPx(16), dpToPx(16));
        scroll.addView(layout);
        layout.addView(buildNavFecha());

        String[] diasNombre = {"Lun","Mar","Mié","Jue","Vie","Sáb","Dom"};
        Calendar dia = (Calendar) inicioSemana.clone();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

        for (int i = 0; i < 7; i++) {
            String fechaDia  = sdf.format(dia.getTime());
            String nombreDia = diasNombre[i] + " " +
                    new SimpleDateFormat("dd/MM", Locale.getDefault()).format(dia.getTime());

            List<Cita> citasDia = new ArrayList<>();
            for (Cita c : todasLasCitas) {
                if (c.fecha.equals(fechaDia) &&
                        (filtroServicio.equals("Todos") || c.servicio.equals(filtroServicio)))
                    citasDia.add(c);
            }

            layout.addView(buildDiaHeader(nombreDia, citasDia.size(),
                    fechaDia.equals(sdf.format(Calendar.getInstance().getTime()))));

            if (citasDia.isEmpty()) {
                TextView tvVacio = new TextView(this);
                tvVacio.setText("Sin citas");
                tvVacio.setTextSize(11f);
                tvVacio.setTextColor(Color.parseColor("#6B7FA3"));
                tvVacio.setTypeface(getResources().getFont(R.font.outfit_regular));
                LinearLayout.LayoutParams vp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                vp.setMargins(dpToPx(8), dpToPx(4), 0, dpToPx(10));
                tvVacio.setLayoutParams(vp);
                layout.addView(tvVacio);
            } else {
                for (Cita c : citasDia) layout.addView(buildCitaCard(c, true));
            }
            dia.add(Calendar.DAY_OF_MONTH, 1);
        }
        contenedor.addView(scroll);
    }

    // ── Vista mensual ────────────────────────────────────────────────
    private void renderMensual() {
        int mes  = fechaSeleccionada.get(Calendar.MONTH);
        int anio = fechaSeleccionada.get(Calendar.YEAR);
        String mesLabel = new SimpleDateFormat("MMMM yyyy", new Locale("es","ES"))
                .format(fechaSeleccionada.getTime());
        mesLabel = Character.toUpperCase(mesLabel.charAt(0)) + mesLabel.substring(1);
        tvSubtitle.setText(mesLabel);

        ScrollView scroll = new ScrollView(this);
        scroll.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dpToPx(16), 0, dpToPx(16), dpToPx(16));
        scroll.addView(layout);
        layout.addView(buildNavFecha());

        // Cabecera L M X J V S D
        String[] diasHdr = {"L","M","X","J","V","S","D"};
        LinearLayout hdrRow = new LinearLayout(this);
        hdrRow.setOrientation(LinearLayout.HORIZONTAL);
        hdrRow.setWeightSum(7);
        LinearLayout.LayoutParams hdrP = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        hdrP.bottomMargin = dpToPx(4);
        hdrRow.setLayoutParams(hdrP);
        for (String d : diasHdr) {
            TextView tv = new TextView(this);
            tv.setText(d);
            tv.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            tv.setTextSize(11f);
            tv.setTextColor(Color.parseColor("#6B7FA3"));
            tv.setTypeface(getResources().getFont(R.font.outfit_bold));
            tv.setGravity(Gravity.CENTER);
            tv.setPadding(0, dpToPx(4), 0, dpToPx(4));
            hdrRow.addView(tv);
        }
        layout.addView(hdrRow);

        // Grid
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, anio);
        cal.set(Calendar.MONTH, mes);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        int primerDia  = cal.get(Calendar.DAY_OF_WEEK);
        int offset     = (primerDia == Calendar.SUNDAY) ? 6 : primerDia - 2;
        int diasEnMes  = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        Calendar hoy = Calendar.getInstance();

        int celda = 0;
        LinearLayout fila = null;
        for (int i = 0; i < offset + diasEnMes; i++) {
            if (celda % 7 == 0) {
                fila = new LinearLayout(this);
                fila.setOrientation(LinearLayout.HORIZONTAL);
                fila.setWeightSum(7);
                LinearLayout.LayoutParams fp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(52));
                fp.bottomMargin = dpToPx(4);
                fila.setLayoutParams(fp);
                layout.addView(fila);
            }
            if (i < offset) {
                View sp = new View(this);
                sp.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f));
                fila.addView(sp);
            } else {
                int numDia = i - offset + 1;
                cal.set(Calendar.DAY_OF_MONTH, numDia);
                String fechaDia = sdf.format(cal.getTime());
                boolean esHoy = (numDia == hoy.get(Calendar.DAY_OF_MONTH)
                        && mes == hoy.get(Calendar.MONTH) && anio == hoy.get(Calendar.YEAR));
                boolean esSel = (numDia == fechaSeleccionada.get(Calendar.DAY_OF_MONTH)
                        && mes == fechaSeleccionada.get(Calendar.MONTH)
                        && anio == fechaSeleccionada.get(Calendar.YEAR));
                int count = 0;
                for (Cita c : todasLasCitas) if (c.fecha.equals(fechaDia)) count++;

                FrameLayout celdaView = buildCeldaMes(numDia, count, esHoy, esSel);
                celdaView.setLayoutParams(new LinearLayout.LayoutParams(
                        0, LinearLayout.LayoutParams.MATCH_PARENT, 1f));
                final int diaF = numDia;
                celdaView.setOnClickListener(v -> {
                    fechaSeleccionada.set(anio, mes, diaF);
                    setVista("diaria", tabDiaria);
                });
                fila.addView(celdaView);
            }
            celda++;
        }
        if (fila != null && celda % 7 != 0) {
            int rest = 7 - (celda % 7);
            for (int i = 0; i < rest; i++) {
                View sp = new View(this);
                sp.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f));
                fila.addView(sp);
            }
        }

        // Lista citas del mes
        TextView tvTitulo = new TextView(this);
        tvTitulo.setText("Citas este mes");
        tvTitulo.setTextSize(13f);
        tvTitulo.setTextColor(Color.parseColor("#0D1B3E"));
        tvTitulo.setTypeface(getResources().getFont(R.font.outfit_bold));
        LinearLayout.LayoutParams ltP = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        ltP.topMargin = dpToPx(16);
        ltP.bottomMargin = dpToPx(8);
        tvTitulo.setLayoutParams(ltP);
        layout.addView(tvTitulo);

        List<Cita> citasMes = new ArrayList<>();
        for (Cita c : todasLasCitas) {
            if (c.mes == mes + 1 && c.anio == anio &&
                    (filtroServicio.equals("Todos") || c.servicio.equals(filtroServicio)))
                citasMes.add(c);
        }

        if (citasMes.isEmpty()) layout.addView(buildVacioView("Sin citas este mes", ""));
        else for (Cita c : citasMes) layout.addView(buildCitaCard(c, false));

        contenedor.addView(scroll);
    }


    private View buildNavFecha() {
        CardView nav = new CardView(this);
        LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        np.bottomMargin = dpToPx(12);
        nav.setLayoutParams(np);
        nav.setRadius(dpToPx(16));
        nav.setCardElevation(dpToPx(2));
        nav.setCardBackgroundColor(Color.WHITE);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8));
        nav.addView(row);

        TextView btnPrev = buildNavBtn("‹");
        btnPrev.setOnClickListener(v -> { avanzarFecha(-1); cargarCitas(); });
        row.addView(btnPrev);

        TextView tvFecha = new TextView(this);
        tvFecha.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        tvFecha.setGravity(Gravity.CENTER);
        tvFecha.setTextSize(13f);
        tvFecha.setTextColor(Color.parseColor("#0D1B3E"));
        tvFecha.setTypeface(getResources().getFont(R.font.outfit_bold));
        SimpleDateFormat fmt;
        switch (vistaActual) {
            case "semanal": fmt = new SimpleDateFormat("'Semana del' dd/MM", new Locale("es","ES")); break;
            case "mensual": fmt = new SimpleDateFormat("MMMM yyyy", new Locale("es","ES")); break;
            default:        fmt = new SimpleDateFormat("EEE dd/MM/yyyy", new Locale("es","ES")); break;
        }
        String label = fmt.format(fechaSeleccionada.getTime());
        label = Character.toUpperCase(label.charAt(0)) + label.substring(1);
        tvFecha.setText(label);
        tvFecha.setOnClickListener(v -> mostrarDatePicker());
        row.addView(tvFecha);

        TextView btnNext = buildNavBtn("›");
        btnNext.setOnClickListener(v -> { avanzarFecha(1); cargarCitas(); });
        row.addView(btnNext);

        TextView btnHoy = new TextView(this);
        btnHoy.setText("Hoy");
        btnHoy.setTextSize(11f);
        btnHoy.setTextColor(Color.parseColor("#0A66FF"));
        btnHoy.setTypeface(getResources().getFont(R.font.outfit_bold));
        btnHoy.setPadding(dpToPx(10), dpToPx(6), dpToPx(10), dpToPx(6));
        btnHoy.setBackground(getDrawable(R.drawable.shape_filter_inactive));
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        hp.setMarginStart(dpToPx(6));
        btnHoy.setLayoutParams(hp);
        btnHoy.setOnClickListener(v -> {
            fechaSeleccionada = Calendar.getInstance();
            cargarCitas();
        });
        row.addView(btnHoy);
        return nav;
    }

    private TextView buildNavBtn(String texto) {
        TextView btn = new TextView(this);
        btn.setText(texto);
        btn.setTextSize(22f);
        btn.setTextColor(Color.parseColor("#0A66FF"));
        btn.setTypeface(getResources().getFont(R.font.outfit_bold));
        btn.setPadding(dpToPx(12), dpToPx(4), dpToPx(12), dpToPx(4));
        btn.setGravity(Gravity.CENTER);
        return btn;
    }

    private void avanzarFecha(int cantidad) {
        switch (vistaActual) {
            case "diaria":  fechaSeleccionada.add(Calendar.DAY_OF_MONTH, cantidad); break;
            case "semanal": fechaSeleccionada.add(Calendar.WEEK_OF_YEAR, cantidad); break;
            case "mensual": fechaSeleccionada.add(Calendar.MONTH, cantidad); break;
        }
    }

    private View buildDiaHeader(String nombreDia, int numCitas, boolean esHoy) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rp.topMargin = dpToPx(8);
        rp.bottomMargin = dpToPx(4);
        row.setLayoutParams(rp);

        TextView tvDia = new TextView(this);
        tvDia.setText(nombreDia);
        tvDia.setTextSize(12f);
        tvDia.setTextColor(esHoy ? Color.parseColor("#0A66FF") : Color.parseColor("#0D1B3E"));
        tvDia.setTypeface(getResources().getFont(R.font.outfit_bold));
        row.addView(tvDia);

        if (numCitas > 0) {
            TextView badge = new TextView(this);
            badge.setText(numCitas + " cita" + (numCitas > 1 ? "s" : ""));
            badge.setTextSize(10f);
            badge.setTextColor(Color.WHITE);
            badge.setTypeface(getResources().getFont(R.font.outfit_bold));
            badge.setPadding(dpToPx(8), dpToPx(3), dpToPx(8), dpToPx(3));
            badge.setBackground(getDrawable(R.drawable.shape_chip_blue));
            LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            bp.setMarginStart(dpToPx(8));
            badge.setLayoutParams(bp);
            row.addView(badge);
        }

        View linea = new View(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dpToPx(1), 1f);
        lp.setMarginStart(dpToPx(10));
        linea.setLayoutParams(lp);
        linea.setBackgroundColor(Color.parseColor("#DDE6FF"));
        row.addView(linea);
        return row;
    }

    private FrameLayout buildCeldaMes(int dia, int numCitas, boolean esHoy, boolean esSel) {
        FrameLayout frame = new FrameLayout(this);
        frame.setPadding(dpToPx(2), dpToPx(2), dpToPx(2), dpToPx(2));

        CardView card = new CardView(this);
        card.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        card.setRadius(dpToPx(10));
        card.setCardElevation(esHoy || esSel ? dpToPx(3) : dpToPx(1));
        card.setCardBackgroundColor(esSel ? Color.parseColor("#0A66FF")
                : esHoy ? Color.parseColor("#E8F0FF") : Color.WHITE);

        LinearLayout inner = new LinearLayout(this);
        inner.setOrientation(LinearLayout.VERTICAL);
        inner.setGravity(Gravity.CENTER);
        inner.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        card.addView(inner);

        TextView tvDia = new TextView(this);
        tvDia.setText(String.valueOf(dia));
        tvDia.setTextSize(13f);
        tvDia.setTextColor(esSel ? Color.WHITE
                : esHoy ? Color.parseColor("#0A66FF") : Color.parseColor("#0D1B3E"));
        tvDia.setTypeface(getResources().getFont(
                (esHoy || esSel) ? R.font.outfit_bold : R.font.outfit_regular));
        tvDia.setGravity(Gravity.CENTER);
        inner.addView(tvDia);

        if (numCitas > 0) {
            View dot = new View(this);
            LinearLayout.LayoutParams dp2 = new LinearLayout.LayoutParams(dpToPx(5), dpToPx(5));
            dp2.gravity = Gravity.CENTER_HORIZONTAL;
            dp2.topMargin = dpToPx(2);
            dot.setLayoutParams(dp2);
            dot.setBackground(getDrawable(R.drawable.shape_dot));
            dot.getBackground().setTint(esSel ? Color.WHITE : Color.parseColor("#0A66FF"));
            inner.addView(dot);
        }
        frame.addView(card);
        return frame;
    }

    private View buildCitaCard(Cita cita, boolean compacto) {
        CardView card = new CardView(this);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cp.bottomMargin = dpToPx(compacto ? 6 : 10);
        card.setLayoutParams(cp);
        card.setRadius(dpToPx(16));
        card.setCardElevation(dpToPx(2));
        card.setCardBackgroundColor(
                "cancelada".equals(cita.estado) ? Color.parseColor("#FFF5F5") : Color.WHITE);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12));
        card.addView(row);

        // Barra estado
        View barra = new View(this);
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(dpToPx(4), LinearLayout.LayoutParams.MATCH_PARENT);
        bp.setMarginEnd(dpToPx(12));
        barra.setLayoutParams(bp);
        barra.setMinimumHeight(dpToPx(40));
        barra.post(() -> {
            android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
            bg.setColor(getEstadoColor(cita.estado));
            bg.setCornerRadius(dpToPx(4));
            barra.setBackground(bg);
        });
        row.addView(barra);

        // Avatar
        if (!compacto) {
            CardView avatar = new CardView(this);
            LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(dpToPx(42), dpToPx(42));
            ap.setMarginEnd(dpToPx(10));
            avatar.setLayoutParams(ap);
            avatar.setRadius(dpToPx(13));
            avatar.setCardElevation(dpToPx(2));
            avatar.setCardBackgroundColor(Color.parseColor("#0A66FF"));
            TextView tvI = new TextView(this);
            tvI.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
            tvI.setText(cita.inicial());
            tvI.setTextSize(16f);
            tvI.setTextColor(Color.WHITE);
            tvI.setTypeface(getResources().getFont(R.font.outfit_bold));
            tvI.setGravity(Gravity.CENTER);
            avatar.addView(tvI);
            row.addView(avatar);
        }

        // Texto
        LinearLayout textBlock = new LinearLayout(this);
        textBlock.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams tbp = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        tbp.setMarginEnd(dpToPx(4));
        textBlock.setLayoutParams(tbp);

        TextView tvNombre = new TextView(this);
        tvNombre.setText(cita.cliente);
        tvNombre.setTextSize(compacto ? 11.5f : 13f);
        tvNombre.setTextColor(Color.parseColor("#0D1B3E"));
        tvNombre.setTypeface(getResources().getFont(R.font.outfit_bold));
        tvNombre.setMaxLines(1);
        tvNombre.setEllipsize(android.text.TextUtils.TruncateAt.END);
        textBlock.addView(tvNombre);

        TextView tvServicio = new TextView(this);
        tvServicio.setText(cita.servicio + (compacto ? "" : " · " + cita.hora));
        tvServicio.setTextSize(10f);
        tvServicio.setTextColor(Color.parseColor("#6B7FA3"));
        tvServicio.setTypeface(getResources().getFont(R.font.outfit_regular));
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        sp.topMargin = dpToPx(2);
        tvServicio.setLayoutParams(sp);
        textBlock.addView(tvServicio);
        row.addView(textBlock);

        // Hora + precio
        LinearLayout rightCol = new LinearLayout(this);
        rightCol.setOrientation(LinearLayout.VERTICAL);
        rightCol.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rcp = new LinearLayout.LayoutParams(dpToPx(52), LinearLayout.LayoutParams.WRAP_CONTENT);
        rcp.setMarginStart(dpToPx(8));
        rightCol.setLayoutParams(rcp);

        TextView tvHora = new TextView(this);
        tvHora.setText(cita.hora);
        tvHora.setTextSize(13f);
        tvHora.setTextColor(Color.parseColor("#0A66FF"));
        tvHora.setTypeface(getResources().getFont(R.font.outfit_bold));
        tvHora.setGravity(Gravity.END);
        tvHora.setSingleLine(true);
        rightCol.addView(tvHora);

        TextView tvPrecio = new TextView(this);
        tvPrecio.setText(cita.precio);
        tvPrecio.setTextSize(10f);
        tvPrecio.setTextColor(Color.parseColor("#6B7FA3"));
        tvPrecio.setTypeface(getResources().getFont(R.font.outfit_regular));
        tvPrecio.setGravity(Gravity.END);
        LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        pp.topMargin = dpToPx(2);
        tvPrecio.setLayoutParams(pp);
        rightCol.addView(tvPrecio);
        row.addView(rightCol);

        card.setClickable(true);
        card.setForeground(getDrawable(android.R.drawable.list_selector_background));
        card.setOnClickListener(v -> showDetalleCita(cita));
        return card;
    }

    private View buildVacioView(String titulo, String subtitulo) {
        LinearLayout v = new LinearLayout(this);
        v.setOrientation(LinearLayout.VERTICAL);
        v.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams vp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        vp.topMargin = dpToPx(40);
        vp.bottomMargin = dpToPx(40);
        v.setLayoutParams(vp);

        TextView emoji = new TextView(this);
        emoji.setText("📅");
        emoji.setTextSize(42f);
        emoji.setGravity(Gravity.CENTER);
        v.addView(emoji);

        TextView tvT = new TextView(this);
        tvT.setText(titulo);
        tvT.setTextSize(15f);
        tvT.setTextColor(Color.parseColor("#0D1B3E"));
        tvT.setTypeface(getResources().getFont(R.font.outfit_bold));
        tvT.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        tp.topMargin = dpToPx(10);
        tvT.setLayoutParams(tp);
        v.addView(tvT);

        if (!subtitulo.isEmpty()) {
            TextView tvS = new TextView(this);
            tvS.setText(subtitulo);
            tvS.setTextSize(11f);
            tvS.setTextColor(Color.parseColor("#6B7FA3"));
            tvS.setTypeface(getResources().getFont(R.font.outfit_regular));
            tvS.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams spp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            spp.topMargin = dpToPx(4);
            tvS.setLayoutParams(spp);
            v.addView(tvS);
        }
        return v;
    }


    private void showDetalleCita(Cita cita) {
        BottomSheetDialog sheet = new BottomSheetDialog(
                this, com.google.android.material.R.style.Theme_Material3_Light_BottomSheetDialog);
        View view = getLayoutInflater().inflate(R.layout.sheet_detalle_cita, null);
        sheet.setContentView(view);
        sheet.setOnShowListener(d -> {
            android.view.View bottomSheet = sheet.getDelegate()
                    .findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                com.google.android.material.bottomsheet.BottomSheetBehavior<android.view.View> behavior =
                        com.google.android.material.bottomsheet.BottomSheetBehavior.from(bottomSheet);
                bottomSheet.getLayoutParams().height =
                        getResources().getDisplayMetrics().heightPixels;
                behavior.setPeekHeight(getResources().getDisplayMetrics().heightPixels);
                behavior.setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
            }
        });

        ((TextView) view.findViewById(R.id.tvCitaInicial)).setText(cita.inicial());
        ((TextView) view.findViewById(R.id.tvCitaCliente)).setText(cita.cliente);
        ((TextView) view.findViewById(R.id.tvCitaServicio)).setText(cita.servicio);
        ((TextView) view.findViewById(R.id.tvCitaFechaHora)).setText("📅 " + cita.fecha + " · " + cita.hora + "h");
        ((TextView) view.findViewById(R.id.tvCitaPrecio)).setText("💰 " + cita.precio);
        ((TextView) view.findViewById(R.id.tvCitaNotas)).setText(
                "📝 " + (cita.notas.isEmpty() ? "Sin notas" : cita.notas));

        TextView tvBadge = view.findViewById(R.id.tvCitaEstadoBadge);
        tvBadge.setText(cita.estado.substring(0, 1).toUpperCase() + cita.estado.substring(1));
        tvBadge.getBackground().setTint(getEstadoColor(cita.estado));

        // ── CONFIRMAR ─────────────────────────────────────────────────
        // → Estado cita: confirmada
        // → Cobros: eliminar cobrado (si existe), crear/mantener pendiente
        view.findViewById(R.id.btnCitaConfirmar).setOnClickListener(v ->
                accionCita(cita, "confirmada", sheet));

        view.findViewById(R.id.btnCitaEditar).setOnClickListener(v -> {
            sheet.dismiss();
            showNuevaCitaSheet(cita);
        });

        // ── COBRAR ────────────────────────────────────────────────────
        // → Estado cita: cobrada
        // → Cobros: eliminar pendiente (si existe), crear/mantener cobrado
        // → No aparece en citas de hoy del dashboard (cobrada = procesada)
        view.findViewById(R.id.btnCitaCobrar).setOnClickListener(v ->
                accionCita(cita, "cobrada", sheet));

        // ── CANCELAR ──────────────────────────────────────────────────
        // → Estado cita: cancelada
        // → Cobros: eliminar TODOS los cobros asociados
        // → Desaparece del dashboard (buildListaCitas filtra canceladas)
        view.findViewById(R.id.btnCitaCancelar).setOnClickListener(v ->
                accionCita(cita, "cancelada", sheet));

        sheet.show();
    }

    /**
     * Punto central de todas las acciones sobre una cita.
     * 1. Cambia el estado en Supabase
     * 2. Gestiona los cobros según la transición de estado
     * 3. Actualiza la UI
     *
     * Tabla de comportamiento:
     *  CONFIRMAR → crea cobro PENDIENTE si no existe ninguno;
     *              si había cobrado → lo elimina y crea pendiente
     *  COBRAR    → si hay pendiente → lo marca cobrado (no duplica);
     *              si no hay ninguno → crea cobrado;
     *              si ya había cobrado → no hace nada
     *  CANCELAR  → elimina TODOS los cobros asociados (pendientes y cobrados)
     */
    private void accionCita(Cita cita, String nuevoEstado, BottomSheetDialog sheet) {
        if (cita.id == null) {
            cita.estado = nuevoEstado;
            sheet.dismiss();
            renderVista();
            return;
        }

        // Calcular importe una sola vez
        double imp = 0;
        try { imp = Double.parseDouble(cita.precio.replace("€","").replace(",",".").trim()); }
        catch (Exception ignored) {}
        final double importe = imp;
        final String concepto = (cita.servicio != null && !cita.servicio.isEmpty()
                ? cita.servicio : "Servicio") + " · " + cita.fecha;
        final String estadoAnterior = cita.estado;

        // Paso 1: cambiar estado de la cita en Supabase
        SupabaseRepository.get().cambiarEstadoCita(cita.id, nuevoEstado,
                new SupabaseRepository.Callback<Void>() {
                    @Override public void onSuccess(Void data) {
                        // Actualizar en memoria y UI
                        cita.estado = nuevoEstado;
                        runOnUiThread(() -> {
                            sheet.dismiss();
                            renderVista();
                            Toast.makeText(AgendaActivity.this,
                                    mensajeCita(nuevoEstado), Toast.LENGTH_SHORT).show();
                        });

                        if (importe <= 0) return;

                        // Paso 2: gestionar cobros según el nuevo estado
                        gestionarCobros(cita, nuevoEstado, estadoAnterior, concepto, importe);
                    }
                    @Override public void onError(String e) {
                        runOnUiThread(() -> Toast.makeText(AgendaActivity.this,
                                "Error: " + e, Toast.LENGTH_SHORT).show());
                    }
                });
    }

    /** Gestiona los cobros según la transición de estado.
     *  Si la cita tiene id, usa getCobrosPorCita (más preciso).
     *  Fallback: busca entre todos los cobros por cliente+servicio+importe. */
    private void gestionarCobros(Cita cita, String nuevoEstado, String estadoAnterior,
                                 String concepto, double importe) {
        if (cita.id != null) {
            // Usar búsqueda directa por cita_id (rápido y preciso)
            SupabaseRepository.get().getCobrosPorCita(cita.id,
                    new SupabaseRepository.Callback<List<CobroModel>>() {
                        @Override public void onSuccess(List<CobroModel> cobros) {
                            procesarCambioEstadoCobros(cobros, cita, nuevoEstado, concepto, importe);
                        }
                        @Override public void onError(String e) {
                            android.util.Log.e("AGENDA", "Error buscando cobros por cita: " + e);
                            // Fallback: buscar entre todos
                            gestionarCobrosFallback(cita, nuevoEstado, concepto, importe);
                        }
                    });
        } else {
            gestionarCobrosFallback(cita, nuevoEstado, concepto, importe);
        }
    }

    private void gestionarCobrosFallback(Cita cita, String nuevoEstado,
                                         String concepto, double importe) {
        SupabaseRepository.get().getCobros(null,
                new SupabaseRepository.Callback<List<CobroModel>>() {
                    @Override public void onSuccess(List<CobroModel> todos) {
                        List<CobroModel> deCita = new ArrayList<>();
                        for (CobroModel c : todos) {
                            if (esMismaCita(c, cita, importe)) deCita.add(c);
                        }
                        procesarCambioEstadoCobros(deCita, cita, nuevoEstado, concepto, importe);
                    }
                    @Override public void onError(String e) {
                        android.util.Log.e("AGENDA", "Error leyendo cobros: " + e);
                    }
                });
    }

    private void procesarCambioEstadoCobros(List<CobroModel> cobros, Cita cita,
                                            String nuevoEstado, String concepto, double importe) {
        String idPendiente = null;
        String idCobrado   = null;
        for (CobroModel c : cobros) {
            if ("pendiente".equals(c.estado) && idPendiente == null)
                idPendiente = c.id;
            else if ("cobrado".equals(c.estado) && idCobrado == null)
                idCobrado = c.id;
        }

        final String pId = idPendiente;
        final String cId = idCobrado;

        switch (nuevoEstado) {
            case "confirmada":
                // Si había cobrado → eliminar y crear pendiente
                // Si había pendiente → dejarlo (ya está bien)
                // Si no había nada → crear pendiente
                if (cId != null) {
                    SupabaseRepository.get().eliminarCobro(cId,
                            new SupabaseRepository.Callback<Void>() {
                                @Override public void onSuccess(Void d) {
                                    if (pId == null)
                                        crearCobroAsync(cita, concepto, importe, "pendiente");
                                }
                                @Override public void onError(String e) {}
                            });
                } else if (pId == null) {
                    // No existe ninguno → crear pendiente
                    crearCobroAsync(cita, concepto, importe, "pendiente");
                }
                // Si ya hay pendiente y no hay cobrado → no hacer nada
                break;

            case "cobrada":
                // Si hay pendiente → marcarlo cobrado (y asegurar cita_id)
                if (pId != null) {
                    java.util.Map<String, Object> body = new java.util.HashMap<>();
                    body.put("estado", "cobrado");
                    // Asignar cita_id si el cobro no lo tenía (cobros viejos sin vínculo)
                    if (cita.id != null) body.put("cita_id", cita.id);
                    SupabaseRepository.get().actualizarCobro(pId, body,
                            new SupabaseRepository.Callback<Void>() {
                                @Override public void onSuccess(Void d) {
                                    runOnUiThread(() -> Toast.makeText(
                                            AgendaActivity.this,
                                            "💰 Cobro de " + String.format(java.util.Locale.US, "%.2f", importe).replace(".", ",") + "€ cobrado",
                                            Toast.LENGTH_SHORT).show());
                                }
                                @Override public void onError(String e) {}
                            });
                } else if (cId == null) {
                    // No hay ninguno → crear cobrado
                    crearCobroAsync(cita, concepto, importe, "cobrado");
                } else {
                    // Ya hay cobrado → asegurar que tiene cita_id (cobros viejos)
                    if (cita.id != null) {
                        java.util.Map<String, Object> bodyLink = new java.util.HashMap<>();
                        bodyLink.put("cita_id", cita.id);
                        SupabaseRepository.get().actualizarCobro(cId, bodyLink,
                                new SupabaseRepository.Callback<Void>() {
                                    @Override public void onSuccess(Void d) {}
                                    @Override public void onError(String e) {}
                                });
                    }
                }
                break;

            case "cancelada":
                // Eliminar TODOS los cobros asociados
                List<String> aEliminar = new ArrayList<>();
                if (pId != null) aEliminar.add(pId);
                if (cId != null) aEliminar.add(cId);
                for (CobroModel c : cobros) {
                    if (!aEliminar.contains(c.id)) aEliminar.add(c.id);
                }
                eliminarCobrosEnCadena(aEliminar, 0, () ->
                        runOnUiThread(() -> Toast.makeText(
                                AgendaActivity.this,
                                "🗑 Cobros eliminados automáticamente",
                                Toast.LENGTH_SHORT).show()));
                break;
        }
    }

    /** ¿Este CobroModel pertenece a esta cita?
     *  Criterio prioritario: cita_id directo (cobros nuevos).
     *  Fallback: cliente + servicio en concepto + importe (cobros viejos sin cita_id). */
    private boolean esMismaCita(CobroModel c, Cita cita, double importe) {
        // Criterio primario: cita_id directo (más fiable)
        if (cita.id != null && c.citaId != null) {
            return cita.id.equals(c.citaId);
        }
        // Fallback para cobros sin cita_id (cobros creados antes de esta corrección)
        boolean mismoCliente = cita.clienteId != null && c.clienteId != null
                ? cita.clienteId.equals(c.clienteId)
                : (cita.cliente != null && cita.cliente.equals(c.clienteNombre));
        String servLower = cita.servicio != null ? cita.servicio.toLowerCase() : "";
        boolean mismoServicio = !servLower.isEmpty() && c.concepto != null
                && c.concepto.toLowerCase().contains(servLower);
        boolean mismoImporte = Math.abs(c.importe - importe) < 0.01;
        return mismoCliente && mismoServicio && mismoImporte;
    }

    /** Crea un cobro asíncronamente y muestra Toast al terminar.
     *  IMPORTANTE: pasa cita.id como citaId para vincular el cobro con la cita
     *  y permitir la sincronización bidireccional desde CobrosActivity. */
    private void crearCobroAsync(Cita cita, String concepto, double importe, String estado) {
        SupabaseRepository.get().crearCobro(
                cita.id,        // citaId — CLAVE para sincronización bidireccional
                cita.clienteId, cita.cliente, concepto, importe,
                "Efectivo", estado, "",
                new SupabaseRepository.Callback<CobroModel>() {
                    @Override public void onSuccess(CobroModel cobro) {
                        String msg = "pendiente".equals(estado)
                                ? "⏳ Pago pendiente de " + String.format(java.util.Locale.US, "%.2f", importe).replace(".", ",") + "€" + " creado"
                                : "💰 Cobro de " + String.format(java.util.Locale.US, "%.2f", importe).replace(".", ",") + "€" + " registrado";
                        runOnUiThread(() -> Toast.makeText(AgendaActivity.this, msg, Toast.LENGTH_SHORT).show());
                    }
                    @Override public void onError(String e) {
                        android.util.Log.e("AGENDA", "Error creando cobro (" + estado + "): " + e);
                    }
                });
    }

    /** Elimina una lista de cobros en cadena; llama onDone al terminar todos */
    private void eliminarCobrosEnCadena(List<String> ids, int idx, Runnable onDone) {
        if (idx >= ids.size()) {
            if (onDone != null) onDone.run();
            return;
        }
        SupabaseRepository.get().eliminarCobro(ids.get(idx),
                new SupabaseRepository.Callback<Void>() {
                    @Override public void onSuccess(Void d) { eliminarCobrosEnCadena(ids, idx + 1, onDone); }
                    @Override public void onError(String e)  { eliminarCobrosEnCadena(ids, idx + 1, onDone); }
                });
    }

    private String mensajeCita(String estado) {
        switch (estado) {
            case "confirmada": return "✅ Cita confirmada · pago pendiente creado";
            case "cobrada":    return "💰 Cita cobrada";
            case "cancelada":  return "❌ Cita cancelada";
            default:           return "Estado actualizado";
        }
    }

    private void showNuevaCitaSheet(Cita citaEditar) {
        BottomSheetDialog sheet = new BottomSheetDialog(
                this, com.google.android.material.R.style.Theme_Material3_Light_BottomSheetDialog);
        View view = getLayoutInflater().inflate(R.layout.sheet_nueva_cita, null);
        sheet.setContentView(view);

        EditText etCliente = view.findViewById(R.id.etCitaCliente);
        EditText etPrecio  = view.findViewById(R.id.etCitaPrecio);
        EditText etNotas   = view.findViewById(R.id.etCitaNotas);
        TextView tvFecha   = view.findViewById(R.id.tvCitaFecha);
        TextView tvHora    = view.findViewById(R.id.tvCitaHora);
        TextView tvTitulo  = view.findViewById(R.id.tvNuevaCitaTitulo);
        CardView btnGuardarCard = view.findViewById(R.id.btnGuardarCita);
        TextView btnGuardar = (TextView) btnGuardarCard.getChildAt(0);

        // ── Estado del formulario ──────────────────────────────────
        final String[] clienteIdSel = {null};  // UUID del cliente seleccionado (puede ser null)
        final String[] servicioSel  = {SERVICIOS[0]};
        final String[] fechaSelStr  = {new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                .format(fechaSeleccionada.getTime())};
        final String[] horaSelStr   = {"10:00"};

        // Si es edición, prerellenar
        if (citaEditar != null) {
            tvTitulo.setText("Editar cita");
            etCliente.setText(citaEditar.cliente);
            etPrecio.setText(citaEditar.precio.replace("€", "").trim());
            etNotas.setText(citaEditar.notas);
            tvFecha.setText("📅 " + citaEditar.fecha);
            tvHora.setText("🕐 " + citaEditar.hora);
            servicioSel[0] = citaEditar.servicio;
            fechaSelStr[0] = citaEditar.fecha;
            horaSelStr[0]  = citaEditar.hora;
        } else {
            tvFecha.setText("📅 " + fechaSelStr[0]);
            tvHora.setText("🕐 " + horaSelStr[0]);
            // Si venimos de ClientesActivity, prerellenar cliente y poner título claro
            if (clienteFiltroNom != null && !clienteFiltroNom.isEmpty()) {
                etCliente.setText(clienteFiltroNom);
                clienteIdSel[0] = clienteFiltroId;
                tvTitulo.setText("Nueva cita · " + clienteFiltroNom);
            }
        }

        // ── Lista de sugerencias de clientes ──────────────────────
        // La insertamos en el ScrollView raíz justo después del CardView del cliente
        LinearLayout layoutSug = new LinearLayout(this);
        layoutSug.setOrientation(LinearLayout.VERTICAL);
        layoutSug.setVisibility(View.GONE);
        LinearLayout.LayoutParams sugLP = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        sugLP.setMargins(dpToPx(16), 0, dpToPx(16), dpToPx(8));
        layoutSug.setLayoutParams(sugLP);
        // etCliente → CardView → LinearLayout raíz
        android.view.ViewGroup cardCliente = (android.view.ViewGroup) etCliente.getParent();
        android.view.ViewGroup rootLayout  = (android.view.ViewGroup) cardCliente.getParent();
        int idxCard = -1;
        for (int i = 0; i < rootLayout.getChildCount(); i++) {
            if (rootLayout.getChildAt(i) == cardCliente) { idxCard = i; break; }
        }
        if (idxCard >= 0) rootLayout.addView(layoutSug, idxCard + 1);

        // ── TextWatcher: busca mientras el usuario escribe ─────────
        // IMPORTANTE: la selección de un cliente de la lista usa un flag
        // para que setText() no dispare una nueva búsqueda
        final boolean[] seleccionando = {false};

        etCliente.addTextChangedListener(new android.text.TextWatcher() {
            private final android.os.Handler h = new android.os.Handler();
            private Runnable r;
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            public void onTextChanged(CharSequence s, int a, int b, int c) {
                if (seleccionando[0]) return;
                // El usuario escribió → limpiar la selección anterior
                clienteIdSel[0] = null;
                if (r != null) h.removeCallbacks(r);
            }
            public void afterTextChanged(android.text.Editable s) {
                if (seleccionando[0]) {
                    seleccionando[0] = false;
                    return;
                }
                String txt = s.toString().trim();
                if (txt.length() < 2) {
                    layoutSug.setVisibility(View.GONE);
                    layoutSug.removeAllViews();
                    return;
                }
                r = () -> mostrarSugerencias(txt, layoutSug, etCliente, clienteIdSel, seleccionando);
                h.postDelayed(r, 350);
            }
        });

        // ── Chips de servicio ──────────────────────────────────────
        LinearLayout layoutServ = view.findViewById(R.id.layoutServicios);
        for (String serv : SERVICIOS) {
            TextView chip = new TextView(this);
            chip.setText(serv);
            chip.setTextSize(11f);
            chip.setTypeface(getResources().getFont(R.font.outfit_bold));
            chip.setPadding(dpToPx(14), dpToPx(8), dpToPx(14), dpToPx(8));
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            cp.setMarginEnd(dpToPx(6));
            chip.setLayoutParams(cp);
            boolean activo = serv.equals(servicioSel[0]);
            chip.setBackground(getDrawable(activo
                    ? R.drawable.shape_filter_active : R.drawable.shape_filter_inactive));
            chip.setTextColor(activo ? Color.WHITE : Color.parseColor("#6B7FA3"));
            chip.setOnClickListener(v -> {
                servicioSel[0] = serv;
                for (int j = 0; j < layoutServ.getChildCount(); j++) {
                    TextView c = (TextView) layoutServ.getChildAt(j);
                    boolean sel = c.getText().toString().equals(serv);
                    c.setBackground(getDrawable(sel
                            ? R.drawable.shape_filter_active : R.drawable.shape_filter_inactive));
                    c.setTextColor(sel ? Color.WHITE : Color.parseColor("#6B7FA3"));
                }
            });
            layoutServ.addView(chip);
        }

        // ── Selectores de fecha y hora ────────────────────────────
        view.findViewById(R.id.cardFecha).setOnClickListener(v ->
                showCustomDatePicker(tvFecha.getText().toString(), result -> {
                    fechaSelStr[0] = result;
                    tvFecha.setText("📅 " + result);
                }));
        view.findViewById(R.id.cardHora).setOnClickListener(v ->
                showCustomTimePicker(tvHora.getText().toString(), result -> {
                    horaSelStr[0] = result;
                    tvHora.setText("🕐 " + result);
                }));

        // ── Guardar ───────────────────────────────────────────────
        btnGuardarCard.setOnClickListener(v -> {
            // El nombre del cliente es SIEMPRE lo que está en el EditText
            String nombre = etCliente.getText().toString().trim();
            if (nombre.isEmpty()) { etCliente.setError("Campo obligatorio"); return; }

            String precioStr = etPrecio.getText().toString().trim();
            double precioNum = 0;
            try { precioNum = Double.parseDouble(precioStr.replace("€","").replace(",",".")); }
            catch (Exception ignored) {}

            String[] partes = fechaSelStr[0].split("/");
            int dia  = Integer.parseInt(partes[0]);
            int mes  = Integer.parseInt(partes[1]);
            int anio = Integer.parseInt(partes[2]);
            String fechaBD = String.format("%04d-%02d-%02d", anio, mes, dia);

            final double precioFinal  = precioNum;
            final String nombreFinal  = nombre;
            final String servicioFinal= servicioSel[0];
            final String horaFinal    = horaSelStr[0];
            final String notasFinal   = etNotas.getText().toString().trim();
            // clienteIdSel[0] puede ser null si escribió el nombre a mano
            final String clienteId    = clienteIdSel[0];

            // ── Validar hora duplicada (solo Masaje, citas no canceladas) ──
            if ("Masaje".equals(servicioFinal)) {
                for (Cita c : todasLasCitas) {
                    if ("cancelada".equals(c.estado)) continue;
                    if (citaEditar != null && citaEditar.id != null
                            && citaEditar.id.equals(c.id)) continue;
                    if (c.fecha.equals(fechaSelStr[0]) && c.hora.equals(horaFinal)) {
                        Toast.makeText(AgendaActivity.this,
                                "⚠️ Ya hay una cita de masaje a las " + horaFinal + " ese día",
                                Toast.LENGTH_LONG).show();
                        return;
                    }
                }
            }

            btnGuardarCard.setEnabled(false);
            btnGuardar.setText("Guardando...");

            if (citaEditar != null && citaEditar.id != null) {
                // EDITAR
                Map<String, Object> body = new HashMap<>();
                body.put("cliente_nombre",  nombreFinal);
                body.put("servicio_nombre", servicioFinal);
                body.put("fecha",           fechaBD);
                body.put("hora",            horaFinal);
                body.put("precio",          precioFinal);
                body.put("notas",           notasFinal);
                if (clienteId != null) body.put("cliente_id", clienteId);

                SupabaseRepository.get().actualizarCita(citaEditar.id, body,
                        new SupabaseRepository.Callback<Void>() {
                            @Override public void onSuccess(Void data) {
                                runOnUiThread(() -> {
                                    citaEditar.cliente  = nombreFinal;
                                    citaEditar.servicio = servicioFinal;
                                    citaEditar.fecha    = fechaSelStr[0];
                                    citaEditar.fechaBD  = fechaBD;
                                    citaEditar.hora     = horaFinal;
                                    citaEditar.precio   = precioStr.isEmpty() ? "0€" : precioStr + "€";
                                    citaEditar.notas    = notasFinal;
                                    citaEditar.diaMes   = dia;
                                    citaEditar.mes      = mes;
                                    citaEditar.anio     = anio;
                                    sheet.dismiss();
                                    ocultarTeclado();
                                    renderVista();
                                    Toast.makeText(AgendaActivity.this,
                                            "✅ Cita actualizada", Toast.LENGTH_SHORT).show();
                                });
                            }
                            @Override public void onError(String e) {
                                runOnUiThread(() -> {
                                    btnGuardarCard.setEnabled(true);
                                    btnGuardar.setText("Guardar");
                                    Toast.makeText(AgendaActivity.this,
                                            "Error: " + e, Toast.LENGTH_SHORT).show();
                                });
                            }
                        });
            } else {
                // CREAR
                SupabaseRepository.get().crearCita(
                        clienteId, nombreFinal, null, servicioFinal,
                        fechaBD, horaFinal, precioFinal, notasFinal,
                        new SupabaseRepository.Callback<CitaModel>() {
                            @Override public void onSuccess(CitaModel model) {
                                runOnUiThread(() -> {
                                    // Asegurar campos aunque el dummy venga vacío
                                    if (model.clienteNombre == null || model.clienteNombre.isEmpty())
                                        model.clienteNombre = nombreFinal;
                                    if (model.servicioNombre == null || model.servicioNombre.isEmpty())
                                        model.servicioNombre = servicioFinal;
                                    if (model.fecha == null || model.fecha.isEmpty())
                                        model.fecha = fechaBD;
                                    if (model.hora == null || model.hora.isEmpty())
                                        model.hora = horaFinal + ":00";
                                    if (model.estado == null) model.estado = "pendiente";
                                    // precio ya asignado por Supabase o se recargará en onResume
                                    todasLasCitas.add(0, new Cita(model));
                                    sheet.dismiss();
                                    ocultarTeclado();
                                    renderVista();
                                    Toast.makeText(AgendaActivity.this,
                                            "✅ Cita añadida", Toast.LENGTH_SHORT).show();
                                });
                            }
                            @Override public void onError(String e) {
                                runOnUiThread(() -> {
                                    btnGuardarCard.setEnabled(true);
                                    btnGuardar.setText("Guardar");
                                    Toast.makeText(AgendaActivity.this,
                                            "Error: " + e, Toast.LENGTH_SHORT).show();
                                });
                            }
                        });
            }
        });
        sheet.setOnShowListener(d -> {
            com.google.android.material.bottomsheet.BottomSheetBehavior<?> b =
                    com.google.android.material.bottomsheet.BottomSheetBehavior.from(
                            (android.view.View) sheet.findViewById(R.id.btnGuardarCita).getParent().getParent());
            b.setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED);
            b.setSkipCollapsed(true);
        });
        sheet.show();
    }

    private void showFiltroServicio() {
        BottomSheetDialog sheet = new BottomSheetDialog(
                this, com.google.android.material.R.style.Theme_Material3_Light_BottomSheetDialog);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackground(getDrawable(R.drawable.shape_sheet_bg));
        layout.setPadding(dpToPx(16), dpToPx(20), dpToPx(16), dpToPx(30));

        TextView titulo = new TextView(this);
        titulo.setText("Filtrar por servicio");
        titulo.setTextSize(16f);
        titulo.setTextColor(Color.parseColor("#0D1B3E"));
        titulo.setTypeface(getResources().getFont(R.font.outfit_bold));
        titulo.setPadding(dpToPx(4), 0, 0, dpToPx(16));
        layout.addView(titulo);

        List<String> opciones = new ArrayList<>();
        opciones.add("Todos");
        opciones.addAll(Arrays.asList(SERVICIOS));

        for (String op : opciones) {
            TextView item = new TextView(this);
            item.setText((op.equals(filtroServicio) ? "✓  " : "     ") + op);
            item.setTextSize(13f);
            item.setTextColor(op.equals(filtroServicio)
                    ? Color.parseColor("#0A66FF") : Color.parseColor("#0D1B3E"));
            item.setTypeface(getResources().getFont(
                    op.equals(filtroServicio) ? R.font.outfit_bold : R.font.outfit_regular));
            item.setPadding(dpToPx(8), dpToPx(14), dpToPx(8), dpToPx(14));
            item.setOnClickListener(v -> {
                filtroServicio = op;
                sheet.dismiss();
                renderVista();
            });
            layout.addView(item);
        }
        sheet.setContentView(layout);
        sheet.show();
    }


    private void mostrarDatePicker() {
        String fechaActual = String.format("%02d/%02d/%04d",
                fechaSeleccionada.get(Calendar.DAY_OF_MONTH),
                fechaSeleccionada.get(Calendar.MONTH) + 1,
                fechaSeleccionada.get(Calendar.YEAR));
        showCustomDatePicker(fechaActual, result -> {
            try {
                String[] p = result.split("/");
                fechaSeleccionada.set(Integer.parseInt(p[2]),
                        Integer.parseInt(p[1]) - 1, Integer.parseInt(p[0]));
                cargarCitas();
            } catch (Exception ignored) {}
        });
    }

    private void showCustomTimePicker(String horaActual, KauCallback onResult) {
        int initH = 10, initM = 0;
        try {
            StringBuilder sb = new StringBuilder();
            for (char ch : horaActual.toCharArray())
                if ((ch >= '0' && ch <= '9') || ch == ':') sb.append(ch);
            String[] p = sb.toString().split(":");
            if (p.length >= 2) {
                initH = Math.max(0, Math.min(23, Integer.parseInt(p[0])));
                initM = Math.max(0, Math.min(59, Integer.parseInt(p[1])));
            }
        } catch (Exception ignored) {}

        BottomSheetDialog sheet = new BottomSheetDialog(
                this, com.google.android.material.R.style.Theme_Material3_Light_BottomSheetDialog);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.WHITE);
        root.setPadding(dpToPx(24), dpToPx(16), dpToPx(24), dpToPx(36));

        // Handle
        LinearLayout hw = new LinearLayout(this);
        hw.setGravity(Gravity.CENTER_HORIZONTAL);
        LinearLayout.LayoutParams hwP = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        hwP.bottomMargin = dpToPx(20);
        hw.setLayoutParams(hwP);
        View handle = new View(this);
        android.graphics.drawable.GradientDrawable hbg = new android.graphics.drawable.GradientDrawable();
        hbg.setColor(Color.parseColor("#DDE6FF"));
        hbg.setCornerRadius(dpToPx(4));
        handle.setBackground(hbg);
        handle.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(40), dpToPx(4)));
        hw.addView(handle);
        root.addView(hw);

        TextView tvTitle = new TextView(this);
        tvTitle.setText("Seleccionar hora");
        tvTitle.setTextSize(18f);
        tvTitle.setTextColor(Color.parseColor("#0D1B3E"));
        tvTitle.setTypeface(getResources().getFont(R.font.outfit_bold));
        tvTitle.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams titleP = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        titleP.bottomMargin = dpToPx(24);
        tvTitle.setLayoutParams(titleP);
        root.addView(tvTitle);

        final int[] selH = {initH}, selM = {initM};
        TextView tvDisplay = new TextView(this);
        tvDisplay.setText(String.format("%02d:%02d", selH[0], selM[0]));
        tvDisplay.setTextSize(56f);
        tvDisplay.setTextColor(Color.parseColor("#0A66FF"));
        tvDisplay.setTypeface(getResources().getFont(R.font.outfit_bold));
        tvDisplay.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams dispP = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        dispP.bottomMargin = dpToPx(32);
        tvDisplay.setLayoutParams(dispP);
        root.addView(tvDisplay);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams rowP = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rowP.bottomMargin = dpToPx(32);
        row.setLayoutParams(rowP);

        // Horas
        LinearLayout horaBlock = new LinearLayout(this);
        horaBlock.setOrientation(LinearLayout.VERTICAL);
        horaBlock.setGravity(Gravity.CENTER);
        horaBlock.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        TextView lblH = new TextView(this);
        lblH.setText("Horas");
        lblH.setTextSize(11f);
        lblH.setTextColor(Color.parseColor("#6B7FA3"));
        lblH.setTypeface(getResources().getFont(R.font.outfit_bold));
        lblH.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams lblHP = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lblHP.bottomMargin = dpToPx(10);
        lblH.setLayoutParams(lblHP);
        horaBlock.addView(lblH);
        android.widget.NumberPicker npH = new android.widget.NumberPicker(this);
        npH.setMinValue(0); npH.setMaxValue(23); npH.setValue(selH[0]);
        npH.setWrapSelectorWheel(true);
        String[] horasVals = new String[24];
        for (int i = 0; i < 24; i++) horasVals[i] = String.format("%02d", i);
        npH.setDisplayedValues(horasVals);
        npH.setOnValueChangedListener((p, o, n) -> { selH[0] = n; tvDisplay.setText(String.format("%02d:%02d", selH[0], selM[0])); });
        horaBlock.addView(npH);
        row.addView(horaBlock);

        TextView sep = new TextView(this);
        sep.setText(":");
        sep.setTextSize(40f);
        sep.setTextColor(Color.parseColor("#0A66FF"));
        sep.setTypeface(getResources().getFont(R.font.outfit_bold));
        sep.setGravity(Gravity.CENTER);
        sep.setPadding(dpToPx(8), dpToPx(24), dpToPx(8), 0);
        row.addView(sep);

        // Minutos
        LinearLayout minBlock = new LinearLayout(this);
        minBlock.setOrientation(LinearLayout.VERTICAL);
        minBlock.setGravity(Gravity.CENTER);
        minBlock.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        TextView lblM = new TextView(this);
        lblM.setText("Minutos");
        lblM.setTextSize(11f);
        lblM.setTextColor(Color.parseColor("#6B7FA3"));
        lblM.setTypeface(getResources().getFont(R.font.outfit_bold));
        lblM.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams lblMP = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lblMP.bottomMargin = dpToPx(10);
        lblM.setLayoutParams(lblMP);
        minBlock.addView(lblM);
        android.widget.NumberPicker npM = new android.widget.NumberPicker(this);
        npM.setMinValue(0); npM.setMaxValue(11); npM.setValue(selM[0] / 5);
        npM.setWrapSelectorWheel(true);
        String[] minVals = new String[12];
        for (int i = 0; i < 12; i++) minVals[i] = String.format("%02d", i * 5);
        npM.setDisplayedValues(minVals);
        npM.setOnValueChangedListener((p, o, n) -> { selM[0] = n * 5; tvDisplay.setText(String.format("%02d:%02d", selH[0], selM[0])); });
        minBlock.addView(npM);
        row.addView(minBlock);
        root.addView(row);

        TextView btnOk = new TextView(this);
        btnOk.setText("Confirmar");
        btnOk.setTextSize(15f);
        btnOk.setTextColor(Color.WHITE);
        btnOk.setTypeface(getResources().getFont(R.font.outfit_bold));
        btnOk.setGravity(Gravity.CENTER);
        btnOk.setPadding(0, dpToPx(16), 0, dpToPx(16));
        android.graphics.drawable.GradientDrawable btnBg = new android.graphics.drawable.GradientDrawable();
        btnBg.setColor(Color.parseColor("#0A66FF"));
        btnBg.setCornerRadius(dpToPx(16));
        btnOk.setBackground(btnBg);
        btnOk.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        btnOk.setOnClickListener(v -> {
            npH.clearFocus(); npM.clearFocus();
            sheet.dismiss();
            onResult.call(String.format("%02d:%02d", selH[0], selM[0]));
        });
        root.addView(btnOk);
        sheet.setContentView(root);
        sheet.setOnShowListener(d -> {
            com.google.android.material.bottomsheet.BottomSheetBehavior<?> beh =
                    com.google.android.material.bottomsheet.BottomSheetBehavior.from(
                            (android.view.View) root.getParent());
            beh.setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED);
            beh.setSkipCollapsed(true);
        });
        sheet.show();
    }

    private void showCustomDatePicker(String fechaActual, KauCallback onResult) {
        BottomSheetDialog sheet = new BottomSheetDialog(
                this, com.google.android.material.R.style.Theme_Material3_Light_BottomSheetDialog);
        Calendar cal = Calendar.getInstance();
        try {
            String clean = fechaActual.replaceAll("[^0-9/]", "").trim();
            String[] parts = clean.split("/");
            cal.set(Integer.parseInt(parts[2]), Integer.parseInt(parts[1]) - 1, Integer.parseInt(parts[0]));
        } catch (Exception ignored) {}

        final int[] selDay = {cal.get(Calendar.DAY_OF_MONTH)};
        final int[] selMonth = {cal.get(Calendar.MONTH)};
        final int[] selYear  = {cal.get(Calendar.YEAR)};

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.WHITE);
        root.setPadding(dpToPx(20), dpToPx(16), dpToPx(20), dpToPx(32));

        View handle = new View(this);
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(dpToPx(40), dpToPx(4));
        hp.gravity = Gravity.CENTER_HORIZONTAL;
        hp.bottomMargin = dpToPx(20);
        android.graphics.drawable.GradientDrawable hbg = new android.graphics.drawable.GradientDrawable();
        hbg.setColor(Color.parseColor("#DDE6FF"));
        hbg.setCornerRadius(dpToPx(4));
        handle.setBackground(hbg);
        handle.setLayoutParams(hp);
        root.addView(handle);

        TextView tvTitle = new TextView(this);
        tvTitle.setText("¿Qué día?");
        tvTitle.setTextSize(18f);
        tvTitle.setTextColor(Color.parseColor("#0D1B3E"));
        tvTitle.setTypeface(getResources().getFont(R.font.outfit_bold));
        tvTitle.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams titleP = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        titleP.bottomMargin = dpToPx(20);
        tvTitle.setLayoutParams(titleP);
        root.addView(tvTitle);

        LinearLayout calContainer = new LinearLayout(this);
        calContainer.setOrientation(LinearLayout.VERTICAL);
        root.addView(calContainer);

        TextView btnOk = new TextView(this);
        btnOk.setText("Confirmar");
        btnOk.setTextSize(15f);
        btnOk.setTextColor(Color.WHITE);
        btnOk.setTypeface(getResources().getFont(R.font.outfit_bold));
        btnOk.setGravity(Gravity.CENTER);
        btnOk.setPadding(0, dpToPx(16), 0, dpToPx(16));
        android.graphics.drawable.GradientDrawable btnBg = new android.graphics.drawable.GradientDrawable();
        btnBg.setColor(Color.parseColor("#0A66FF"));
        btnBg.setCornerRadius(dpToPx(16));
        btnOk.setBackground(btnBg);
        LinearLayout.LayoutParams bop = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        bop.topMargin = dpToPx(20);
        btnOk.setLayoutParams(bop);
        btnOk.setOnClickListener(v -> {
            sheet.dismiss();
            onResult.call(String.format("%02d/%02d/%04d", selDay[0], selMonth[0] + 1, selYear[0]));
        });

        buildCalendarView(calContainer, selDay, selMonth, selYear);
        root.addView(btnOk);
        sheet.setContentView(root);
        sheet.show();
    }

    private static final String[] MESES    = {"Enero","Febrero","Marzo","Abril","Mayo","Junio",
            "Julio","Agosto","Septiembre","Octubre","Noviembre","Diciembre"};
    private static final String[] DIAS_SEM = {"L","M","X","J","V","S","D"};

    private void buildCalendarView(LinearLayout container, int[] selDay, int[] selMonth, int[] selYear) {
        container.removeAllViews();
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams hdrP = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        hdrP.bottomMargin = dpToPx(16);
        header.setLayoutParams(hdrP);

        TextView btnPrev = new TextView(this);
        btnPrev.setText("‹"); btnPrev.setTextSize(22f);
        btnPrev.setTextColor(Color.parseColor("#0A66FF"));
        btnPrev.setTypeface(getResources().getFont(R.font.outfit_bold));
        btnPrev.setGravity(Gravity.CENTER);
        btnPrev.setPadding(dpToPx(14), dpToPx(8), dpToPx(14), dpToPx(8));
        android.graphics.drawable.GradientDrawable prevBg = new android.graphics.drawable.GradientDrawable();
        prevBg.setColor(Color.parseColor("#F0F5FF")); prevBg.setCornerRadius(dpToPx(12));
        btnPrev.setBackground(prevBg);
        header.addView(btnPrev);

        TextView tvMesAnio = new TextView(this);
        tvMesAnio.setText(MESES[selMonth[0]] + " " + selYear[0]);
        tvMesAnio.setTextSize(16f); tvMesAnio.setTextColor(Color.parseColor("#0D1B3E"));
        tvMesAnio.setTypeface(getResources().getFont(R.font.outfit_bold));
        tvMesAnio.setGravity(Gravity.CENTER);
        tvMesAnio.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        header.addView(tvMesAnio);

        TextView btnNext = new TextView(this);
        btnNext.setText("›"); btnNext.setTextSize(22f);
        btnNext.setTextColor(Color.parseColor("#0A66FF"));
        btnNext.setTypeface(getResources().getFont(R.font.outfit_bold));
        btnNext.setGravity(Gravity.CENTER);
        btnNext.setPadding(dpToPx(14), dpToPx(8), dpToPx(14), dpToPx(8));
        android.graphics.drawable.GradientDrawable nextBg = new android.graphics.drawable.GradientDrawable();
        nextBg.setColor(Color.parseColor("#F0F5FF")); nextBg.setCornerRadius(dpToPx(12));
        btnNext.setBackground(nextBg);
        header.addView(btnNext);
        container.addView(header);

        LinearLayout semRow = new LinearLayout(this);
        semRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams srP = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        srP.bottomMargin = dpToPx(8); semRow.setLayoutParams(srP);
        for (String d : DIAS_SEM) {
            TextView tv = new TextView(this);
            tv.setText(d); tv.setTextSize(11f);
            tv.setTextColor(Color.parseColor("#6B7FA3"));
            tv.setTypeface(getResources().getFont(R.font.outfit_bold));
            tv.setGravity(Gravity.CENTER);
            tv.setLayoutParams(new LinearLayout.LayoutParams(0, dpToPx(32), 1f));
            semRow.addView(tv);
        }
        container.addView(semRow);

        Calendar cal = Calendar.getInstance();
        cal.set(selYear[0], selMonth[0], 1);
        int primerDia = cal.get(Calendar.DAY_OF_WEEK);
        int offset    = (primerDia == Calendar.SUNDAY) ? 6 : primerDia - 2;
        int diasEnMes = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        Calendar hoy  = Calendar.getInstance();

        int filas = (int) Math.ceil((offset + diasEnMes) / 7.0);
        for (int f = 0; f < filas; f++) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            rp.bottomMargin = dpToPx(4); row.setLayoutParams(rp);
            for (int c = 0; c < 7; c++) {
                int pos = f * 7 + c;
                int dia = pos - offset + 1;
                if (pos < offset || dia > diasEnMes) {
                    View empty = new View(this);
                    empty.setLayoutParams(new LinearLayout.LayoutParams(0, dpToPx(40), 1f));
                    row.addView(empty);
                    continue;
                }
                final int diaVal = dia;
                boolean esSel = (diaVal == selDay[0]);
                boolean esHoy = (diaVal == hoy.get(Calendar.DAY_OF_MONTH)
                        && selMonth[0] == hoy.get(Calendar.MONTH) && selYear[0] == hoy.get(Calendar.YEAR));
                LinearLayout cell = new LinearLayout(this);
                cell.setGravity(Gravity.CENTER);
                cell.setLayoutParams(new LinearLayout.LayoutParams(0, dpToPx(40), 1f));
                android.graphics.drawable.GradientDrawable cellBg = new android.graphics.drawable.GradientDrawable();
                cellBg.setShape(android.graphics.drawable.GradientDrawable.OVAL);
                cellBg.setColor(esSel ? Color.parseColor("#0A66FF")
                        : esHoy ? Color.parseColor("#E8F0FF") : Color.TRANSPARENT);
                cell.setBackground(cellBg);
                TextView tvDia = new TextView(this);
                tvDia.setText(String.valueOf(diaVal)); tvDia.setTextSize(13f);
                tvDia.setTextColor(esSel ? Color.WHITE
                        : esHoy ? Color.parseColor("#0A66FF") : Color.parseColor("#0D1B3E"));
                tvDia.setTypeface(getResources().getFont(
                        (esSel || esHoy) ? R.font.outfit_bold : R.font.outfit_regular));
                tvDia.setGravity(Gravity.CENTER);
                cell.addView(tvDia);
                cell.setOnClickListener(v -> { selDay[0] = diaVal; buildCalendarView(container, selDay, selMonth, selYear); });
                row.addView(cell);
            }
            container.addView(row);
        }

        btnPrev.setOnClickListener(v -> {
            selMonth[0]--; if (selMonth[0] < 0) { selMonth[0] = 11; selYear[0]--; }
            selDay[0] = 1; buildCalendarView(container, selDay, selMonth, selYear);
        });
        btnNext.setOnClickListener(v -> {
            selMonth[0]++; if (selMonth[0] > 11) { selMonth[0] = 0; selYear[0]++; }
            selDay[0] = 1; buildCalendarView(container, selDay, selMonth, selYear);
        });
    }

    private void setupBotones() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnAddCita).setOnClickListener(v -> showNuevaCitaSheet(null));
    }

    private void setupBottomNav() {
        NavHelper.setup(this, "agenda");
    }



    /** Necesitamos este método en el repo — añade el wrapper aquí */
    // SupabaseRepository necesita actualizarCita con Map → añadimos el método:
    // (ver nota al final del archivo)

    private int getEstadoColor(String estado) {
        switch (estado) {
            case "confirmada": return Color.parseColor("#12B76A");
            case "cancelada":  return Color.parseColor("#EF4444");
            case "cobrada":    return Color.parseColor("#0A66FF");
            default:           return Color.parseColor("#F59E0B");
        }
    }

    /** yyyy-MM-dd */
    private String fmt(Calendar cal) {
        return String.format(Locale.getDefault(), "%04d-%02d-%02d",
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.DAY_OF_MONTH));
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


    private void mostrarSugerencias(String texto, LinearLayout layoutSug,
                                    EditText etCliente, String[] clienteIdSel,
                                    boolean[] seleccionando) {
        SupabaseRepository.get().getClientes(null,
                new SupabaseRepository.Callback<List<ClienteModel>>() {
                    @Override public void onSuccess(List<ClienteModel> data) {
                        runOnUiThread(() -> {
                            layoutSug.removeAllViews();
                            String q = texto.toLowerCase().trim();

                            List<ClienteModel> matches = new ArrayList<>();
                            for (ClienteModel c : data) {
                                if (matches.size() >= 6) break;
                                String nom = c.nombre != null ? c.nombre.trim() : "";
                                String ape = c.apellidos != null ? c.apellidos.trim() : "";
                                String full = ape.isEmpty() ? nom : nom + " " + ape;
                                String tel  = c.telefono != null ? c.telefono.trim() : "";
                                if (full.toLowerCase().contains(q) || tel.contains(q))
                                    matches.add(c);
                            }

                            if (matches.isEmpty()) {
                                layoutSug.setVisibility(View.GONE);
                                return;
                            }

                            // Contenedor con borde y sombra
                            CardView contenedor = new CardView(AgendaActivity.this);
                            contenedor.setLayoutParams(new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT));
                            contenedor.setRadius(dpToPx(14));
                            contenedor.setCardElevation(dpToPx(6));
                            contenedor.setCardBackgroundColor(Color.WHITE);

                            LinearLayout lista = new LinearLayout(AgendaActivity.this);
                            lista.setOrientation(LinearLayout.VERTICAL);
                            contenedor.addView(lista);

                            for (int idx = 0; idx < matches.size(); idx++) {
                                ClienteModel c = matches.get(idx);
                                String nom  = c.nombre   != null ? c.nombre.trim()   : "";
                                String ape  = c.apellidos!= null ? c.apellidos.trim(): "";
                                String full = ape.isEmpty() ? nom : nom + " " + ape;
                                String tel  = c.telefono != null ? c.telefono.trim() : "";
                                if (full.isEmpty()) full = "Cliente";
                                String inicial = String.valueOf(full.charAt(0)).toUpperCase();

                                // Fila
                                LinearLayout fila = new LinearLayout(AgendaActivity.this);
                                fila.setOrientation(LinearLayout.HORIZONTAL);
                                fila.setGravity(Gravity.CENTER_VERTICAL);
                                fila.setPadding(dpToPx(14), dpToPx(13), dpToPx(14), dpToPx(13));
                                fila.setClickable(true);
                                fila.setFocusable(true);

                                // Avatar
                                CardView av = new CardView(AgendaActivity.this);
                                LinearLayout.LayoutParams avP = new LinearLayout.LayoutParams(dpToPx(36), dpToPx(36));
                                avP.setMarginEnd(dpToPx(10));
                                av.setLayoutParams(avP);
                                av.setRadius(dpToPx(10));
                                av.setCardElevation(0);
                                av.setCardBackgroundColor(Color.parseColor("#0A66FF"));
                                TextView tvI = new TextView(AgendaActivity.this);
                                tvI.setText(inicial);
                                tvI.setTextSize(14f);
                                tvI.setTextColor(Color.WHITE);
                                tvI.setTypeface(getResources().getFont(R.font.outfit_bold));
                                tvI.setGravity(Gravity.CENTER);
                                tvI.setLayoutParams(new LinearLayout.LayoutParams(
                                        LinearLayout.LayoutParams.MATCH_PARENT,
                                        LinearLayout.LayoutParams.MATCH_PARENT));
                                av.addView(tvI);
                                fila.addView(av);

                                // Info
                                LinearLayout info = new LinearLayout(AgendaActivity.this);
                                info.setOrientation(LinearLayout.VERTICAL);
                                info.setLayoutParams(new LinearLayout.LayoutParams(
                                        0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
                                TextView tvN = new TextView(AgendaActivity.this);
                                tvN.setText(full);
                                tvN.setTextSize(13f);
                                tvN.setTextColor(Color.parseColor("#0D1B3E"));
                                tvN.setTypeface(getResources().getFont(R.font.outfit_bold));
                                info.addView(tvN);
                                if (!tel.isEmpty()) {
                                    TextView tvT = new TextView(AgendaActivity.this);
                                    tvT.setText(tel);
                                    tvT.setTextSize(11f);
                                    tvT.setTextColor(Color.parseColor("#6B7FA3"));
                                    tvT.setTypeface(getResources().getFont(R.font.outfit_regular));
                                    info.addView(tvT);
                                }
                                fila.addView(info);

                                // Flecha
                                TextView arrow = new TextView(AgendaActivity.this);
                                arrow.setText("›");
                                arrow.setTextSize(20f);
                                arrow.setTextColor(Color.parseColor("#0A66FF"));
                                arrow.setPadding(dpToPx(8), 0, 0, 0);
                                fila.addView(arrow);

                                // ── CLICK: asigna nombre e id, cierra la lista ──
                                final String idFinal   = c.id;
                                final String nomFinal  = full;
                                fila.setOnClickListener(vv -> {
                                    // Marcar que el próximo cambio de texto es programático
                                    seleccionando[0]  = true;
                                    clienteIdSel[0]   = idFinal;
                                    etCliente.setText(nomFinal);
                                    etCliente.setSelection(nomFinal.length());
                                    layoutSug.setVisibility(View.GONE);
                                    layoutSug.removeAllViews();
                                    ocultarTeclado();
                                });

                                lista.addView(fila);

                                // Separador
                                if (idx < matches.size() - 1) {
                                    View sep = new View(AgendaActivity.this);
                                    LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(
                                            LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(1));
                                    sp.setMargins(dpToPx(14), 0, dpToPx(14), 0);
                                    sep.setLayoutParams(sp);
                                    sep.setBackgroundColor(Color.parseColor("#EEF2FF"));
                                    lista.addView(sep);
                                }
                            }

                            layoutSug.addView(contenedor);
                            layoutSug.setVisibility(View.VISIBLE);
                        });
                    }
                    @Override public void onError(String e) { /* silencioso */ }
                });
    }

}

/*
Evitar duplicidad en el codigo con cobros.


 */