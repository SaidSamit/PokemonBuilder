package com.pokemones.pokemonbuilder.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.pokemones.pokemonbuilder.R;
import com.pokemones.pokemonbuilder.data.AppDbHelper;
import com.pokemones.pokemonbuilder.models.User;
import com.pokemones.pokemonbuilder.utils.PermissionUtils;

public class LoginActivity extends AppCompatActivity {
    private AppDbHelper db;
    private EditText etUser, etPass;
    private Button btnLogin, btnRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Instancia DB
        db = new AppDbHelper(this);

        // Pedimos permisos críticos al iniciar la app (cámara y audio)
        PermissionUtils.requestCameraAndAudioPermissions(this);

        // Vínculos con la UI
        etUser = findViewById(R.id.etUser);
        etPass = findViewById(R.id.etPass);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);

        // Botón registrar: abre RegisterActivity (debe existir en tu proyecto)
        btnRegister.setOnClickListener(v -> {
            Intent i = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(i);
        });

        // Botón login: valida credenciales usando AppDbHelper.login(...)
        btnLogin.setOnClickListener(v -> {
            String username = etUser.getText().toString().trim();
            String password = etPass.getText().toString().trim();

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(LoginActivity.this, "Ingresa usuario y contraseña", Toast.LENGTH_SHORT).show();
                return;
            }

            User user = db.login(username, password);
            if (user != null) {
                // Login correcto: abrir MainMenuActivity pasando userId usando el getter
                Intent i = new Intent(LoginActivity.this, MainMenuActivity.class);
                i.putExtra("userId", user.getId());
                startActivity(i);
                finish();
            } else {
                Toast.makeText(LoginActivity.this, "Usuario o contraseña incorrectos", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        // Delegamos el manejo simple a PermissionUtils (muestra toast si faltan permisos)
        PermissionUtils.handlePermissionsResult(this, requestCode, permissions, grantResults);
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
