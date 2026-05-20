package com.example.kaunatureapplication;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
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

public class CobrosActivity extends AppCompatActivity {

    // ── Modelo local ─────────────────────────────────────────────────
    static class Cobro {
        String id;          // UUID Supabase
        String clienteId;   // UUID del cliente (para enlazar)
        String cliente;
        String concepto;
        double importe;
        String metodo;      // "Efectivo","Tarjeta","Bizum","Transferencia"
        String estado;      // "pendiente","cobrado"
        String fecha;       // dd/MM/yyyy  (display)
        String fechaBD;     // yyyy-MM-dd  (Supabase)
        String notas;

        /** Construye desde modelo de red */
        Cobro(CobroModel m) {
            this.id        = m.id;
            this.clienteId = m.clienteId;
            this.cliente   = m.clienteNombre != null ? m.clienteNombre : "";
            this.concepto = m.concepto      != null ? m.concepto      : "";
            this.importe  = m.importe;
            this.metodo   = m.metodo        != null ? m.metodo        : "Efectivo";
            this.estado   = m.estado        != null ? m.estado        : "cobrado";
            this.notas    = m.notas         != null ? m.notas         : "";
            this.fechaBD  = m.fecha         != null ? m.fecha         : "";
            // Supabase devuelve "yyyy-MM-dd" → convertir a "dd/MM/yyyy" para UI
            if (fechaBD.length() >= 10) {
                this.fecha = fechaBD.substring(8, 10) + "/" +
                        fechaBD.substring(5, 7) + "/" +
                        fechaBD.substring(0, 4);
            } else {
                this.fecha = fechaBD;
            }
        }

        /** Constructor para cobros nuevos antes de guardar */
        Cobro(String cliente, String concepto, double importe,
              String metodo, String estado, String fechaBD, String notas) {
            this.id       = null;
            this.cliente  = cliente;
            this.concepto = concepto;
            this.importe  = importe;
            this.metodo   = metodo;
            this.estado   = estado;
            this.fechaBD  = fechaBD;
            this.notas    = notas;
            if (fechaBD.length() >= 10) {
                this.fecha = fechaBD.substring(8, 10) + "/" +
                        fechaBD.substring(5, 7) + "/" +
                        fechaBD.substring(0, 4);
            } else {
                this.fecha = fechaBD;
            }
        }

        String inicial() {
            return cliente != null && !cliente.isEmpty()
                    ? String.valueOf(cliente.charAt(0)).toUpperCase() : "?";
        }

        String importeFormateado() {
            return String.format("%.2f€", importe).replace(".", ",");
        }
    }

    // ── Métodos de pago ──────────────────────────────────────────────
    private static final String[] METODOS     = {"💵 Efectivo","💳 Tarjeta","📱 Bizum","🏦 Transferencia"};
    private static final String[] METODOS_KEY = {"Efectivo","Tarjeta","Bizum","Transferencia"};

    // ── Estado ───────────────────────────────────────────────────────
    private final List<Cobro> todosCobros    = new ArrayList<>();
    private String filtroActual              = "Todos";
    private String filtroClienteId           = null;  // viene de ClientesActivity
    private String filtroClienteNom          = null;
    private final List<ClienteModel> todosLosClientes = new ArrayList<>();

    // ── Views ────────────────────────────────────────────────────────
    private LinearLayout listaPendientes, listaHistorial;
    private LinearLayout seccionPendientes, seccionHistorial, layoutVacio;
    private TextView     tvTotalMes, tvKpiEfectivo, tvKpiTarjeta, tvKpiBizumTransfer;
    private TextView     tvMesLabel, tvSubtitle;
    private TextView     filtroTodos, filtroPendientes, filtroCobrados;
    private TextView     filtroEfectivo, filtroTarjeta, filtroBizum, filtroTransferencia;

