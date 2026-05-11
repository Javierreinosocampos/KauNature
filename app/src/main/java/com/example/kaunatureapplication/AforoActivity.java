package com.example.kaunatureapplication;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
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
import java.util.List;

public class AforoActivity extends AppCompatActivity {

    // ── Días ────────────────────────────────────────────────────────
    private static final String[] DIAS       = {"Lunes","Martes","Miércoles","Jueves","Viernes","Sábado","Domingo"};
    private static final String[] DIAS_CORTO = {"L","M","X","J","V","S","D"};
    // dia_semana en BD: 1=Lun … 7=Dom
    private static final int[]    DIAS_BD    = {1, 2, 3, 4, 5, 6, 7};

    // ── Estado ──────────────────────────────────────────────────────
    private int diaSelIdx = 0;

    // Franjas cargadas de Supabase para el día seleccionado
    // Cada FranjaLocal agrupa FranjaModel + lista de asistentes
    private final List<FranjaLocal> franjasDelDia = new ArrayList<>();

    // ── Views ────────────────────────────────────────────────────────
    private LinearLayout layoutDias;
    private LinearLayout layoutFranjas;
    private TextView     tvDiaNombre;
    private TextView     tvTotalPersonas;

    // ── Modelo local ─────────────────────────────────────────────────
    static class FranjaLocal {
        FranjaModel         modelo;
        List<AsistenciaModel> asistentes = new ArrayList<>();

        FranjaLocal(FranjaModel m) { this.modelo = m; }

        int ocupacion() { return asistentes.size(); }
        boolean llena() { return asistentes.size() >= modelo.aforoMax; }
        float pct()     { return modelo.aforoMax > 0
                ? Math.min(1f, (float) asistentes.size() / modelo.aforoMax) : 0f; }

        int colorEstado() {
            if (llena())      return Color.parseColor("#EF4444");
            if (pct() > 0.6f) return Color.parseColor("#F59E0B");
            return Color.parseColor("#0A66FF");
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  onCreate
    // ════════════════════════════════════════════════════════════════
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ── Root ─────────────────────────────────────────────────────
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#F5F8FF"));
        root.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT));

