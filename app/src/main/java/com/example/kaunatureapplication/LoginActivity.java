package com.example.kaunatureapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail;
    private EditText etPassword;
    private Button btnLogin;
    private TextView tvForgotPassword;
    private TextView tvRegister;

    private View logoSection;
    private CardView loginCard;
    private View footerSection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        tvRegister = findViewById(R.id.tvRegister);

        logoSection = findViewById(R.id.logoSection);
        loginCard = findViewById(R.id.loginCard);
        footerSection = findViewById(R.id.footerSection);

        animateEntrance();

        btnLogin.setOnClickListener(v -> hacerLogin());

        tvForgotPassword.setOnClickListener(v -> {
            Toast.makeText(this, "Funcionalidad próximamente", Toast.LENGTH_SHORT).show();
        });

        tvRegister.setOnClickListener(v -> {
            Toast.makeText(this, "Registro próximamente", Toast.LENGTH_SHORT).show();
        });

        etPassword.setOnEditorActionListener((v, actionId, event) -> {
            hacerLogin();
            return true;
        });
    }

    private void animateEntrance() {
        AlphaAnimation fadeIn = new AlphaAnimation(0f, 1f);
        fadeIn.setDuration(800);
        fadeIn.setInterpolator(new DecelerateInterpolator());
        logoSection.startAnimation(fadeIn);

        TranslateAnimation slideUp = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF, 0f,
                Animation.RELATIVE_TO_SELF, 0f,
                Animation.RELATIVE_TO_SELF, 0.15f,
                Animation.RELATIVE_TO_SELF, 0f
        );
        slideUp.setDuration(900);
        slideUp.setStartOffset(200);
        slideUp.setInterpolator(new DecelerateInterpolator());

        AlphaAnimation fadeInCard = new AlphaAnimation(0f, 1f);
        fadeInCard.setDuration(900);
        fadeInCard.setStartOffset(200);

        loginCard.startAnimation(slideUp);
        loginCard.startAnimation(fadeInCard);

        AlphaAnimation fadeInFooter = new AlphaAnimation(0f, 1f);
        fadeInFooter.setDuration(600);
        fadeInFooter.setStartOffset(500);
        fadeInFooter.setInterpolator(new DecelerateInterpolator());
        footerSection.startAnimation(fadeInFooter);
    }

    private void hacerLogin() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty()) {
            etEmail.setError("Introduce el email");
            etEmail.requestFocus();
            shakeView(etEmail);
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Email no válido");
            etEmail.requestFocus();
            shakeView(etEmail);
            return;
        }

        if (password.isEmpty()) {
            etPassword.setError("Introduce la contraseña");
            etPassword.requestFocus();
            shakeView(etPassword);
            return;
        }

        if (password.length() < 6) {
            etPassword.setError("La contraseña debe tener al menos 6 caracteres");
            etPassword.requestFocus();
            shakeView(etPassword);
            return;
        }

        btnLogin.setEnabled(false);
        btnLogin.setText("Iniciando sesión...");

        new AuthRepository().login(this, email, password, new AuthRepository.AuthCallback() {

            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    btnLogin.setText("✓ Sesión iniciada");

                    btnLogin.postDelayed(() -> {
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        startActivity(intent);
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                        finish();
                    }, 400);
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    btnLogin.setEnabled(true);
                    btnLogin.setText("Iniciar Sesión");

                    String msg = error;
                    if (error.contains("Invalid login credentials") ||
                            error.contains("invalid_grant")) {
                        msg = "Email o contraseña incorrectos";
                        shakeView(loginCard);
                    } else if (error.contains("Email not confirmed")) {
                        msg = "Por favor, confirma tu email antes de iniciar sesión";
                    } else if (error.contains("User not found")) {
                        msg = "No existe una cuenta con este email";
                    } else if (error.contains("Sin conexión")) {
                        msg = "No hay conexión a internet";
                    }

                    Toast.makeText(LoginActivity.this,
                            "❌ " + msg, Toast.LENGTH_LONG).show();

                    android.util.Log.e("LOGIN", "Error: " + error);
                });
            }
        });
    }

    private void shakeView(View view) {
        TranslateAnimation shake = new TranslateAnimation(0, 10, 0, 0);
        shake.setDuration(50);
        shake.setRepeatCount(5);
        shake.setRepeatMode(Animation.REVERSE);
        view.startAnimation(shake);
    }
}