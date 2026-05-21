package com.example.kaunatureapplication;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * NavHelper — 5 tabs: Inicio · Clientes · Gimnasio · Agenda · Cobros
 * Todos los tabs tienen el mismo estilo visual (icono + label).
 * Uso: NavHelper.setup(activity, "gimnasio");
 * Keys: "home" | "clientes" | "gimnasio" | "agenda" | "cobros"
 */
public class NavHelper {
    public static final String COBRO_CAMBIADO = "com.example.kaunatureapplication.COBRO_CAMBIADO";
    public static final String CITA_CAMBIADA  = "com.example.kaunatureapplication.CITA_CAMBIADA";

    public static void setup(Activity activity, String activeKey) {
        LinearLayout navHome     = activity.findViewById(R.id.nav_home);
        LinearLayout navClientes = activity.findViewById(R.id.nav_clientes);
        LinearLayout navGimnasio = activity.findViewById(R.id.nav_gimnasio);
        LinearLayout navAgenda   = activity.findViewById(R.id.nav_agenda);
        LinearLayout navCobros   = activity.findViewById(R.id.nav_cobros);

        TextView labelHome     = activity.findViewById(R.id.navLabelHome);
        TextView labelClientes = activity.findViewById(R.id.navLabelClientes);
        TextView labelGimnasio = activity.findViewById(R.id.navLabelGimnasio);
        TextView labelAgenda   = activity.findViewById(R.id.navLabelAgenda);
        TextView labelCobros   = activity.findViewById(R.id.navLabelCobros);

        // Reset todos inactivos
        if (navHome     != null) setInactive(navHome,     labelHome);
        if (navClientes != null) setInactive(navClientes, labelClientes);
        if (navGimnasio != null) setInactive(navGimnasio, labelGimnasio);
        if (navAgenda   != null) setInactive(navAgenda,   labelAgenda);
        if (navCobros   != null) setInactive(navCobros,   labelCobros);

        // Activar tab seleccionado
        switch (activeKey) {
            case "home":     if (navHome     != null) setActive(navHome,     labelHome);     break;
            case "clientes": if (navClientes != null) setActive(navClientes, labelClientes); break;
            case "gimnasio": if (navGimnasio != null) setActive(navGimnasio, labelGimnasio); break;
            case "agenda":   if (navAgenda   != null) setActive(navAgenda,   labelAgenda);   break;
            case "cobros":   if (navCobros   != null) setActive(navCobros,   labelCobros);   break;
        }

        // ── Listeners ──────────────────────────────────────────────
        if (navHome != null) navHome.setOnClickListener(v -> {
            if (!activeKey.equals("home")) {
                activity.startActivity(new Intent(activity, MainActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
                activity.overridePendingTransition(0, 0);
            }
        });

        if (navClientes != null) navClientes.setOnClickListener(v -> {
            if (!activeKey.equals("clientes")) {
                activity.startActivity(new Intent(activity, ClientesActivity.class));
                activity.overridePendingTransition(0, 0);
                activity.finish();
            }
        });

        if (navGimnasio != null) navGimnasio.setOnClickListener(v -> {
            if (!activeKey.equals("gimnasio")) {
                activity.startActivity(new Intent(activity, GimnasioActivity.class));
                activity.overridePendingTransition(0, 0);
                activity.finish();
            }
        });

        if (navAgenda != null) navAgenda.setOnClickListener(v -> {
            if (!activeKey.equals("agenda")) {
                activity.startActivity(new Intent(activity, AgendaActivity.class));
                activity.overridePendingTransition(0, 0);
                activity.finish();
            }
        });

        if (navCobros != null) navCobros.setOnClickListener(v -> {
            if (!activeKey.equals("cobros")) {
                activity.startActivity(new Intent(activity, CobrosActivity.class));
                activity.overridePendingTransition(0, 0);
                activity.finish();
            }
        });
    }

    private static void setActive(LinearLayout tab, TextView label) {
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setColor(Color.parseColor("#0A66FF"));
        bg.setCornerRadius(999);
        tab.setBackground(bg);
        if (label != null) {
            label.setTextColor(Color.WHITE);
            label.setTypeface(Typeface.DEFAULT_BOLD);
        }
    }

    private static void setInactive(LinearLayout tab, TextView label) {
        tab.setBackgroundColor(Color.TRANSPARENT);
        if (label != null) {
            label.setTextColor(Color.parseColor("#6B7FA3"));
            label.setTypeface(Typeface.DEFAULT);
        }
    }
}