        // ── Header ───────────────────────────────────────────────────
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setBackgroundColor(Color.WHITE);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dpToPx(16), dpToPx(52), dpToPx(16), dpToPx(16));
        header.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView btnBack = new TextView(this);
        btnBack.setText("←");
        btnBack.setTextSize(22f);
        btnBack.setTextColor(Color.parseColor("#0A66FF"));
        btnBack.setTypeface(Typeface.DEFAULT_BOLD);
        btnBack.setPadding(0, 0, dpToPx(12), 0);
        btnBack.setOnClickListener(v -> finish());
        header.addView(btnBack);

        LinearLayout titleCol = new LinearLayout(this);
        titleCol.setOrientation(LinearLayout.VERTICAL);
        titleCol.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView tvTitulo = new TextView(this);
        tvTitulo.setText("Aforo Gimnasio");
        tvTitulo.setTextSize(20f);
        tvTitulo.setTextColor(Color.parseColor("#0D1B3E"));
        tvTitulo.setTypeface(Typeface.DEFAULT_BOLD);
        titleCol.addView(tvTitulo);

        tvDiaNombre = new TextView(this);
        tvDiaNombre.setText(DIAS[diaSelIdx]);
        tvDiaNombre.setTextSize(12f);
        tvDiaNombre.setTextColor(Color.parseColor("#6B7FA3"));
        titleCol.addView(tvDiaNombre);
        header.addView(titleCol);

        tvTotalPersonas = new TextView(this);
        tvTotalPersonas.setText("0 hoy");
        tvTotalPersonas.setTextSize(12f);
        tvTotalPersonas.setTextColor(Color.WHITE);
        tvTotalPersonas.setTypeface(Typeface.DEFAULT_BOLD);
        tvTotalPersonas.setGravity(Gravity.CENTER);
        tvTotalPersonas.setPadding(dpToPx(12), dpToPx(6), dpToPx(12), dpToPx(6));
        android.graphics.drawable.GradientDrawable chipBg = new android.graphics.drawable.GradientDrawable();
        chipBg.setColor(Color.parseColor("#0A66FF"));
        chipBg.setCornerRadius(dpToPx(20));
        tvTotalPersonas.setBackground(chipBg);
        header.addView(tvTotalPersonas);
        root.addView(header);

        // ── Selector días ─────────────────────────────────────────────
        HorizontalScrollView diaHScroll = new HorizontalScrollView(this);
        diaHScroll.setBackgroundColor(Color.WHITE);
        diaHScroll.setHorizontalScrollBarEnabled(false);
        diaHScroll.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        layoutDias = new LinearLayout(this);
        layoutDias.setOrientation(LinearLayout.HORIZONTAL);
        layoutDias.setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12));
        layoutDias.setGravity(Gravity.CENTER_VERTICAL);
        diaHScroll.addView(layoutDias);
        root.addView(diaHScroll);

        View sep = new View(this);
        sep.setBackgroundColor(Color.parseColor("#EEF4FF"));
        sep.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(1)));
        root.addView(sep);

        // ── Franjas ───────────────────────────────────────────────────
        ScrollView sv = new ScrollView(this);
        sv.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        sv.setBackgroundColor(Color.parseColor("#F5F8FF"));

        layoutFranjas = new LinearLayout(this);
        layoutFranjas.setOrientation(LinearLayout.VERTICAL);
        layoutFranjas.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(100));
        sv.addView(layoutFranjas);
        root.addView(sv);

        root.addView(buildBottomNav());
        setContentView(root);

        // Carga inicial
        renderDias();
        cargarFranjasYAsistencia();
    }

    // ════════════════════════════════════════════════════════════════
    //  CARGA DESDE SUPABASE
    // ════════════════════════════════════════════════════════════════

    /**
     * 1. Pide al repo las franjas del día de semana seleccionado.
     * 2. Para cada franja pide la asistencia de HOY (fecha real).
     * 3. Al terminar todas las llamadas, renderiza.
     */
    private void cargarFranjasYAsistencia() {
        mostrarCargando();

        int diaSemana = DIAS_BD[diaSelIdx]; // 1=Lun … 7=Dom

        // Fecha de hoy en formato yyyy-MM-dd
        String fechaHoy = fechaHoy();

        SupabaseRepository.get().getFranjas(new SupabaseRepository.Callback<List<FranjaModel>>() {
            @Override
            public void onSuccess(List<FranjaModel> todasFranjas) {

                // Filtrar solo las del día seleccionado
                List<FranjaModel> franjasDia = new ArrayList<>();
                for (FranjaModel f : todasFranjas) {
                    if (f.diaSemana == diaSemana) franjasDia.add(f);
                }

                if (franjasDia.isEmpty()) {
                    runOnUiThread(() -> {
                        franjasDelDia.clear();
                        renderFranjas();
                        actualizarTotal();
                    });
                    return;
                }

                // Carga asistencia de cada franja (llamadas en paralelo, contamos con contador)
                final int[] pendientes = {franjasDia.size()};
                final List<FranjaLocal> resultado = new ArrayList<>();
                for (FranjaModel fm : franjasDia) resultado.add(new FranjaLocal(fm));

                for (int i = 0; i < resultado.size(); i++) {
                    final FranjaLocal fl = resultado.get(i);
                    SupabaseRepository.get().getAsistencia(fechaHoy, fl.modelo.id,
                            new SupabaseRepository.Callback<List<AsistenciaModel>>() {
                                @Override
                                public void onSuccess(List<AsistenciaModel> asistentes) {
                                    fl.asistentes.addAll(asistentes);
                                    decrementarYRender(pendientes, resultado);
                                }
                                @Override
                                public void onError(String e) {
                                    // Si falla una franja seguimos con las demás
                                    decrementarYRender(pendientes, resultado);
                                }
                            });
                }
            }

            @Override
            public void onError(String e) {
                runOnUiThread(() ->
                        Toast.makeText(AforoActivity.this,
                                "Error cargando franjas: " + e, Toast.LENGTH_LONG).show());
            }
        });
    }

    private synchronized void decrementarYRender(int[] pendientes, List<FranjaLocal> resultado) {
        pendientes[0]--;
        if (pendientes[0] <= 0) {
            runOnUiThread(() -> {
                franjasDelDia.clear();
                franjasDelDia.addAll(resultado);
                renderDias();
                renderFranjas();
                actualizarTotal();
            });
        }
    }

    private void mostrarCargando() {
        layoutFranjas.removeAllViews();
        TextView tv = new TextView(this);
        tv.setText("Cargando...");
        tv.setTextSize(14f);
        tv.setTextColor(Color.parseColor("#6B7FA3"));
        tv.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        p.topMargin = dpToPx(40);
        tv.setLayoutParams(p);
        layoutFranjas.addView(tv);
    }

    // ════════════════════════════════════════════════════════════════
    //  RENDER DÍAS (selector)
    // ════════════════════════════════════════════════════════════════
    private void renderDias() {
        layoutDias.removeAllViews();

        for (int i = 0; i < DIAS.length; i++) {
            final int idx = i;

            // Contar total personas del día en memoria
            int total = 0;
            if (i == diaSelIdx) {
                for (FranjaLocal fl : franjasDelDia) total += fl.ocupacion();
            }

            LinearLayout pill = new LinearLayout(this);
            pill.setOrientation(LinearLayout.VERTICAL);
            pill.setGravity(Gravity.CENTER);
            pill.setPadding(dpToPx(14), dpToPx(10), dpToPx(14), dpToPx(10));
            LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            pp.setMarginEnd(dpToPx(8));
            pill.setLayoutParams(pp);

            boolean activo = (i == diaSelIdx);
            android.graphics.drawable.GradientDrawable pbg = new android.graphics.drawable.GradientDrawable();
            pbg.setColor(activo ? Color.parseColor("#0A66FF") : Color.parseColor("#F0F5FF"));
            pbg.setCornerRadius(dpToPx(16));
            pill.setBackground(pbg);

            TextView tvDia = new TextView(this);
            tvDia.setText(DIAS_CORTO[i]);
            tvDia.setTextSize(13f);
            tvDia.setTextColor(activo ? Color.WHITE : Color.parseColor("#0D1B3E"));
            tvDia.setTypeface(Typeface.DEFAULT_BOLD);
            tvDia.setGravity(Gravity.CENTER);
            pill.addView(tvDia);

            if (activo && total > 0) {
                TextView tvCount = new TextView(this);
                tvCount.setText(String.valueOf(total));
                tvCount.setTextSize(10f);
                tvCount.setTextColor(Color.parseColor("#CCE0FF"));
                tvCount.setTypeface(Typeface.DEFAULT_BOLD);
                tvCount.setGravity(Gravity.CENTER);
                pill.addView(tvCount);
            }

            pill.setOnClickListener(v -> {
                diaSelIdx = idx;
                tvDiaNombre.setText(DIAS[diaSelIdx]);
                cargarFranjasYAsistencia(); // recarga para el nuevo día
            });
            layoutDias.addView(pill);
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  RENDER FRANJAS
    // ════════════════════════════════════════════════════════════════
    private void renderFranjas() {
        layoutFranjas.removeAllViews();
        String dia = DIAS[diaSelIdx];

        if (franjasDelDia.isEmpty()) {
            TextView tvVacio = new TextView(this);
            tvVacio.setText("Sin franjas configuradas para " + dia);
            tvVacio.setTextSize(14f);
            tvVacio.setTextColor(Color.parseColor("#6B7FA3"));
            tvVacio.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams vp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            vp.topMargin = dpToPx(40);
            tvVacio.setLayoutParams(vp);
            layoutFranjas.addView(tvVacio);
            return;
        }

        // Título sección
        TextView tvSec = new TextView(this);
        tvSec.setText("Franjas horarias · " + dia);
        tvSec.setTextSize(13f);
        tvSec.setTextColor(Color.parseColor("#6B7FA3"));
        tvSec.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams secP = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        secP.bottomMargin = dpToPx(16);
        tvSec.setLayoutParams(secP);
        layoutFranjas.addView(tvSec);

        for (FranjaLocal fl : franjasDelDia) {
            layoutFranjas.addView(buildFranjaCard(fl, dia));
        }
    }

    private View buildFranjaCard(FranjaLocal fl, String dia) {
        int count    = fl.ocupacion();
        boolean llena   = fl.llena();
        boolean ocupada = count > 0;
        String horaDisplay = fl.modelo.horaDisplay();

        CardView card = new CardView(this);
        LinearLayout.LayoutParams cardP = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cardP.bottomMargin = dpToPx(12);
        card.setLayoutParams(cardP);
        card.setRadius(dpToPx(18));
        card.setCardElevation(dpToPx(2));
        card.setCardBackgroundColor(Color.WHITE);

        LinearLayout inner = new LinearLayout(this);
        inner.setOrientation(LinearLayout.VERTICAL);
        inner.setPadding(dpToPx(18), dpToPx(16), dpToPx(18), dpToPx(16));

        // ── Fila: hora + barra + contador ────────────────────────────
        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams trP = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        trP.bottomMargin = dpToPx(12);
        topRow.setLayoutParams(trP);

        TextView tvHora = new TextView(this);
        tvHora.setText(horaDisplay);
        tvHora.setTextSize(17f);
        tvHora.setTextColor(Color.parseColor("#0D1B3E"));
        tvHora.setTypeface(Typeface.DEFAULT_BOLD);
        tvHora.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(56),
                LinearLayout.LayoutParams.WRAP_CONTENT));
        topRow.addView(tvHora);

        // Barra de progreso
        FrameLayout barraFrame = new FrameLayout(this);
        LinearLayout.LayoutParams bfP = new LinearLayout.LayoutParams(0, dpToPx(10), 1f);
        bfP.setMarginStart(dpToPx(12));
        bfP.setMarginEnd(dpToPx(12));
        barraFrame.setLayoutParams(bfP);

        View barraFondo = new View(this);
        barraFondo.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, dpToPx(10)));
        android.graphics.drawable.GradientDrawable fondoBg = new android.graphics.drawable.GradientDrawable();
        fondoBg.setColor(Color.parseColor("#F0F5FF"));
        fondoBg.setCornerRadius(dpToPx(8));
        barraFondo.setBackground(fondoBg);
        barraFrame.addView(barraFondo);

        if (count > 0) {
            float pct = fl.pct();
            View barraRelleno = new View(this);
            FrameLayout.LayoutParams rrP = new FrameLayout.LayoutParams(0, dpToPx(10));
            barraRelleno.setLayoutParams(rrP);
            android.graphics.drawable.GradientDrawable rellenoBg = new android.graphics.drawable.GradientDrawable();
            rellenoBg.setColor(fl.colorEstado());
            rellenoBg.setCornerRadius(dpToPx(8));
            barraRelleno.setBackground(rellenoBg);
            barraFrame.addView(barraRelleno);
            barraFrame.post(() -> {
                FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) barraRelleno.getLayoutParams();
                lp.width = (int) (barraFrame.getWidth() * pct);
                barraRelleno.setLayoutParams(lp);
            });
        }
        topRow.addView(barraFrame);

        TextView tvCount = new TextView(this);
        tvCount.setText(count + "/" + fl.modelo.aforoMax);
        tvCount.setTextSize(13f);
        tvCount.setTextColor(fl.colorEstado());
        tvCount.setTypeface(Typeface.DEFAULT_BOLD);
        topRow.addView(tvCount);
        inner.addView(topRow);

        // ── Chips de personas ─────────────────────────────────────────
        if (ocupada) {
            HorizontalScrollView chipsScroll = new HorizontalScrollView(this);
            chipsScroll.setHorizontalScrollBarEnabled(false);
            LinearLayout chipsRow = new LinearLayout(this);
            chipsRow.setOrientation(LinearLayout.HORIZONTAL);

            for (AsistenciaModel a : fl.asistentes) {
                TextView chip = new TextView(this);
                chip.setText(a.clienteNombre.split(" ")[0]);
                chip.setTextSize(11f);
                chip.setTextColor(Color.parseColor("#0A66FF"));
                chip.setTypeface(Typeface.DEFAULT_BOLD);
                chip.setPadding(dpToPx(10), dpToPx(5), dpToPx(10), dpToPx(5));
                android.graphics.drawable.GradientDrawable chipBg2 = new android.graphics.drawable.GradientDrawable();
                chipBg2.setColor(Color.parseColor("#EEF4FF"));
                chipBg2.setCornerRadius(dpToPx(20));
                chip.setBackground(chipBg2);
                LinearLayout.LayoutParams chP = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                chP.setMarginEnd(dpToPx(6));
                chip.setLayoutParams(chP);
                chipsRow.addView(chip);
            }
            chipsScroll.addView(chipsRow);
            LinearLayout.LayoutParams csP = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            csP.bottomMargin = dpToPx(12);
            chipsScroll.setLayoutParams(csP);
            inner.addView(chipsScroll);
        }

        // ── Botones ───────────────────────────────────────────────────
        LinearLayout btnsRow = new LinearLayout(this);
        btnsRow.setOrientation(LinearLayout.HORIZONTAL);
        btnsRow.setGravity(Gravity.CENTER_VERTICAL);

        if (ocupada) {
            TextView btnVer = mkAccionBtn("👥 Ver todos", false);
            LinearLayout.LayoutParams bvP = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            bvP.setMarginEnd(dpToPx(8));
            btnVer.setLayoutParams(bvP);
            btnVer.setOnClickListener(v -> showPersonasSheet(fl, dia));
            btnsRow.addView(btnVer);
        }

        if (!llena) {
            TextView btnAdd = mkAccionBtn("＋ Añadir", true);
            btnAdd.setOnClickListener(v -> showAnadirSheet(fl, dia));
            btnsRow.addView(btnAdd);
        } else {
            TextView tvLleno = new TextView(this);
            tvLleno.setText("🔴 Aforo completo");
            tvLleno.setTextSize(11f);
            tvLleno.setTextColor(Color.parseColor("#EF4444"));
            tvLleno.setTypeface(Typeface.DEFAULT_BOLD);
            btnsRow.addView(tvLleno);
        }

        inner.addView(btnsRow);
        card.addView(inner);
        return card;
    }

    // ════════════════════════════════════════════════════════════════
    //  SHEET: VER TODAS LAS PERSONAS
    // ════════════════════════════════════════════════════════════════
    private void showPersonasSheet(FranjaLocal fl, String dia) {
        BottomSheetDialog sheet = new BottomSheetDialog(
                this, com.google.android.material.R.style.Theme_Material3_Light_BottomSheetDialog);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.WHITE);
        root.setPadding(dpToPx(20), dpToPx(16), dpToPx(20), dpToPx(32));
        root.addView(mkHandle());

        TextView tvTit = new TextView(this);
        tvTit.setText(dia + " · " + fl.modelo.horaDisplay());
        tvTit.setTextSize(18f);
        tvTit.setTextColor(Color.parseColor("#0D1B3E"));
        tvTit.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams tP = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        tP.bottomMargin = dpToPx(4);
        tvTit.setLayoutParams(tP);
        root.addView(tvTit);

        TextView tvSub = new TextView(this);
        tvSub.setText(fl.ocupacion() + " personas · Aforo máx. " + fl.modelo.aforoMax);
        tvSub.setTextSize(12f);
        tvSub.setTextColor(Color.parseColor("#6B7FA3"));
        LinearLayout.LayoutParams subP = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        subP.bottomMargin = dpToPx(20);
        tvSub.setLayoutParams(subP);
        root.addView(tvSub);

        ScrollView sv = new ScrollView(this);
        LinearLayout lista = new LinearLayout(this);
        lista.setOrientation(LinearLayout.VERTICAL);
        sv.addView(lista);
        LinearLayout.LayoutParams svP = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(300));
        svP.bottomMargin = dpToPx(20);
        sv.setLayoutParams(svP);

        for (int i = 0; i < fl.asistentes.size(); i++) {
            final AsistenciaModel a = fl.asistentes.get(i);
            final int pos = i;

            LinearLayout fila = new LinearLayout(this);
            fila.setOrientation(LinearLayout.HORIZONTAL);
            fila.setGravity(Gravity.CENTER_VERTICAL);
            fila.setPadding(dpToPx(14), dpToPx(12), dpToPx(14), dpToPx(12));
            LinearLayout.LayoutParams fP = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            fP.bottomMargin = dpToPx(8);
            fila.setLayoutParams(fP);
            android.graphics.drawable.GradientDrawable filaBg = new android.graphics.drawable.GradientDrawable();
            filaBg.setColor(Color.parseColor("#F8FAFF"));
            filaBg.setCornerRadius(dpToPx(14));
            fila.setBackground(filaBg);

            // Avatar
            LinearLayout avatar = new LinearLayout(this);
            avatar.setGravity(Gravity.CENTER);
            android.graphics.drawable.GradientDrawable avBg = new android.graphics.drawable.GradientDrawable();
            avBg.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            avBg.setColor(Color.parseColor("#0A66FF"));
            avatar.setBackground(avBg);
            LinearLayout.LayoutParams avP = new LinearLayout.LayoutParams(dpToPx(36), dpToPx(36));
            avP.setMarginEnd(dpToPx(12));
            avatar.setLayoutParams(avP);
            TextView tvInic = new TextView(this);
            tvInic.setText(String.valueOf(a.clienteNombre.charAt(0)).toUpperCase());
            tvInic.setTextSize(14f);
            tvInic.setTextColor(Color.WHITE);
            tvInic.setTypeface(Typeface.DEFAULT_BOLD);
            tvInic.setGravity(Gravity.CENTER);
            avatar.addView(tvInic);
            fila.addView(avatar);

            // Info
            LinearLayout infoCol = new LinearLayout(this);
            infoCol.setOrientation(LinearLayout.VERTICAL);
            infoCol.setLayoutParams(new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            TextView tvNum = new TextView(this);
            tvNum.setText("#" + (pos + 1));
            tvNum.setTextSize(10f);
            tvNum.setTextColor(Color.parseColor("#6B7FA3"));
            infoCol.addView(tvNum);
            TextView tvNom = new TextView(this);
            tvNom.setText(a.clienteNombre);
            tvNom.setTextSize(14f);
            tvNom.setTextColor(Color.parseColor("#0D1B3E"));
            tvNom.setTypeface(Typeface.DEFAULT_BOLD);
            infoCol.addView(tvNom);
            fila.addView(infoCol);

            // Botón eliminar → llama a Supabase
            TextView btnDel = new TextView(this);
            btnDel.setText("✕");
            btnDel.setTextSize(14f);
            btnDel.setTextColor(Color.parseColor("#EF4444"));
            btnDel.setTypeface(Typeface.DEFAULT_BOLD);
            btnDel.setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4));
            android.graphics.drawable.GradientDrawable delBg = new android.graphics.drawable.GradientDrawable();
            delBg.setColor(Color.parseColor("#FEF2F2"));
            delBg.setCornerRadius(dpToPx(10));
            btnDel.setBackground(delBg);
            btnDel.setOnClickListener(v -> {
                btnDel.setEnabled(false);
                SupabaseRepository.get().quitarPersona(
                        fechaHoy(), fl.modelo.id, a.clienteNombre,
                        new SupabaseRepository.Callback<Void>() {
                            @Override public void onSuccess(Void data) {
                                runOnUiThread(() -> {
                                    fl.asistentes.remove(a);
                                    sheet.dismiss();
                                    renderDias();
                                    renderFranjas();
                                    actualizarTotal();
                                });
                            }
                            @Override public void onError(String e) {
                                runOnUiThread(() -> {
                                    btnDel.setEnabled(true);
                                    Toast.makeText(AforoActivity.this,
                                            "Error: " + e, Toast.LENGTH_SHORT).show();
                                });
                            }
                        });
            });
            fila.addView(btnDel);
            lista.addView(fila);
        }
        root.addView(sv);

        if (!fl.llena()) {
            TextView btnAdd = new TextView(this);
            btnAdd.setText("＋ Añadir persona");
            btnAdd.setTextSize(15f);
            btnAdd.setTextColor(Color.WHITE);
            btnAdd.setTypeface(Typeface.DEFAULT_BOLD);
            btnAdd.setGravity(Gravity.CENTER);
            btnAdd.setPadding(0, dpToPx(14), 0, dpToPx(14));
            android.graphics.drawable.GradientDrawable addBg = new android.graphics.drawable.GradientDrawable();
            addBg.setColor(Color.parseColor("#0A66FF"));
            addBg.setCornerRadius(dpToPx(16));
            btnAdd.setBackground(addBg);
            btnAdd.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            btnAdd.setOnClickListener(v -> {
                sheet.dismiss();
                showAnadirSheet(fl, dia);
            });
            root.addView(btnAdd);
        }

        sheet.setContentView(root);
        sheet.show();
    }

    // ════════════════════════════════════════════════════════════════
    //  SHEET: AÑADIR PERSONA
    // ════════════════════════════════════════════════════════════════
    private void showAnadirSheet(FranjaLocal fl, String dia) {
        BottomSheetDialog sheet = new BottomSheetDialog(
                this, com.google.android.material.R.style.Theme_Material3_Light_BottomSheetDialog);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.WHITE);
        root.setPadding(dpToPx(20), dpToPx(16), dpToPx(20), dpToPx(32));
        root.addView(mkHandle());

        TextView tvTit = new TextView(this);
        tvTit.setText("Añadir a " + fl.modelo.horaDisplay() + " · " + dia);
        tvTit.setTextSize(17f);
        tvTit.setTextColor(Color.parseColor("#0D1B3E"));
        tvTit.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams tP = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        tP.bottomMargin = dpToPx(6);
        tvTit.setLayoutParams(tP);
        root.addView(tvTit);

        int libres = fl.modelo.aforoMax - fl.ocupacion();
        TextView tvSub = new TextView(this);
        tvSub.setText("Plazas disponibles: " + libres + "/" + fl.modelo.aforoMax);
        tvSub.setTextSize(12f);
        tvSub.setTextColor(Color.parseColor("#6B7FA3"));
        LinearLayout.LayoutParams subP = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        subP.bottomMargin = dpToPx(20);
        tvSub.setLayoutParams(subP);
        root.addView(tvSub);

        EditText etNombre = new EditText(this);
        etNombre.setHint("Nombre completo");
        etNombre.setTextSize(15f);
        etNombre.setTextColor(Color.parseColor("#0D1B3E"));
        etNombre.setHintTextColor(Color.parseColor("#6B7FA3"));
        etNombre.setPadding(dpToPx(16), dpToPx(14), dpToPx(16), dpToPx(14));
        etNombre.setBackground(mkFieldBg());
        LinearLayout.LayoutParams etP = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        etP.bottomMargin = dpToPx(24);
        etNombre.setLayoutParams(etP);
        root.addView(etNombre);

        TextView btnOk = new TextView(this);
        btnOk.setText("Añadir persona");
        btnOk.setTextSize(15f);
        btnOk.setTextColor(Color.WHITE);
        btnOk.setTypeface(Typeface.DEFAULT_BOLD);
        btnOk.setGravity(Gravity.CENTER);
        btnOk.setPadding(0, dpToPx(14), 0, dpToPx(14));
        android.graphics.drawable.GradientDrawable okBg = new android.graphics.drawable.GradientDrawable();
        okBg.setColor(Color.parseColor("#0A66FF"));
        okBg.setCornerRadius(dpToPx(16));
        btnOk.setBackground(okBg);
        btnOk.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        btnOk.setOnClickListener(v -> {
            String nombre = etNombre.getText().toString().trim();
            if (nombre.isEmpty()) {
                Toast.makeText(this, "Escribe un nombre", Toast.LENGTH_SHORT).show();
                return;
            }
            btnOk.setEnabled(false);
            btnOk.setText("Guardando...");

            // clienteId null → nombre libre (sin cuenta en la app)
            SupabaseRepository.get().apuntarPersona(
                    fechaHoy(), fl.modelo.id, null, nombre,
                    new SupabaseRepository.Callback<Void>() {
                        @Override public void onSuccess(Void data) {
                            // Añadir localmente para no recargar toda la lista
                            AsistenciaModel nueva = new AsistenciaModel();
                            nueva.clienteNombre = nombre;
                            nueva.fecha         = fechaHoy();
                            nueva.horarioSemanalId = fl.modelo.id;
                            fl.asistentes.add(nueva);

                            runOnUiThread(() -> {
                                sheet.dismiss();
                                renderDias();
                                renderFranjas();
                                actualizarTotal();
                                Toast.makeText(AforoActivity.this,
                                        "✓ " + nombre + " añadido/a a las "
                                                + fl.modelo.horaDisplay(),
                                        Toast.LENGTH_SHORT).show();
                            });
                        }
                        @Override public void onError(String e) {
                            runOnUiThread(() -> {
                                btnOk.setEnabled(true);
                                btnOk.setText("Añadir persona");
                                Toast.makeText(AforoActivity.this,
                                        "Error: " + e, Toast.LENGTH_SHORT).show();
                            });
                        }
                    });
        });
        root.addView(btnOk);

        sheet.setContentView(root);
        sheet.show();
    }

    // ════════════════════════════════════════════════════════════════
    //  TOTAL DEL DÍA
    // ════════════════════════════════════════════════════════════════
    private void actualizarTotal() {
        int total = 0;
        for (FranjaLocal fl : franjasDelDia) total += fl.ocupacion();
        tvTotalPersonas.setText(total + " hoy");
    }

    // ════════════════════════════════════════════════════════════════
    //  BOTTOM NAV
    // ════════════════════════════════════════════════════════════════
    private LinearLayout buildBottomNav() {
        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setBackgroundColor(Color.WHITE);
        nav.setElevation(dpToPx(16));
        nav.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(70)));
        nav.setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8));

        String[][]   tabs    = {{"🏠","Inicio"},{"👥","Clientes"},{"📅","Agenda"},{"💰","Cobros"}};
        Class<?>[]   targets = {MainActivity.class, ClientesActivity.class,
                AgendaActivity.class, CobrosActivity.class};

        for (int i = 0; i < tabs.length; i++) {
            final int idx = i;
            LinearLayout tab = new LinearLayout(this);
            tab.setOrientation(LinearLayout.VERTICAL);
            tab.setGravity(Gravity.CENTER);
            tab.setLayoutParams(new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.MATCH_PARENT, 1f));

            TextView tvIcon = new TextView(this);
            tvIcon.setText(tabs[i][0]);
            tvIcon.setTextSize(18f);
            tvIcon.setGravity(Gravity.CENTER);
            tab.addView(tvIcon);

            TextView tvLabel = new TextView(this);
            tvLabel.setText(tabs[i][1]);
            tvLabel.setTextSize(10f);
            tvLabel.setTextColor(Color.parseColor("#6B7FA3"));
            tvLabel.setGravity(Gravity.CENTER);
            tab.addView(tvLabel);

            tab.setOnClickListener(v -> {
                startActivity(new Intent(this, targets[idx]));
                overridePendingTransition(0, 0);
                finish();
            });
            nav.addView(tab);
        }
        return nav;
    }

    // ════════════════════════════════════════════════════════════════
    //  HELPERS
    // ════════════════════════════════════════════════════════════════
    private String fechaHoy() {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        return String.format(java.util.Locale.getDefault(), "%04d-%02d-%02d",
                cal.get(java.util.Calendar.YEAR),
                cal.get(java.util.Calendar.MONTH) + 1,
                cal.get(java.util.Calendar.DAY_OF_MONTH));
    }

    private View mkHandle() {
        LinearLayout hw = new LinearLayout(this);
        hw.setGravity(Gravity.CENTER_HORIZONTAL);
        LinearLayout.LayoutParams hwP = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        hwP.bottomMargin = dpToPx(20);
        hw.setLayoutParams(hwP);
        View hdl = new View(this);
        android.graphics.drawable.GradientDrawable hbg = new android.graphics.drawable.GradientDrawable();
        hbg.setColor(Color.parseColor("#DDE6FF"));
        hbg.setCornerRadius(dpToPx(4));
        hdl.setBackground(hbg);
        hdl.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(40), dpToPx(4)));
        hw.addView(hdl);
        return hw;
    }

    private TextView mkAccionBtn(String texto, boolean primary) {
        TextView tv = new TextView(this);
        tv.setText(texto);
        tv.setTextSize(12f);
        tv.setTextColor(primary ? Color.WHITE : Color.parseColor("#0A66FF"));
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(dpToPx(14), dpToPx(8), dpToPx(14), dpToPx(8));
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setColor(primary ? Color.parseColor("#0A66FF") : Color.parseColor("#EEF4FF"));
        bg.setCornerRadius(dpToPx(20));
        tv.setBackground(bg);
        return tv;
    }

    private android.graphics.drawable.GradientDrawable mkFieldBg() {
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setColor(Color.parseColor("#F5F8FF"));
        bg.setCornerRadius(dpToPx(14));
        bg.setStroke(dpToPx(1), Color.parseColor("#DDE6FF"));
        return bg;
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}