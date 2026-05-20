package com.example.kaunatureapplication;

import android.content.Context;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.*;

/**
 * Gestiona login y refresh de token con Supabase Auth.
 */
public class AuthRepository {

    public interface AuthCallback {
        void onSuccess();
        void onError(String error);
    }

    // ── LOGIN ─────────────────────────────────────────────────────
    public void login(Context context, String email, String password, AuthCallback cb) {
        OkHttpClient client = new OkHttpClient();
        try {
            JSONObject json = new JSONObject();
            json.put("email",    email);
            json.put("password", password);

            Request request = new Request.Builder()
                    .url(SupabaseConfig.URL + "/auth/v1/token?grant_type=password")
                    .addHeader("apikey",       SupabaseConfig.API_KEY)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(json.toString(),
                            MediaType.parse("application/json")))
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override public void onFailure(Call call, IOException e) {
                    cb.onError("Sin conexión: " + e.getMessage());
                }
                @Override public void onResponse(Call call, Response response) throws IOException {
                    String res = response.body().string();
                    android.util.Log.d("AUTH", "Login response " + response.code() + ": " + res);
                    if (response.isSuccessful()) {
                        try {
                            JSONObject obj  = new JSONObject(res);
                            String token    = obj.getString("access_token");
                            String refresh  = obj.optString("refresh_token", "");
                            String uid      = obj.getJSONObject("user").getString("id");
                            long expiresIn  = obj.optLong("expires_in", 3600); // segundos
                            long expiresAt  = System.currentTimeMillis() / 1000L + expiresIn;

                            SessionManager.saveSession(context, token, refresh, uid, expiresAt);

                            // Resetear el singleton de Retrofit para que use el nuevo token
                            SupabaseClient.reset();

                            cb.onSuccess();
                        } catch (Exception e) {
                            cb.onError("Parse error: " + e.getMessage());
                        }
                    } else {
                        // Mensaje legible del error de Supabase
                        String msg = res;
                        try {
                            JSONObject err = new JSONObject(res);
                            msg = err.optString("error_description",
                                    err.optString("message", res));
                        } catch (Exception ignored) {}
                        cb.onError(msg);
                    }
                }
            });
        } catch (Exception e) {
            cb.onError(e.getMessage());
        }
    }

    // ── REFRESH TOKEN ─────────────────────────────────────────────
    public void refreshToken(Context context, AuthCallback cb) {
        String refresh = SessionManager.getRefreshToken();
        if (refresh.isEmpty()) { cb.onError("Sin refresh token"); return; }

        OkHttpClient client = new OkHttpClient();
        try {
            JSONObject json = new JSONObject();
            json.put("refresh_token", refresh);

            Request request = new Request.Builder()
                    .url(SupabaseConfig.URL + "/auth/v1/token?grant_type=refresh_token")
                    .addHeader("apikey",       SupabaseConfig.API_KEY)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(json.toString(),
                            MediaType.parse("application/json")))
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override public void onFailure(Call call, IOException e) {
                    cb.onError(e.getMessage());
                }
                @Override public void onResponse(Call call, Response response) throws IOException {
                    String res = response.body().string();
                    if (response.isSuccessful()) {
                        try {
                            JSONObject obj = new JSONObject(res);
                            String token   = obj.getString("access_token");
                            String newRef  = obj.optString("refresh_token", refresh);
                            long expiresIn = obj.optLong("expires_in", 3600);
                            long expiresAt = System.currentTimeMillis() / 1000L + expiresIn;

                            SessionManager.updateToken(context, token, expiresAt);
                            // Guardar también el nuevo refresh_token
                            SessionManager.saveSession(context, token, newRef,
                                    SessionManager.getUserId(), expiresAt);
                            SupabaseClient.reset();
                            cb.onSuccess();
                        } catch (Exception e) {
                            cb.onError(e.getMessage());
                        }
                    } else {
                        // Refresh inválido → forzar logout
                        SessionManager.clear(context);
                        cb.onError("Sesión expirada");
                    }
                }
            });
        } catch (Exception e) {
            cb.onError(e.getMessage());
        }
    }
}