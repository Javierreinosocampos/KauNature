
package com.example.kaunatureapplication;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class SupabaseClient {

    private static Retrofit retrofit = null;

    public static Retrofit get() {
        if (retrofit == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY); // quitar en producción

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .addInterceptor(chain -> {
                        // Supabase necesita estos headers en TODAS las peticiones
                        okhttp3.Request request = chain.request().newBuilder()
                                .addHeader("apikey", SupabaseConfig.API_KEY)
                                .addHeader("Authorization", "Bearer " + SupabaseConfig.API_KEY)
                                .addHeader("Content-Type", "application/json")
                                .addHeader("Prefer", "return=representation") // para que POST devuelva el objeto creado
                                .build();
                        return chain.proceed(request);
                    })
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(SupabaseConfig.URL + "/rest/v1/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(client)
                    .build();
        }
        return retrofit;
    }
}