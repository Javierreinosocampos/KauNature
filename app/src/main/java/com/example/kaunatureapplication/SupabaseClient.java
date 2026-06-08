package com.example.kaunatureapplication;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class SupabaseClient {

    private static Retrofit retrofit = null;

    public static Retrofit get() {
        if (retrofit == null) {
            retrofit = build();
        }
        return retrofit;
    }

    public static synchronized void reset() {
        retrofit = null;
        SupabaseRepository.reset();
    }

    private static Retrofit build() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(logging)
                .addInterceptor(chain -> {
                    String token = SessionManager.getToken();

                    okhttp3.Request original = chain.request();
                    okhttp3.Request request = original.newBuilder()
                            .header("apikey",        SupabaseConfig.API_KEY)
                            .header("Authorization", "Bearer " + token)
                            .header("Content-Type",  "application/json")
                            .header("Prefer", "return=representation,resolution=merge-duplicates")
                            .build();

                    okhttp3.Response response = chain.proceed(request);

                    android.util.Log.d("SUPABASE_AUTH",
                            "Token usado: " + (token.isEmpty() ? "VACÍO" : token.substring(0, Math.min(20, token.length())) + "...") +
                                    " | URL: " + original.url() +
                                    " | Status: " + response.code());

                    return response;
                })
                .build();

        return new Retrofit.Builder()
                .baseUrl(SupabaseConfig.URL + "/rest/v1/")
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build();
    }
}