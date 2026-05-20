package com.example.kaunatureapplication;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Cliente Retrofit para Supabase REST.
 * Siempre lee el token de SessionManager en cada petición (no lo cachea).
 * reset() destruye el singleton para forzar recreación con nuevo token.
 */
public class SupabaseClient {

    private static Retrofit retrofit = null;

    public static Retrofit get() {
        if (retrofit == null) build();
        return retrofit;
    }

    /** Llamar tras login o refresh para que el nuevo token se use inmediatamente */
    public static void reset() {
        retrofit = null;
        // También resetear el Repository singleton
        SupabaseRepository.reset();
    }

    private static void build() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(logging)
                .addInterceptor(chain -> {
                    // Lee el token SIEMPRE en el momento de la petición — nunca cacheado
                    String token = SessionManager.getToken();

                    okhttp3.Request request = chain.request().newBuilder()
                            .addHeader("apikey",        SupabaseConfig.API_KEY)
                            .addHeader("Authorization", "Bearer " + token)
                            .addHeader("Content-Type",  "application/json")
                            .addHeader("Prefer",        "return=representation")
                            .build();

                    okhttp3.Response response = chain.proceed(request);

                    // Si 401 → el token expiró mientras se hacía la llamada
                    // (el flujo de refresh se maneja en BaseActivity)
                    return response;
                })
                .build();

        retrofit = new Retrofit.Builder()
                .baseUrl(SupabaseConfig.URL + "/rest/v1/")
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build();
    }
}