    // ════════════════════════════════════════════════════════════════
    //  onCreate
    // ════════════════════════════════════════════════════════════════
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cobros);

        bindViews();
        setupFiltros();
        setupBotones();
        setupBottomNav();

        // Recibir cliente desde ClientesActivity
        if (getIntent() != null) {
            filtroClienteId  = getIntent().getStringExtra("CLIENTE_ID");
            filtroClienteNom = getIntent().getStringExtra("CLIENTE_NOMBRE");

        }

        cargarClientes();  // cargar pool de clientes para el buscador
        cargarCobros();
    }

    // ════════════════════════════════════════════════════════════════
    //  BIND
    // ════════════════════════════════════════════════════════════════
    private void bindViews() {
        listaPendientes    = findViewById(R.id.listaPendientes);
        listaHistorial     = findViewById(R.id.listaHistorial);
        seccionPendientes  = findViewById(R.id.seccionPendientes);
        seccionHistorial   = findViewById(R.id.seccionHistorial);
        layoutVacio        = findViewById(R.id.layoutVacio);
        tvTotalMes         = findViewById(R.id.tvTotalMes);
        tvKpiEfectivo      = findViewById(R.id.tvKpiEfectivo);
        tvKpiTarjeta       = findViewById(R.id.tvKpiTarjeta);
        tvKpiBizumTransfer = findViewById(R.id.tvKpiBizumTransfer);
        tvMesLabel         = findViewById(R.id.tvMesLabel);
        tvSubtitle         = findViewById(R.id.tvCobrosSubtitle);
        filtroTodos        = findViewById(R.id.filtroTodos);
        filtroPendientes   = findViewById(R.id.filtroPendientes);
        filtroCobrados     = findViewById(R.id.filtroCobrados);
        filtroEfectivo     = findViewById(R.id.filtroEfectivo);
        filtroTarjeta      = findViewById(R.id.filtroTarjeta);
        filtroBizum        = findViewById(R.id.filtroBizum);
        filtroTransferencia = findViewById(R.id.filtroTransferencia);
    }

    // ════════════════════════════════════════════════════════════════
    //  CARGA DESDE SUPABASE
    // ════════════════════════════════════════════════════════════════
    private void cargarCobros() {
        tvSubtitle.setText("Cargando...");

        SupabaseRepository.get().getCobros(null,
                new SupabaseRepository.Callback<List<CobroModel>>() {
                    @Override public void onSuccess(List<CobroModel> data) {
                        runOnUiThread(() -> {
                            todosCobros.clear();
                            for (CobroModel m : data) todosCobros.add(new Cobro(m));
                            renderTodo();
                        });
                    }
                    @Override public void onError(String e) {
                        runOnUiThread(() -> {
                            Toast.makeText(CobrosActivity.this,
                                    "Error al cargar cobros: " + e, Toast.LENGTH_LONG).show();
                            renderTodo();
                        });
                    }
                });
    }

    // ════════════════════════════════════════════════════════════════
    //  FILTROS
    // ════════════════════════════════════════════════════════════════
    private void setupFiltros() {
        TextView[] btns = {filtroTodos, filtroPendientes, filtroCobrados,
                filtroEfectivo, filtroTarjeta, filtroBizum, filtroTransferencia};
        String[]   keys = {"Todos","pendiente","cobrado",
                "Efectivo","Tarjeta","Bizum","Transferencia"};

        for (int i = 0; i < btns.length; i++) {
            final String key = keys[i];
            btns[i].setOnClickListener(v -> {
                filtroActual = key;
                for (int j = 0; j < btns.length; j++) {
                    boolean sel = keys[j].equals(key);
                    btns[j].setBackground(getDrawable(sel
                            ? R.drawable.shape_filter_active
                            : R.drawable.shape_filter_inactive));
                    btns[j].setTextColor(sel ? Color.WHITE : Color.parseColor("#6B7FA3"));
                }
                renderTodo();
            });
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  RENDER
    // ════════════════════════════════════════════════════════════════
    private void renderTodo() {
        List<Cobro> filtrados = new ArrayList<>();
        for (Cobro c : todosCobros) {
            // Si venimos de un cliente concreto, filtrar solo sus cobros
            if (filtroClienteId != null && !filtroClienteId.isEmpty()) {
                if (!filtroClienteId.equals(c.clienteId)) continue;
            }
            boolean pasa;
            switch (filtroActual) {
                case "pendiente":     pasa = "pendiente".equals(c.estado);      break;
                case "cobrado":       pasa = "cobrado".equals(c.estado);        break;
                case "Efectivo":      pasa = "Efectivo".equals(c.metodo);       break;
                case "Tarjeta":       pasa = "Tarjeta".equals(c.metodo);        break;
                case "Bizum":         pasa = "Bizum".equals(c.metodo);          break;
                case "Transferencia": pasa = "Transferencia".equals(c.metodo);  break;
                default:              pasa = true;
            }
            if (pasa) filtrados.add(c);
        }

        List<Cobro> pendientes = new ArrayList<>();
        List<Cobro> cobrados   = new ArrayList<>();
        for (Cobro c : filtrados) {
            if ("pendiente".equals(c.estado)) pendientes.add(c);
            else cobrados.add(c);
        }

        actualizarKpis();

        listaPendientes.removeAllViews();
        listaHistorial.removeAllViews();

        boolean hayAlgo = !pendientes.isEmpty() || !cobrados.isEmpty();
        layoutVacio.setVisibility(hayAlgo ? View.GONE : View.VISIBLE);
        seccionPendientes.setVisibility(!pendientes.isEmpty() ? View.VISIBLE : View.GONE);
        seccionHistorial.setVisibility(!cobrados.isEmpty() ? View.VISIBLE : View.GONE);

        for (Cobro c : pendientes) listaPendientes.addView(buildCobroCard(c));
        for (Cobro c : cobrados)   listaHistorial.addView(buildCobroCard(c));

        long totalPend = todosCobros.stream().filter(c -> "pendiente".equals(c.estado)).count();
        tvSubtitle.setText(totalPend > 0
                ? totalPend + " cobro" + (totalPend > 1 ? "s" : "") + " pendiente" + (totalPend > 1 ? "s" : "")
                : "Todo cobrado ✅");
    }

    private void actualizarKpis() {
        String mesLabel = new SimpleDateFormat("MMMM yyyy", new Locale("es","ES")).format(new Date());
        mesLabel = Character.toUpperCase(mesLabel.charAt(0)) + mesLabel.substring(1);
        tvMesLabel.setText(mesLabel);

        double total = 0, efectivo = 0, tarjeta = 0, bizumTransfer = 0;
        for (Cobro c : todosCobros) {
            if (!"cobrado".equals(c.estado)) continue;
            total += c.importe;
            switch (c.metodo) {
                case "Efectivo":      efectivo      += c.importe; break;
                case "Tarjeta":       tarjeta       += c.importe; break;
                case "Bizum":
                case "Transferencia": bizumTransfer += c.importe; break;
            }
        }
        tvTotalMes.setText(String.format("%.2f€", total).replace(".", ","));
        tvKpiEfectivo.setText(String.format("%.0f€", efectivo));
        tvKpiTarjeta.setText(String.format("%.0f€", tarjeta));
        tvKpiBizumTransfer.setText(String.format("%.0f€", bizumTransfer));
    }

    // ════════════════════════════════════════════════════════════════
    //  CARD DE COBRO
    // ════════════════════════════════════════════════════════════════
    private View buildCobroCard(Cobro cobro) {
        CardView card = new CardView(this);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cp.bottomMargin = dpToPx(10);
        card.setLayoutParams(cp);
        card.setRadius(dpToPx(18));
        card.setCardElevation(dpToPx(2));
        card.setCardBackgroundColor("pendiente".equals(cobro.estado)
                ? Color.parseColor("#FFFBF0") : Color.WHITE);

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
        avatar.setCardBackgroundColor("pendiente".equals(cobro.estado)
                ? Color.parseColor("#F59E0B") : Color.parseColor("#12B76A"));
        TextView tvI = new TextView(this);
        tvI.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        tvI.setText(cobro.inicial());
        tvI.setTextSize(18f);
        tvI.setTextColor(Color.WHITE);
        tvI.setTypeface(getResources().getFont(R.font.outfit_bold));
        tvI.setGravity(Gravity.CENTER);
        avatar.addView(tvI);
        row.addView(avatar);

        // Texto
        LinearLayout textBlock = new LinearLayout(this);
        textBlock.setOrientation(LinearLayout.VERTICAL);
        textBlock.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView tvNombre = new TextView(this);
        tvNombre.setText(cobro.cliente);
        tvNombre.setTextSize(13f);
        tvNombre.setTextColor(Color.parseColor("#0D1B3E"));
        tvNombre.setTypeface(getResources().getFont(R.font.outfit_bold));
        tvNombre.setMaxLines(1);
        tvNombre.setEllipsize(android.text.TextUtils.TruncateAt.END);
        textBlock.addView(tvNombre);

        TextView tvConcepto = new TextView(this);
        tvConcepto.setText(cobro.concepto + " · " + getMetodoEmoji(cobro.metodo));
        tvConcepto.setTextSize(11f);
        tvConcepto.setTextColor(Color.parseColor("#6B7FA3"));
        tvConcepto.setTypeface(getResources().getFont(R.font.outfit_regular));
        LinearLayout.LayoutParams subP = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        subP.topMargin = dpToPx(2);
        tvConcepto.setLayoutParams(subP);
        textBlock.addView(tvConcepto);

        TextView tvFecha = new TextView(this);
        tvFecha.setText(cobro.fecha);
        tvFecha.setTextSize(10f);
        tvFecha.setTextColor(Color.parseColor("#6B7FA3"));
        tvFecha.setTypeface(getResources().getFont(R.font.outfit_regular));
        LinearLayout.LayoutParams fechaP = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        fechaP.topMargin = dpToPx(2);
        tvFecha.setLayoutParams(fechaP);
        textBlock.addView(tvFecha);
        row.addView(textBlock);

        // Importe + estado
        LinearLayout rightCol = new LinearLayout(this);
        rightCol.setOrientation(LinearLayout.VERTICAL);
        rightCol.setGravity(Gravity.END);
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rp.setMarginStart(dpToPx(8));
        rightCol.setLayoutParams(rp);

        TextView tvImporte = new TextView(this);
        tvImporte.setText(cobro.importeFormateado());
        tvImporte.setTextSize(16f);
        tvImporte.setTextColor("pendiente".equals(cobro.estado)
                ? Color.parseColor("#F59E0B") : Color.parseColor("#12B76A"));
        tvImporte.setTypeface(getResources().getFont(R.font.outfit_bold));
        tvImporte.setGravity(Gravity.END);
        rightCol.addView(tvImporte);

        TextView tvEstado = new TextView(this);
        tvEstado.setText("pendiente".equals(cobro.estado) ? "Pendiente" : "Cobrado");
        tvEstado.setTextSize(9f);
        tvEstado.setTextColor(Color.WHITE);
        tvEstado.setTypeface(getResources().getFont(R.font.outfit_bold));
        tvEstado.setPadding(dpToPx(8), dpToPx(3), dpToPx(8), dpToPx(3));
        tvEstado.setBackground(getDrawable(R.drawable.shape_chip_blue));
        tvEstado.getBackground().setTint("pendiente".equals(cobro.estado)
                ? Color.parseColor("#F59E0B") : Color.parseColor("#12B76A"));
        LinearLayout.LayoutParams ep = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        ep.topMargin = dpToPx(4);
        ep.gravity = Gravity.END;
        tvEstado.setLayoutParams(ep);
        rightCol.addView(tvEstado);
        row.addView(rightCol);

        card.setClickable(true);
        card.setForeground(getDrawable(android.R.drawable.list_selector_background));
        card.setOnClickListener(v -> showDetalleCobro(cobro));
        return card;
    }

    // ════════════════════════════════════════════════════════════════
    //  SHEET: DETALLE COBRO
    // ════════════════════════════════════════════════════════════════
    private void showDetalleCobro(Cobro cobro) {
        BottomSheetDialog sheet = new BottomSheetDialog(
                this, com.google.android.material.R.style.Theme_Material3_Light_BottomSheetDialog);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackground(getDrawable(R.drawable.shape_sheet_bg));
        layout.setPadding(dpToPx(20), dpToPx(20), dpToPx(20), dpToPx(30));

        // Handle
        View handle = new View(this);
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(dpToPx(40), dpToPx(4));
        hp.gravity = Gravity.CENTER_HORIZONTAL;
        hp.bottomMargin = dpToPx(16);
        handle.setLayoutParams(hp);
        handle.setBackground(getDrawable(R.drawable.shape_handle));
        layout.addView(handle);

        // Cabecera
        TextView tvNombre = new TextView(this);
        tvNombre.setText(cobro.cliente);
        tvNombre.setTextSize(18f);
        tvNombre.setTextColor(Color.parseColor("#0D1B3E"));
        tvNombre.setTypeface(getResources().getFont(R.font.outfit_bold));
        layout.addView(tvNombre);

        TextView tvConcepto = new TextView(this);
        tvConcepto.setText(cobro.concepto + " · " + cobro.metodo);
        tvConcepto.setTextSize(12f);
        tvConcepto.setTextColor(Color.parseColor("#6B7FA3"));
        tvConcepto.setTypeface(getResources().getFont(R.font.outfit_regular));
        LinearLayout.LayoutParams cP = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cP.topMargin = dpToPx(4);
        cP.bottomMargin = dpToPx(8);
        tvConcepto.setLayoutParams(cP);
        layout.addView(tvConcepto);

        TextView tvImporte = new TextView(this);
        tvImporte.setText(cobro.importeFormateado());
        tvImporte.setTextSize(32f);
        tvImporte.setTextColor("pendiente".equals(cobro.estado)
                ? Color.parseColor("#F59E0B") : Color.parseColor("#12B76A"));
        tvImporte.setTypeface(getResources().getFont(R.font.outfit_bold));
        LinearLayout.LayoutParams iP = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        iP.bottomMargin = dpToPx(4);
        tvImporte.setLayoutParams(iP);
        layout.addView(tvImporte);

        TextView tvFecha = new TextView(this);
        tvFecha.setText("📅 " + cobro.fecha);
        tvFecha.setTextSize(12f);
        tvFecha.setTextColor(Color.parseColor("#6B7FA3"));
        tvFecha.setTypeface(getResources().getFont(R.font.outfit_regular));
        layout.addView(tvFecha);

        if (!cobro.notas.isEmpty()) {
            TextView tvNotas = new TextView(this);
            tvNotas.setText("📝 " + cobro.notas);
            tvNotas.setTextSize(12f);
            tvNotas.setTextColor(Color.parseColor("#6B7FA3"));
            tvNotas.setTypeface(getResources().getFont(R.font.outfit_regular));
            LinearLayout.LayoutParams nP = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            nP.topMargin = dpToPx(4);
            tvNotas.setLayoutParams(nP);
            layout.addView(tvNotas);
        }

        // Divider
        View div = new View(this);
        LinearLayout.LayoutParams dp2 = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(1));
        dp2.topMargin = dpToPx(16);
        dp2.bottomMargin = dpToPx(14);
        div.setLayoutParams(dp2);
        div.setBackgroundColor(Color.parseColor("#DDE6FF"));
        layout.addView(div);

        // ── Botón: marcar cobrado (solo si pendiente) ──────────────
        if ("pendiente".equals(cobro.estado)) {
            CardView btnCobrar = buildAccionBtn("💰 Marcar como cobrado", "#0A66FF", true);
            btnCobrar.setOnClickListener(v -> {
                if (cobro.id == null) {
                    cobro.estado = "cobrado";
                    sheet.dismiss();
                    renderTodo();
                    return;
                }
                ((TextView) btnCobrar.getChildAt(0)).setText("Guardando...");
                btnCobrar.setClickable(false);

                SupabaseRepository.get().marcarCobrado(cobro.id,
                        new SupabaseRepository.Callback<Void>() {
                            @Override public void onSuccess(Void data) {
                                runOnUiThread(() -> {
                                    cobro.estado = "cobrado";
                                    sheet.dismiss();
                                    renderTodo();
                                    Toast.makeText(CobrosActivity.this,
                                            "✅ Cobro registrado: " + cobro.importeFormateado(),
                                            Toast.LENGTH_SHORT).show();
                                });
                            }
                            @Override public void onError(String e) {
                                runOnUiThread(() -> {
                                    ((TextView) btnCobrar.getChildAt(0)).setText("💰 Marcar como cobrado");
                                    btnCobrar.setClickable(true);
                                    Toast.makeText(CobrosActivity.this,
                                            "Error: " + e, Toast.LENGTH_SHORT).show();
                                });
                            }
                        });
            });
            layout.addView(btnCobrar);
        }

        // ── Botón: ver cliente (si tiene clienteId) ───────────────
        if (cobro.clienteId != null && !cobro.clienteId.isEmpty()) {
            CardView btnCliente = buildAccionBtn("👤 Ver cliente", "#EEF4FF", false);
            ((TextView) btnCliente.getChildAt(0)).setTextColor(Color.parseColor("#0A66FF"));
            LinearLayout.LayoutParams clP = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(52));
            clP.bottomMargin = dpToPx(8);
            btnCliente.setLayoutParams(clP);
            btnCliente.setOnClickListener(v -> {
                sheet.dismiss();
                Intent iClientes = new Intent(this, ClientesActivity.class);
                iClientes.putExtra("OPEN_CLIENTE_ID", cobro.clienteId);
                startActivity(iClientes);
            });
            layout.addView(btnCliente);
        }

        // ── Botón: editar ──────────────────────────────────────────
        CardView btnEditar = buildAccionBtn("✏️ Editar cobro", "#E8F0FF", false);
        ((TextView) btnEditar.getChildAt(0)).setTextColor(Color.parseColor("#0A66FF"));
        LinearLayout.LayoutParams eP = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(52));
        eP.bottomMargin = dpToPx(8);
        btnEditar.setLayoutParams(eP);
        btnEditar.setOnClickListener(v -> {
            sheet.dismiss();
            showNuevoCobroSheet(cobro);
        });
        layout.addView(btnEditar);

        // ── Botón: eliminar ────────────────────────────────────────
        CardView btnEliminar = buildAccionBtn("🗑 Eliminar cobro", "#FFF0F0", false);
        ((TextView) btnEliminar.getChildAt(0)).setTextColor(Color.parseColor("#EF4444"));
        LinearLayout.LayoutParams delP = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(52));
        btnEliminar.setLayoutParams(delP);
        btnEliminar.setOnClickListener(v -> {
            if (cobro.id == null) {
                todosCobros.remove(cobro);
                sheet.dismiss();
                renderTodo();
                return;
            }
            ((TextView) btnEliminar.getChildAt(0)).setText("Eliminando...");
            btnEliminar.setClickable(false);

            SupabaseRepository.get().eliminarCobro(cobro.id,
                    new SupabaseRepository.Callback<Void>() {
                        @Override public void onSuccess(Void data) {
                            runOnUiThread(() -> {
                                todosCobros.remove(cobro);
                                sheet.dismiss();
                                renderTodo();
                                Toast.makeText(CobrosActivity.this,
                                        "🗑 Cobro eliminado", Toast.LENGTH_SHORT).show();
                            });
                        }
                        @Override public void onError(String e) {
                            runOnUiThread(() -> {
                                ((TextView) btnEliminar.getChildAt(0)).setText("🗑 Eliminar cobro");
                                btnEliminar.setClickable(true);
                                Toast.makeText(CobrosActivity.this,
                                        "Error: " + e, Toast.LENGTH_SHORT).show();
                            });
                        }
                    });
        });
        layout.addView(btnEliminar);

        sheet.setContentView(layout);
        sheet.show();
    }

    private CardView buildAccionBtn(String texto, String bgColor, boolean esPrimario) {
        CardView btn = new CardView(this);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(52));
        p.bottomMargin = dpToPx(8);
        btn.setLayoutParams(p);
        btn.setRadius(dpToPx(16));
        btn.setCardElevation(esPrimario ? dpToPx(3) : dpToPx(1));
        btn.setCardBackgroundColor(Color.parseColor(bgColor));

        TextView tv = new TextView(this);
        tv.setText(texto);
        tv.setTextSize(13f);
        tv.setTextColor(esPrimario ? Color.WHITE : Color.parseColor("#0D1B3E"));
        tv.setTypeface(getResources().getFont(R.font.outfit_bold));
        tv.setGravity(Gravity.CENTER);
        tv.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        btn.addView(tv);
        btn.setClickable(true);
        return btn;
    }

    // ════════════════════════════════════════════════════════════════
    //  SHEET: NUEVO / EDITAR COBRO
    // ════════════════════════════════════════════════════════════════
    private void showNuevoCobroSheet(Cobro cobroEditar) {
        BottomSheetDialog sheet = new BottomSheetDialog(
                this, com.google.android.material.R.style.Theme_Material3_Light_BottomSheetDialog);
        View view = getLayoutInflater().inflate(R.layout.sheet_nuevo_cobro, null);
        sheet.setContentView(view);

        EditText etCliente  = view.findViewById(R.id.etCobroCliente);
        EditText etConcepto = view.findViewById(R.id.etCobroConcepto);
        EditText etImporte  = view.findViewById(R.id.etCobroImporte);
        EditText etNotas    = view.findViewById(R.id.etCobroNotas);
        TextView tvTitulo   = view.findViewById(R.id.tvNuevoCobroTitulo);
        androidx.cardview.widget.CardView btnGuardarCard = view.findViewById(R.id.btnGuardarCobro);
        TextView btnGuardar = (TextView) btnGuardarCard.getChildAt(0);

        // Buscador de clientes — se inyecta debajo del campo etCliente
        final String[] clienteIdSel  = {null};
        final boolean[] seleccionando = {false};
        android.widget.LinearLayout layoutSug = new android.widget.LinearLayout(this);
        layoutSug.setOrientation(android.widget.LinearLayout.VERTICAL);
        layoutSug.setVisibility(android.view.View.GONE);
        android.view.ViewGroup parentEt = (android.view.ViewGroup) etCliente.getParent();
        int idxEt = -1;
        for (int i = 0; i < parentEt.getChildCount(); i++)
            if (parentEt.getChildAt(i) == etCliente) { idxEt = i; break; }
        if (idxEt >= 0) parentEt.addView(layoutSug, idxEt + 1);
        setupBuscadorClientes(etCliente, layoutSug, clienteIdSel, seleccionando);

        final String[] metodoSel = {METODOS_KEY[0]};
        final String[] estadoSel = {"cobrado"};

        // Si venimos de ClientesActivity, prerellenar cliente
        if (cobroEditar == null && filtroClienteNom != null && !filtroClienteNom.isEmpty()) {
            etCliente.setText(filtroClienteNom);
            clienteIdSel[0] = filtroClienteId;
        }

        if (cobroEditar != null) {
            tvTitulo.setText("Editar cobro");
            etCliente.setText(cobroEditar.cliente);
            etConcepto.setText(cobroEditar.concepto);
            etImporte.setText(String.valueOf(cobroEditar.importe));
            etNotas.setText(cobroEditar.notas);
            metodoSel[0] = cobroEditar.metodo;
            estadoSel[0] = cobroEditar.estado;
        }

        // Chips método de pago
        LinearLayout layoutMetodos = view.findViewById(R.id.layoutMetodos);
        for (int i = 0; i < METODOS.length; i++) {
            final String key = METODOS_KEY[i];
            CardView chip = new CardView(this);
            LinearLayout.LayoutParams chP = new LinearLayout.LayoutParams(0, dpToPx(44), 1f);
            chP.setMarginEnd(i < METODOS.length - 1 ? dpToPx(6) : 0);
            chip.setLayoutParams(chP);
            chip.setRadius(dpToPx(12));
            chip.setCardElevation(dpToPx(1));
            chip.setCardBackgroundColor(key.equals(metodoSel[0])
                    ? Color.parseColor("#0A66FF") : Color.parseColor("#E8F0FF"));
            TextView tvChip = new TextView(this);
            tvChip.setText(METODOS[i]);
            tvChip.setTextSize(10f);
            tvChip.setTextColor(key.equals(metodoSel[0]) ? Color.WHITE : Color.parseColor("#6B7FA3"));
            tvChip.setTypeface(getResources().getFont(R.font.outfit_bold));
            tvChip.setGravity(Gravity.CENTER);
            tvChip.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
            chip.addView(tvChip);
            chip.setOnClickListener(v -> {
                metodoSel[0] = key;
                for (int j = 0; j < layoutMetodos.getChildCount(); j++) {
                    CardView c = (CardView) layoutMetodos.getChildAt(j);
                    boolean sel = METODOS_KEY[j].equals(key);
                    c.setCardBackgroundColor(sel ? Color.parseColor("#0A66FF") : Color.parseColor("#E8F0FF"));
                    ((TextView) c.getChildAt(0)).setTextColor(sel ? Color.WHITE : Color.parseColor("#6B7FA3"));
                }
            });
            layoutMetodos.addView(chip);
        }

        // Chips estado
        LinearLayout layoutEstados = view.findViewById(R.id.layoutEstados);
        String[][] estados = {{"cobrado","✅ Cobrado"},{"pendiente","⏳ Pendiente"}};
        for (String[] est : estados) {
            final String key = est[0];
            CardView chip = new CardView(this);
            LinearLayout.LayoutParams eP2 = new LinearLayout.LayoutParams(0, dpToPx(44), 1f);
            eP2.setMarginEnd(key.equals("cobrado") ? dpToPx(6) : 0);
            chip.setLayoutParams(eP2);
            chip.setRadius(dpToPx(12));
            chip.setCardElevation(dpToPx(1));
            chip.setCardBackgroundColor(key.equals(estadoSel[0])
                    ? Color.parseColor("#12B76A") : Color.parseColor("#E8F0FF"));
            TextView tvChip = new TextView(this);
            tvChip.setText(est[1]);
            tvChip.setTextSize(11f);
            tvChip.setTextColor(key.equals(estadoSel[0]) ? Color.WHITE : Color.parseColor("#6B7FA3"));
            tvChip.setTypeface(getResources().getFont(R.font.outfit_bold));
            tvChip.setGravity(Gravity.CENTER);
            tvChip.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
            chip.addView(tvChip);
            chip.setOnClickListener(v -> {
                estadoSel[0] = key;
                for (int j = 0; j < layoutEstados.getChildCount(); j++) {
                    CardView c = (CardView) layoutEstados.getChildAt(j);
                    boolean sel = estados[j][0].equals(key);
                    c.setCardBackgroundColor(sel ? Color.parseColor("#12B76A") : Color.parseColor("#E8F0FF"));
                    ((TextView) c.getChildAt(0)).setTextColor(sel ? Color.WHITE : Color.parseColor("#6B7FA3"));
                }
            });
            layoutEstados.addView(chip);
        }

        // ── Guardar ───────────────────────────────────────────────
        btnGuardarCard.setOnClickListener(v -> {
            String nombre   = etCliente.getText().toString().trim();
            String importeS = etImporte.getText().toString().trim();
            if (nombre.isEmpty())   { etCliente.setError("Obligatorio"); return; }
            if (importeS.isEmpty()) { etImporte.setError("Obligatorio"); return; }

            double importe;
            try { importe = Double.parseDouble(importeS.replace(",", ".")); }
            catch (NumberFormatException e) { etImporte.setError("Importe inválido"); return; }

            String concepto = etConcepto.getText().toString().trim();
            String notas    = etNotas.getText().toString().trim();
            String hoy      = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

            btnGuardarCard.setEnabled(false);
            btnGuardar.setText("Guardando...");

            if (cobroEditar != null && cobroEditar.id != null) {
                // ── EDITAR ────────────────────────────────────────
                Map<String, Object> body = new HashMap<>();
                body.put("cliente_nombre", nombre);
                body.put("concepto",       concepto);
                body.put("importe",        importe);
                body.put("metodo",         metodoSel[0]);
                body.put("estado",         estadoSel[0]);
                body.put("notas",          notas);

                final double importeFinal = importe;
                SupabaseRepository.get().actualizarCobro(cobroEditar.id, body,
                        new SupabaseRepository.Callback<Void>() {
                            @Override public void onSuccess(Void data) {
                                runOnUiThread(() -> {
                                    cobroEditar.cliente  = nombre;
                                    cobroEditar.concepto = concepto;
                                    cobroEditar.importe  = importeFinal;
                                    cobroEditar.metodo   = metodoSel[0];
                                    cobroEditar.estado   = estadoSel[0];
                                    cobroEditar.notas    = notas;
                                    sheet.dismiss();
                                    ocultarTeclado();
                                    renderTodo();
                                    Toast.makeText(CobrosActivity.this,
                                            "✅ Cobro actualizado", Toast.LENGTH_SHORT).show();
                                });
                            }
                            @Override public void onError(String e) {
                                runOnUiThread(() -> {
                                    btnGuardarCard.setEnabled(true);
                                    btnGuardar.setText("Guardar");
                                    Toast.makeText(CobrosActivity.this,
                                            "Error: " + e, Toast.LENGTH_SHORT).show();
                                });
                            }
                        });
            } else {
                // ── CREAR ─────────────────────────────────────────
                final double importeFinal = importe;
                SupabaseRepository.get().crearCobro(
                        clienteIdSel[0], nombre, concepto, importe,
                        metodoSel[0], estadoSel[0], notas,
                        new SupabaseRepository.Callback<CobroModel>() {
                            @Override public void onSuccess(CobroModel data) {
                                runOnUiThread(() -> {
                                    todosCobros.add(0, new Cobro(data));
                                    sheet.dismiss();
                                    ocultarTeclado();
                                    renderTodo();
                                    Toast.makeText(CobrosActivity.this,
                                            "✅ Cobro registrado", Toast.LENGTH_SHORT).show();
                                });
                            }
                            @Override public void onError(String e) {
                                runOnUiThread(() -> {
                                    btnGuardarCard.setEnabled(true);
                                    btnGuardar.setText("Guardar");
                                    Toast.makeText(CobrosActivity.this,
                                            "Error: " + e, Toast.LENGTH_SHORT).show();
                                });
                            }
                        });
            }
        });

        sheet.show();
    }

    // ════════════════════════════════════════════════════════════════
    //  BOTONES Y NAV
    // ════════════════════════════════════════════════════════════════
    private void setupBotones() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnNuevoCobro).setOnClickListener(v -> showNuevoCobroSheet(null));
    }

    private void setupBottomNav() {
        NavHelper.setup(this, "cobros");
    }

    // ════════════════════════════════════════════════════════════════
    //  HELPERS
    // ════════════════════════════════════════════════════════════════
    private String getMetodoEmoji(String metodo) {
        switch (metodo) {
            case "Efectivo":      return "💵 Efectivo";
            case "Tarjeta":       return "💳 Tarjeta";
            case "Bizum":         return "📱 Bizum";
            case "Transferencia": return "🏦 Transferencia";
            default:              return metodo;
        }
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

    // ── Cargar pool de clientes para autocompletar ────────────────
    private void cargarClientes() {
        SupabaseRepository.get().getClientes(null,
                new SupabaseRepository.Callback<List<ClienteModel>>() {
                    @Override public void onSuccess(List<ClienteModel> data) {
                        todosLosClientes.clear();
                        todosLosClientes.addAll(data);
                    }
                    @Override public void onError(String e) {}
                });
    }

    // ── Buscador de clientes en el formulario de cobro ────────────
    private void setupBuscadorClientes(android.widget.EditText etCliente,
                                       android.widget.LinearLayout layoutSug,
                                       String[] clienteIdSel, boolean[] seleccionando) {
        etCliente.addTextChangedListener(new android.text.TextWatcher() {
            private final android.os.Handler h = new android.os.Handler();
            private Runnable r;
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            public void onTextChanged(CharSequence s, int a, int b, int c) {
                if (seleccionando[0]) return;
                clienteIdSel[0] = null;
                if (r != null) h.removeCallbacks(r);
            }
            public void afterTextChanged(android.text.Editable s) {
                if (seleccionando[0]) { seleccionando[0] = false; return; }
                String txt = s.toString().trim();
                if (txt.length() < 2) {
                    layoutSug.setVisibility(android.view.View.GONE);
                    layoutSug.removeAllViews(); return;
                }
                r = () -> mostrarSugerenciasClientes(txt, layoutSug, etCliente,
                        clienteIdSel, seleccionando);
                h.postDelayed(r, 300);
            }
        });
    }

    private void mostrarSugerenciasClientes(String texto, android.widget.LinearLayout layoutSug,
                                            android.widget.EditText etCliente,
                                            String[] clienteIdSel, boolean[] seleccionando) {
        layoutSug.removeAllViews();
        String q = texto.toLowerCase();
        List<ClienteModel> hits = new ArrayList<>();
        for (ClienteModel c : todosLosClientes) {
            if (hits.size() >= 5) break;
            String nom = (c.nombre != null ? c.nombre : "") + " " +
                    (c.apellidos != null ? c.apellidos : "");
            String tel  = c.telefono != null ? c.telefono : "";
            if (nom.toLowerCase().contains(q) || tel.contains(q)) hits.add(c);
        }
        if (hits.isEmpty()) { layoutSug.setVisibility(android.view.View.GONE); return; }

        androidx.cardview.widget.CardView card = new androidx.cardview.widget.CardView(this);
        card.setLayoutParams(new android.widget.LinearLayout.LayoutParams(-1, -2));
        card.setRadius(dpToPx(12)); card.setCardElevation(dpToPx(4));
        card.setCardBackgroundColor(Color.WHITE);
        android.widget.LinearLayout inner = new android.widget.LinearLayout(this);
        inner.setOrientation(android.widget.LinearLayout.VERTICAL);
        card.addView(inner);

        for (int i = 0; i < hits.size(); i++) {
            ClienteModel c = hits.get(i);
            String nom = ((c.nombre != null ? c.nombre : "") + " " +
                    (c.apellidos != null ? c.apellidos : "")).trim();
            if (nom.isEmpty()) nom = "Cliente";
            String tel = c.telefono != null ? c.telefono : "";

            android.widget.LinearLayout fila = new android.widget.LinearLayout(this);
            fila.setOrientation(android.widget.LinearLayout.HORIZONTAL);
            fila.setGravity(android.view.Gravity.CENTER_VERTICAL);
            fila.setPadding(dpToPx(14), dpToPx(12), dpToPx(14), dpToPx(12));
            fila.setClickable(true); fila.setFocusable(true);

            // Avatar
            androidx.cardview.widget.CardView av = new androidx.cardview.widget.CardView(this);
            android.widget.LinearLayout.LayoutParams avP = new android.widget.LinearLayout.LayoutParams(dpToPx(34), dpToPx(34));
            avP.setMarginEnd(dpToPx(10)); av.setLayoutParams(avP);
            av.setRadius(dpToPx(10)); av.setCardElevation(0);
            av.setCardBackgroundColor(Color.parseColor("#0A66FF"));
            android.widget.TextView tvI = new android.widget.TextView(this);
            tvI.setText(nom.isEmpty() ? "?" : String.valueOf(nom.charAt(0)).toUpperCase());
            tvI.setTextSize(13f); tvI.setTextColor(Color.WHITE);
            tvI.setGravity(android.view.Gravity.CENTER);
            tvI.setTypeface(getResources().getFont(R.font.outfit_bold));
            tvI.setLayoutParams(new android.widget.LinearLayout.LayoutParams(-1, -1));
            av.addView(tvI); fila.addView(av);

            android.widget.LinearLayout info = new android.widget.LinearLayout(this);
            info.setOrientation(android.widget.LinearLayout.VERTICAL);
            info.setLayoutParams(new android.widget.LinearLayout.LayoutParams(0, -2, 1f));
            android.widget.TextView tvN = new android.widget.TextView(this);
            tvN.setText(nom); tvN.setTextSize(13f); tvN.setTextColor(Color.parseColor("#0D1B3E"));
            tvN.setTypeface(getResources().getFont(R.font.outfit_bold));
            info.addView(tvN);
            if (!tel.isEmpty()) {
                android.widget.TextView tvT = new android.widget.TextView(this);
                tvT.setText(tel); tvT.setTextSize(11f); tvT.setTextColor(Color.parseColor("#6B7FA3"));
                tvT.setTypeface(getResources().getFont(R.font.outfit_regular));
                info.addView(tvT);
            }
            fila.addView(info);

            final String idF = c.id, nomF = nom;
            fila.setOnClickListener(vv -> {
                seleccionando[0] = true;
                clienteIdSel[0]  = idF;
                etCliente.setText(nomF);
                etCliente.setSelection(nomF.length());
                layoutSug.setVisibility(android.view.View.GONE);
                layoutSug.removeAllViews();
                android.view.inputmethod.InputMethodManager imm =
                        (android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(etCliente.getWindowToken(), 0);
            });
            inner.addView(fila);
            if (i < hits.size() - 1) {
                android.view.View sep = new android.view.View(this);
                android.widget.LinearLayout.LayoutParams sp =
                        new android.widget.LinearLayout.LayoutParams(-1, dpToPx(1));
                sp.setMargins(dpToPx(14), 0, dpToPx(14), 0); sep.setLayoutParams(sp);
                sep.setBackgroundColor(Color.parseColor("#E5EDFF")); inner.addView(sep);
            }
        }
        layoutSug.addView(card);
        layoutSug.setVisibility(android.view.View.VISIBLE);
    }

}