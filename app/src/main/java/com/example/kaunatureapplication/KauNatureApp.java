package com.example.kaunatureapplication;

import android.app.Application;

public class KauNatureApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Aplicar el tema guardado al arrancar la app
        SettingsActivity.applyStoredTheme(this);
    }
}