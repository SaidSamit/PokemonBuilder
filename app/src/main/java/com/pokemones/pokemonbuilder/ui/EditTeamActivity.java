package com.pokemones.pokemonbuilder.ui;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.pokemones.pokemonbuilder.R;
import com.pokemones.pokemonbuilder.data.AppDbHelper;
import com.pokemones.pokemonbuilder.models.TeamPokemon;
import com.pokemones.pokemonbuilder.utils.ImageUtils;

import java.util.List;

public class EditTeamActivity extends AppCompatActivity {
    private AppDbHelper db;
    private EditText etTeamName;
    private ImageButton[] slotButtons = new ImageButton[6];
    private long teamId;
    private List<TeamPokemon> pokemons;

    public static final int REQ_SELECT_POKEMON = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_team);

        db = new AppDbHelper(this);
        etTeamName = findViewById(R.id.etTeamName);

        slotButtons[0] = findViewById(R.id.slot0);
        slotButtons[1] = findViewById(R.id.slot1);
        slotButtons[2] = findViewById(R.id.slot2);
        slotButtons[3] = findViewById(R.id.slot3);
        slotButtons[4] = findViewById(R.id.slot4);
        slotButtons[5] = findViewById(R.id.slot5);

        teamId = getIntent().getLongExtra("teamId", -1);

        for (int i = 0; i < 6; i++) {
            final int slot = i;
            slotButtons[i].setOnClickListener(v -> {
                Intent intent = new Intent(this, PokedexActivity.class);
                intent.putExtra("teamId", teamId);
                intent.putExtra("slot", slot);
                intent.putExtra("fromTeamEdit", true);
                startActivityForResult(intent, REQ_SELECT_POKEMON);
            });
        }

        Button btnSave = findViewById(R.id.btnSaveTeam);
        btnSave.setOnClickListener(v -> {
            // Guardar nombre del equipo (si quieres persistir nombre, añade método update en DB)
            Toast.makeText(this, "Equipo guardado (nombre local)", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        pokemons = db.getTeamPokemons(teamId);
        // mostrar sprites si existen
        for (TeamPokemon tp : pokemons) {
            if (tp.slot >= 0 && tp.slot < 6) {
                ImageButton ib = slotButtons[tp.slot];
                if (tp.customSpritePath != null && !tp.customSpritePath.isEmpty()) {
                    Bitmap b = ImageUtils.loadBitmapFromPath(tp.customSpritePath);
                    if (b != null) ib.setImageBitmap(b);
                } else if (tp.pokemonName != null) {
                    // intenta cargar sprite desde PokeAPI (descarga simple)
                    ImageUtils.loadImageIntoButtonFromUrl(this, ib, "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/" + tp.pokemonName + ".png");
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_SELECT_POKEMON && resultCode == RESULT_OK) {
            String pokemonName = data.getStringExtra("selectedPokemon");
            int slot = data.getIntExtra("slot", -1);
            if (slot >= 0) {
                TeamPokemon tp = db.getTeamPokemonBySlot(teamId, slot);
                if (tp == null) tp = new TeamPokemon();
                tp.teamId = teamId;
                tp.slot = slot;
                tp.pokemonName = pokemonName;
                db.saveTeamPokemon(tp);
                Toast.makeText(this, "Pokémon asignado: " + pokemonName, Toast.LENGTH_SHORT).show();
            }
        }
    }
}
