package com.pokemones.pokemonbuilder.ui;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.pokemones.pokemonbuilder.R;
import com.pokemones.pokemonbuilder.data.AppDbHelper;

public class RegisterActivity extends AppCompatActivity {
    private AppDbHelper db;
    private EditText etUser, etPass;
    private Button btnCreate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        db = new AppDbHelper(this);

        etUser = findViewById(R.id.etUser);
        etPass = findViewById(R.id.etPass);
        btnCreate = findViewById(R.id.btnCreate);

        btnCreate.setOnClickListener(v -> {
            String u = etUser.getText().toString().trim();
            String p = etPass.getText().toString().trim();
            if (u.isEmpty() || p.isEmpty()) {
                Toast.makeText(this, "Completa los campos", Toast.LENGTH_SHORT).show();
                return;
            }
            long id = db.createUser(u, p);
            if (id > 0) {
                Toast.makeText(this, "Usuario creado", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "Error: usuario puede existir", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
