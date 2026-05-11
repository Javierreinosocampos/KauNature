package com.example.kaunatureapplication;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.cardview.widget.CardView;

public class SettingsActivity extends AppCompatActivity {

    private static final String PREFS_NAME  = "kau_prefs";
    private static final String KEY_THEME   = "theme_mode"; // "light" | "dark" | "system"

    private CardView cardLight, cardDark, cardSystem;
    private TextView tvLightCheck, tvDarkCheck, tvSystemCheck;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        bindViews();
        setupTopBar();
        setupThemeCards();
        highlightCurrentTheme();
    }

    // ─────────────────────────────────────────────
    //  BIND
    // ─────────────────────────────────────────────
    private void bindViews() {
        cardLight    = findViewById(R.id.cardThemeLight);
        cardDark     = findViewById(R.id.cardThemeDark);
        cardSystem   = findViewById(R.id.cardThemeSystem);
        tvLightCheck = findViewById(R.id.tvLightCheck);
        tvDarkCheck  = findViewById(R.id.tvDarkCheck);
        tvSystemCheck= findViewById(R.id.tvSystemCheck);
    }

    // ─────────────────────────────────────────────
    //  TOP BAR — botón atrás
    // ─────────────────────────────────────────────
    private void setupTopBar() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    // ─────────────────────────────────────────────
    //  TARJETAS DE TEMA
    // ─────────────────────────────────────────────
    private void setupThemeCards() {
        cardLight.setOnClickListener(v  -> applyTheme("light"));
        cardDark.setOnClickListener(v   -> applyTheme("dark"));
        cardSystem.setOnClickListener(v -> applyTheme("system"));
    }

    private void applyTheme(String mode) {
        // Guardar preferencia
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_THEME, mode).apply();

        // Aplicar modo
        switch (mode) {
            case "light":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case "dark":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case "system":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }

        highlightCurrentTheme();

        // Reiniciar MainActivity para que aplique el tema
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void highlightCurrentTheme() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String current = prefs.getString(KEY_THEME, "system");

        // Reset todos
        setCardSelected(cardLight,  tvLightCheck,  false);
        setCardSelected(cardDark,   tvDarkCheck,   false);
        setCardSelected(cardSystem, tvSystemCheck, false);

        // Marcar el activo
        switch (current) {
            case "light":  setCardSelected(cardLight,  tvLightCheck,  true); break;
            case "dark":   setCardSelected(cardDark,   tvDarkCheck,   true); break;
            case "system": setCardSelected(cardSystem, tvSystemCheck, true); break;
        }
    }

    private void setCardSelected(CardView card, TextView check, boolean selected) {
        if (selected) {
            card.setCardBackgroundColor(Color.parseColor("#0A66FF"));
            card.setCardElevation(dpToPx(6));
            check.setVisibility(View.VISIBLE);
        } else {
            // El color neutro cambia según el modo actual del sistema
            boolean isDark = (getResources().getConfiguration().uiMode
                    & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                    == android.content.res.Configuration.UI_MODE_NIGHT_YES;
            card.setCardBackgroundColor(isDark
                    ? Color.parseColor("#1E2A45")
                    : Color.parseColor("#F0F5FF"));
            card.setCardElevation(dpToPx(2));
            check.setVisibility(View.GONE);
        }
    }

    // ─────────────────────────────────────────────
    //  HELPER ESTÁTICO — llamar al arranque de la app
    // ─────────────────────────────────────────────
    public static void applyStoredTheme(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String mode = prefs.getString(KEY_THEME, "system");
        switch (mode) {
            case "light":  AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);           break;
            case "dark":   AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);          break;
            default:       AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);break;
        }
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}