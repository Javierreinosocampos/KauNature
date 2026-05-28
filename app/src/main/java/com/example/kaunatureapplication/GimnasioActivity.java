package com.example.kaunatureapplication;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

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

    // ── Modelo local ─────────────────────────────────────────────────
    static class FranjaLocal {
        String       id;
        String       hora;
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
            if (llena())       return RED;
            if (pct() > 0.65f) return YELLOW;
            return GREEN;
        }
        String etiquetaEstado() {
            if (llena())       return "LLENO";
            if (pct() > 0.65f) return "CASI";
            return "OK";
        }
    }

    // ── Estado de datos ──────────────────────────────────────────────
    final Map<String, List<FranjaLocal>> franjasPorFecha = new HashMap<>();
    final List<String> clientesPool = new ArrayList<>();

    // ── Estado UI ─────────────────────────────────────────────────────
    String  vista                  = "dia";
    String  selectedFecha          = "";
    String  clientePreseleccionado = null;
    int     semanaOffset   = 0;
    int     mesOffset      = 0;
    int     diaIdx         = 0;

    // ── Delegate de UI ────────────────────────────────────────────────
    GimnasioUI ui;

    // ── Views infladas desde XML ──────────────────────────────────────
    LinearLayout llContenido;

    // ════════════════════════════════════════════════════════════════
    //  onCreate
    // ════════════════════════════════════════════════════════════════
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Leer intent antes de inflar
        if (getIntent() != null) {
            clientePreseleccionado = getIntent().getStringExtra("CLIENTE_NOMBRE");
        }
        if (selectedFecha.isEmpty()) selectedFecha = GimnasioDateUtils.hoy();
        diaIdx = GimnasioDateUtils.diaSemanaIdx(selectedFecha);

        // ── Inflar layout XML (header, tabs, KPI, nav, fab) ───────────
        setContentView(R.layout.activity_gimnasio);

        // ── Status bar transparente ───────────────────────────────────
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        );
        getWindow().setStatusBarColor(Color.TRANSPARENT);

        // ── Enlazar contenedor dinámico ───────────────────────────────
        llContenido = findViewById(R.id.gimnasio_ll_contenido);

        // ── Crear delegate UI (solo gestiona partes dinámicas) ────────
        ui = new GimnasioUI(this);

        // ── Conectar listeners de los elementos XML estáticos ─────────
        bindStaticListeners();

        // ── Cargar datos ──────────────────────────────────────────────
        cargarTodo();
    }

    @Override
    protected void onResume() {
        super.onResume();
        ui.renderDias();
        ui.renderContenido();
    }

    // ════════════════════════════════════════════════════════════════
    //  Listeners de vistas XML estáticas
    // ════════════════════════════════════════════════════════════════
    private void bindStaticListeners() {
        // Back
        findViewById(R.id.gimnasio_btn_back).setOnClickListener(v -> finish());

        // Botón gestionar franjas
        findViewById(R.id.gimnasio_btn_fitness)
                .setOnClickListener(v -> ui.showGestionarFranjasSheet(selectedFecha));

        // FAB añadir
        findViewById(R.id.gimnasio_fab)
                .setOnClickListener(v -> ui.showGestionarFranjasSheet(selectedFecha));

        // Tabs vista
        TextView tabDia    = findViewById(R.id.gimnasio_tab_dia);
        TextView tabSemana = findViewById(R.id.gimnasio_tab_semana);
        TextView tabMes    = findViewById(R.id.gimnasio_tab_mes);

        tabDia.setOnClickListener(v -> {
            vista = "dia";
            ui.refreshTabsXml(tabDia, tabSemana, tabMes);
            ui.renderDias();
            ui.renderContenido();
        });
        tabSemana.setOnClickListener(v -> {
            vista = "semana";
            ui.refreshTabsXml(tabDia, tabSemana, tabMes);
            ui.renderDias();
            ui.renderContenido();
        });
        tabMes.setOnClickListener(v -> {
            vista = "mes";
            ui.refreshTabsXml(tabDia, tabSemana, tabMes);
            ui.renderDias();
            ui.renderContenido();
        });

        // Nav bar
        findViewById(R.id.gimnasio_nav_home).setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
            overridePendingTransition(0, 0);
        });
        findViewById(R.id.gimnasio_nav_clientes).setOnClickListener(v -> {
            startActivity(new Intent(this, ClientesActivity.class));
            overridePendingTransition(0, 0);
            finish();
        });
        // gimnasio_nav_gym → ya estamos aquí, no hace nada
        findViewById(R.id.gimnasio_nav_agenda).setOnClickListener(v -> {
            startActivity(new Intent(this, AgendaActivity.class));
            overridePendingTransition(0, 0);
            finish();
        });
        findViewById(R.id.gimnasio_nav_cobros).setOnClickListener(v -> {
            startActivity(new Intent(this, CobrosActivity.class));
            overridePendingTransition(0, 0);
            finish();
        });
    }

    // ════════════════════════════════════════════════════════════════
    //  CARGA DESDE SUPABASE
    // ════════════════════════════════════════════════════════════════

    void cargarTodo() {
        ui.mostrarCargando();

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

        SupabaseRepository.get().getFranjas(new SupabaseRepository.Callback<List<FranjaModel>>() {
            @Override public void onSuccess(List<FranjaModel> franjas) {
                cargarAsistenciaSemana(franjas, semanaOffset);
            }
            @Override public void onError(String e) {
                runOnUiThread(() -> {
                    Toast.makeText(GimnasioActivity.this,
                            "Error cargando franjas: " + e, Toast.LENGTH_LONG).show();
                    ui.renderDias();
                    ui.renderContenido();
                });
            }
        });
    }

    void cargarAsistenciaSemana(List<FranjaModel> todasFranjas, int semOffset) {
        String lunes = GimnasioDateUtils.lunesDeSemana(semOffset);

        for (int d = 0; d < 7; d++) {
            String fecha = GimnasioDateUtils.sumarDias(lunes, d);
            franjasPorFecha.put(fecha, new ArrayList<>());
        }

        final int[] pendientes = {7};

        for (int d = 0; d < 7; d++) {
            final String fecha = GimnasioDateUtils.sumarDias(lunes, d);
            final int diaSemBD = d + 1;

            List<FranjaModel> franjasDelDia = new ArrayList<>();
            for (FranjaModel fm : todasFranjas) {
                if (fm.diaSemana == diaSemBD) franjasDelDia.add(fm);
            }

            if (franjasDelDia.isEmpty()) {
                synchronized (this) { pendientes[0]--; }
                if (pendientes[0] <= 0) renderFinal();
                continue;
            }

            List<FranjaLocal> locales = new ArrayList<>();
            for (FranjaModel fm : franjasDelDia) locales.add(new FranjaLocal(fm));
            franjasPorFecha.put(fecha, locales);

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

    void renderFinal() {
        runOnUiThread(() -> {
            ui.renderDias();
            ui.renderContenido();
        });
    }

    /** Recarga franjas + asistencia para una fecha concreta */
    void recargarFecha(String fecha) {
        SupabaseRepository.get().getFranjas(new SupabaseRepository.Callback<List<FranjaModel>>() {
            @Override public void onSuccess(List<FranjaModel> franjas) {
                int diaSemBD = GimnasioDateUtils.diaSemanaIdx(fecha) + 1;
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

    /** Recarga la asistencia de la semana actual (semanaOffset) */
    void cargarAsistenciaSemanaAsync() {
        ui.mostrarCargando();
        SupabaseRepository.get().getFranjas(new SupabaseRepository.Callback<List<FranjaModel>>() {
            @Override public void onSuccess(List<FranjaModel> franjas) {
                cargarAsistenciaSemana(franjas, semanaOffset);
            }
            @Override public void onError(String e) { renderFinal(); }
        });
    }

    // ── Acceso a franjas del día ──────────────────────────────────────
    List<FranjaLocal> getFranjasDia(String fecha) {
        if (!franjasPorFecha.containsKey(fecha)) franjasPorFecha.put(fecha, new ArrayList<>());
        return franjasPorFecha.get(fecha);
    }

    // ── dp helper ─────────────────────────────────────────────────────
    int dp(int val) {
        return Math.round(val * getResources().getDisplayMetrics().density);
    }
}