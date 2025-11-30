package com.pokemones.pokemonbuilder.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.pokemones.pokemonbuilder.R;
import com.pokemones.pokemonbuilder.data.AppDbHelper;
import com.pokemones.pokemonbuilder.models.User;

public class LoginActivity extends AppCompatActivity {
    private AppDbHelper db;
    private EditText etUser, etPass;
    private Button btnLogin, btnRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        db = new AppDbHelper(this);

        etUser = findViewById(R.id.etUser);
        etPass = findViewById(R.id.etPass);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);

        btnRegister.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));

        btnLogin.setOnClickListener(v -> {
            String u = etUser.getText().toString().trim();
            String p = etPass.getText().toString().trim();
            if (u.isEmpty() || p.isEmpty()) {
                Toast.makeText(this, "Ingresa usuario y contraseña", Toast.LENGTH_SHORT).show();
                return;
            }
            User user = db.login(u, p);
            if (user != null) {
                Intent i = new Intent(this, MainMenuActivity.class);
                i.putExtra("userId", user.id);
                startActivity(i);
                finish();
            } else {
                Toast.makeText(this, "Credenciales inválidas", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
