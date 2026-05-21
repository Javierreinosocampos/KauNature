package com.example.kaunatureapplication;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Gestión de sesión persistente con access_token + refresh_token.
 * El access_token de Supabase dura 1 hora.
 * El refresh_token dura indefinidamente (hasta logout manual).
 */
public class SessionManager {

    private static final String PREF_NAME      = "auth_prefs";
    private static final String KEY_TOKEN      = "access_token";
    private static final String KEY_REFRESH    = "refresh_token";
    private static final String KEY_USER       = "user_id";
    private static final String KEY_EXPIRES_AT = "expires_at"; // epoch segundos

    private static String accessToken;
    private static String refreshToken;
    private static String userId;
    private static long   expiresAt;

    // ── Guardar sesión completa ───────────────────────────────────
    public static void saveSession(Context context, String token,
                                   String refresh, String id, long expiresAtSec) {
        accessToken  = token;
        refreshToken = refresh;
        userId       = id;
        expiresAt    = expiresAtSec;

        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_TOKEN,      token)
                .putString(KEY_REFRESH,    refresh)
                .putString(KEY_USER,       id)
                .putLong(KEY_EXPIRES_AT,   expiresAtSec)
                .apply();
    }

    // ── Cargar sesión al arrancar la app ──────────────────────────
    public static void loadSession(Context context) {
        SharedPreferences p = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        accessToken  = p.getString(KEY_TOKEN,   "");
        refreshToken = p.getString(KEY_REFRESH, "");
        userId       = p.getString(KEY_USER,    "");
        expiresAt    = p.getLong(KEY_EXPIRES_AT, 0);
    }

    // ── Getters ───────────────────────────────────────────────────
    public static String getToken()        { return accessToken  != null ? accessToken  : ""; }
    public static String getRefreshToken() { return refreshToken != null ? refreshToken : ""; }
    public static String getUserId()       { return userId       != null ? userId       : ""; }

    // ── ¿Sesión activa? ───────────────────────────────────────────
    public static boolean isLoggedIn() {
        return accessToken != null && !accessToken.isEmpty();
    }

    // ── ¿Token expirado? ─────────────────────────────────────────
    public static boolean isTokenExpired() {
        if (expiresAt == 0) return false; // sin info → asumir válido
        long nowSec = System.currentTimeMillis() / 1000L;
        return nowSec >= (expiresAt - 60); // margen de 60 seg
    }

    // ── Actualizar solo el access_token (tras refresh) ───────────
    public static void updateToken(Context context, String newToken, long newExpiresAt) {
        accessToken = newToken;
        expiresAt   = newExpiresAt;
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_TOKEN,    newToken)
                .putLong(KEY_EXPIRES_AT, newExpiresAt)
                .apply();
    }

    // ── Cerrar sesión ─────────────────────────────────────────────
    public static void clear(Context context) {
        accessToken  = "";
        refreshToken = "";
        userId       = "";
        expiresAt    = 0;
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit().clear().apply();
    }
}