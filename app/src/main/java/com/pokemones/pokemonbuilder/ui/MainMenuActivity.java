package com.pokemones.pokemonbuilder.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.pokemones.pokemonbuilder.R;

public class MainMenuActivity extends AppCompatActivity {
    private Button btnTeams, btnPokedex;
    private long userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);

        btnTeams = findViewById(R.id.btnTeams);
        btnPokedex = findViewById(R.id.btnPokedex);

        userId = getIntent().getLongExtra("userId", -1);

        btnTeams.setOnClickListener(v -> {
            Intent i = new Intent(this, TeamsActivity.class);
            i.putExtra("userId", userId);
            startActivity(i);
        });

        btnPokedex.setOnClickListener(v -> {
            Intent i = new Intent(this, PokedexActivity.class);
            // fromPokedexButton true indica que no se puede asignar a equipo desde aquí
            i.putExtra("fromPokedexButton", true);
            startActivity(i);
        });
    }
}
