package com.example.kaunatureapplication;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.DecelerateInterpolator;

import androidx.appcompat.app.AppCompatActivity;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DURATION = 2800;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        );

        setContentView(R.layout.activity_splash);

        View centerBlock = findViewById(R.id.centerBlock);
        View bottomBlock = findViewById(R.id.bottomBlock);

        centerBlock.setVisibility(View.INVISIBLE);
        bottomBlock.setVisibility(View.INVISIBLE);

        // Fade-in centerBlock
        centerBlock.postDelayed(() -> {
            centerBlock.setVisibility(View.VISIBLE);
            AlphaAnimation fadeInCenter = new AlphaAnimation(0f, 1f);
            fadeInCenter.setDuration(900);
            fadeInCenter.setInterpolator(new DecelerateInterpolator());
            fadeInCenter.setFillAfter(true);
            centerBlock.startAnimation(fadeInCenter);
        }, 200);

        // Fade-in bottomBlock
        bottomBlock.postDelayed(() -> {
            bottomBlock.setVisibility(View.VISIBLE);
            AlphaAnimation fadeInBottom = new AlphaAnimation(0f, 1f);
            fadeInBottom.setDuration(600);
            fadeInBottom.setInterpolator(new DecelerateInterpolator());
            fadeInBottom.setFillAfter(true);
            bottomBlock.startAnimation(fadeInBottom);
        }, 900);

        // ── Navegar tras la animación ─────────────────────────────
        new Handler(Looper.getMainLooper()).postDelayed(() -> {

            // Cargar sesión guardada
            SessionManager.loadSession(this);

            if (SessionManager.isLoggedIn()) {
                if (SessionManager.isTokenExpired()) {
                    // Token expirado → intentar refresh silencioso
                    new AuthRepository().refreshToken(this, new AuthRepository.AuthCallback() {
                        @Override public void onSuccess() {
                            goToMain(); // refresh ok → entrar
                        }
                        @Override public void onError(String error) {
                            // Refresh falló (sesión muy antigua) → login
                            goToLogin();
                        }
                    });
                } else {
                    goToMain(); // token vigente → entrar directamente
                }
            } else {
                goToLogin(); // sin sesión → login
            }

        }, SPLASH_DURATION);
    }

    private void goToMain() {
        startActivity(new Intent(this, MainActivity.class));
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    private void goToLogin() {
        startActivity(new Intent(this, LoginActivity.class));
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }
}