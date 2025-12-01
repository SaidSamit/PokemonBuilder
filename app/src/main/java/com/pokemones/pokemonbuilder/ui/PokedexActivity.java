package com.pokemones.pokemonbuilder.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.pokemones.pokemonbuilder.R;
import com.pokemones.pokemonbuilder.api.PokeApiClient;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

public class PokedexActivity extends AppCompatActivity {
    private static final String TAG = "PokedexActivity";

    private ListView lv;
    private List<String> names = new ArrayList<>();
    private boolean fromTeamEdit = false;
    private int slot = -1;
    private long teamId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pokedex);

        lv = findViewById(R.id.lvPokedex);

        fromTeamEdit = getIntent().getBooleanExtra("fromTeamEdit", false);
        slot = getIntent().getIntExtra("slot", -1);
        teamId = getIntent().getLongExtra("teamId", -1);

        Log.d(TAG, "onCreate fromTeamEdit=" + fromTeamEdit + " slot=" + slot + " teamId=" + teamId);

        // Cargar lista en background
        new Thread(() -> {
            try {
                JSONArray arr = PokeApiClient.getAllPokemonNames();
                if (arr != null) {
                    for (int i = 0; i < arr.length(); i++) names.add(arr.optString(i));
                } else {
                    names.add("bulbasaur");
                    names.add("charmander");
                    names.add("squirtle");
                }
            } catch (Exception e) {
                names.clear();
                names.add("bulbasaur");
                names.add("charmander");
                names.add("squirtle");
            }
            runOnUiThread(() -> {
                ArrayAdapter<String> a = new ArrayAdapter<>(PokedexActivity.this, android.R.layout.simple_list_item_1, names);
                lv.setAdapter(a);
            });
        }).start();

        lv.setOnItemClickListener((parent, view, position, id) -> {
            String selected = names.get(position);
            Log.d(TAG, "Seleccionado: " + selected + " fromTeamEdit=" + fromTeamEdit + " slot=" + slot + " teamId=" + teamId);

            if (fromTeamEdit) {
                Intent res = new Intent();
                res.putExtra("selectedPokemon", selected);
                res.putExtra("slot", slot);
                res.putExtra("teamId", teamId);
                setResult(RESULT_OK, res);
                finish();
                return;
            }

            Intent i = new Intent(PokedexActivity.this, EditPokemonActivity.class);
            i.putExtra("pokemonName", selected);
            startActivity(i);
        });
    }
}
