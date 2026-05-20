package com.example.kaunatureapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    private EditText    etEmail;
    private EditText    etPassword;
    private Button      btnLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etEmail     = findViewById(R.id.etEmail);
        etPassword  = findViewById(R.id.etPassword);
        btnLogin    = findViewById(R.id.btnLogin);

        // Sesión ya gestionada en SplashActivity — aquí solo mostramos el formulario
        btnLogin.setOnClickListener(v -> hacerLogin());
    }

    private void hacerLogin() {
        String email    = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty())    { etEmail.setError("Introduce el email");       return; }
        if (password.isEmpty()) { etPassword.setError("Introduce la contraseña"); return; }

        btnLogin.setEnabled(false);

        new AuthRepository().login(this, email, password, new AuthRepository.AuthCallback() {

            @Override public void onSuccess() {
                runOnUiThread(() -> {
                    // Sesión guardada en AuthRepository — ir al main
                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                    finish();
                });
            }

            @Override public void onError(String error) {
                runOnUiThread(() -> {
                    btnLogin.setEnabled(true);

                    // Mensaje claro al usuario
                    String msg = error;
                    if (error.contains("Invalid login credentials") ||
                            error.contains("invalid_grant")) {
                        msg = "Email o contraseña incorrectos";
                    } else if (error.contains("Email not confirmed")) {
                        msg = "Confirma tu email antes de entrar";
                    } else if (error.contains("User not found")) {
                        msg = "Usuario no encontrado";
                    }

                    Toast.makeText(LoginActivity.this,
                            "❌ " + msg, Toast.LENGTH_LONG).show();

                    android.util.Log.e("LOGIN", "Error: " + error);
                });
            }
        });
    }
}