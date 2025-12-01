package com.pokemones.pokemonbuilder.ui;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.pokemones.pokemonbuilder.R;
import com.pokemones.pokemonbuilder.data.AppDbHelper;
import com.pokemones.pokemonbuilder.models.TeamPokemon;
import com.pokemones.pokemonbuilder.models.Team;
import com.pokemones.pokemonbuilder.utils.ImageUtils;

import java.util.List;

public class EditTeamActivity extends AppCompatActivity {
    private AppDbHelper db;
    private EditText etTeamName;
    private ImageButton[] slotButtons = new ImageButton[6];
    private long teamId;
    private long userId;
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
        userId = getIntent().getLongExtra("userId", -1);

        // If no teamId provided, create a new team immediately to ensure valid teamId for assignments
        if (teamId == -1) {
            long created = db.createTeam(userId, "Equipo nuevo");
            if (created > 0) {
                teamId = created;
            } else {
                Toast.makeText(this, "No se pudo crear el equipo en la base de datos", Toast.LENGTH_SHORT).show();
            }
        } else {
            // Try to load team name if exists
            List<Team> teams = db.getTeamsForUser(userId);
            for (Team t : teams) {
                if (t.id == teamId && t.name != null) {
                    etTeamName.setText(t.name);
                    break;
                }
            }
        }

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
            String newName = etTeamName.getText().toString().trim();
            if (teamId > 0 && !newName.isEmpty()) {
                db.updateTeamName(teamId, newName);
                Toast.makeText(this, "Equipo guardado", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Nombre vacío o equipo no creado", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (teamId <= 0) return;
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
        if (requestCode == REQ_SELECT_POKEMON && resultCode == RESULT_OK && data != null) {
            String pokemonName = data.getStringExtra("selectedPokemon");
            int slot = data.getIntExtra("slot", -1);
            if (slot >= 0 && teamId > 0) {
                TeamPokemon tp = db.getTeamPokemonBySlot(teamId, slot);
                if (tp == null) tp = new TeamPokemon();
                tp.teamId = teamId;
                tp.slot = slot;
                tp.pokemonName = pokemonName;
                long savedId = db.saveTeamPokemon(tp);
                if (savedId > 0) tp.id = savedId;
                Toast.makeText(this, "Pokémon asignado: " + pokemonName, Toast.LENGTH_SHORT).show();
                // actualizar UI inmediatamente
                if (tp.customSpritePath != null && !tp.customSpritePath.isEmpty()) {
                    Bitmap b = ImageUtils.loadBitmapFromPath(tp.customSpritePath);
                    if (b != null) slotButtons[slot].setImageBitmap(b);
                } else {
                    ImageUtils.loadImageIntoButtonFromUrl(this, slotButtons[slot], "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/" + pokemonName + ".png");
                }
            }
        }
    }
}
