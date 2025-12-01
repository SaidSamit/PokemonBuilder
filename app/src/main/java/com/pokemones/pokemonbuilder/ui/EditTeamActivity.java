package com.pokemones.pokemonbuilder.ui;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.pokemones.pokemonbuilder.R;
import com.pokemones.pokemonbuilder.api.PokeApiClient;
import com.pokemones.pokemonbuilder.data.DbProvider;
import com.pokemones.pokemonbuilder.data.AppDbHelper;
import com.pokemones.pokemonbuilder.models.TeamPokemon;
import com.pokemones.pokemonbuilder.utils.ImageUtils;

import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EditTeamActivity extends AppCompatActivity {
    private static final String TAG = "EditTeamActivity";
    private static final int REQ_EDIT_POKEMON = 4000;
    private static final int REQ_SELECT_FROM_POKEDEX = 4100;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private AppDbHelper db;
    private long teamId;
    private ImageButton[] slotButtons = new ImageButton[6]; // ajustar si tienes más/menos slots

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_team);

        db = DbProvider.get(this);
        teamId = getIntent().getLongExtra("teamId", -1);
        if (teamId <= 0) {
            Toast.makeText(this, "Team inválido", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        slotButtons[0] = findViewById(R.id.slot0);
        slotButtons[1] = findViewById(R.id.slot1);
        slotButtons[2] = findViewById(R.id.slot2);
        slotButtons[3] = findViewById(R.id.slot3);
        slotButtons[4] = findViewById(R.id.slot4);
        slotButtons[5] = findViewById(R.id.slot5);

        for (int i = 0; i < slotButtons.length; i++) {
            final int slot = i;
            slotButtons[i].setOnClickListener(v -> {
                Log.d(TAG, "Abrir Pokedex para slot=" + slot + " teamId=" + teamId);
                Intent it = new Intent(EditTeamActivity.this, PokedexActivity.class);
                it.putExtra("fromTeamEdit", true);
                it.putExtra("slot", slot);
                it.putExtra("teamId", teamId);
                startActivityForResult(it, REQ_SELECT_FROM_POKEDEX + slot);
            });
        }

        refreshAllSlots();
    }

    private void refreshAllSlots() {
        for (int i = 0; i < slotButtons.length; i++) {
            loadSlotImage(i);
        }
    }

    private void loadSlotImage(int slot) {
        TeamPokemon tp = db.getTeamPokemonBySlot(teamId, slot);
        if (tp == null || (tp.customSpritePath == null && tp.pokemonName == null)) {
            slotButtons[slot].setImageResource(android.R.drawable.ic_menu_gallery);
            return;
        }

        if (tp.customSpritePath != null && !tp.customSpritePath.isEmpty()) {
            Bitmap b = ImageUtils.loadBitmapFromPath(tp.customSpritePath);
            if (b != null) {
                slotButtons[slot].setImageBitmap(b);
                return;
            }
        }

        if (tp.pokemonName != null && !tp.pokemonName.isEmpty()) {
            final String name = tp.pokemonName;
            final ImageButton btn = slotButtons[slot];
            executor.execute(() -> {
                try {
                    JSONObject p = PokeApiClient.getPokemon(name);
                    if (p == null) {
                        runOnUiThread(() -> btn.setImageResource(android.R.drawable.ic_menu_report_image));
                        return;
                    }
                    String spriteUrl = PokeApiClient.getDefaultSprite(p);
                    if (spriteUrl != null && !spriteUrl.isEmpty()) {
                        runOnUiThread(() -> ImageUtils.loadImageIntoButtonFromUrl(EditTeamActivity.this, btn, spriteUrl));
                    } else {
                        runOnUiThread(() -> btn.setImageResource(android.R.drawable.ic_menu_report_image));
                    }
                } catch (Exception e) {
                    runOnUiThread(() -> btn.setImageResource(android.R.drawable.ic_menu_report_image));
                }
            });
            return;
        }

        slotButtons[slot].setImageResource(android.R.drawable.ic_menu_gallery);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult rc=" + requestCode + " rr=" + resultCode);

        // Manejo de retorno desde PokedexActivity (selección de Pokémon)
        int baseSelect = REQ_SELECT_FROM_POKEDEX;
        if (requestCode >= baseSelect && requestCode < baseSelect + slotButtons.length) {
            int selectedSlot = requestCode - baseSelect;
            if (resultCode == RESULT_OK && data != null) {
                String selected = data.getStringExtra("selectedPokemon");
                int returnedSlot = data.getIntExtra("slot", selectedSlot);
                long returnedTeamId = data.getLongExtra("teamId", teamId);

                Log.d(TAG, "Pokedex returned selected=" + selected + " returnedSlot=" + returnedSlot + " returnedTeamId=" + returnedTeamId);

                if (returnedTeamId == teamId && returnedSlot >= 0 && returnedSlot < slotButtons.length && selected != null) {
                    TeamPokemon tp = db.getTeamPokemonBySlot(teamId, returnedSlot);
                    if (tp == null) {
                        tp = new TeamPokemon();
                        tp.teamId = teamId;
                        tp.slot = returnedSlot;
                    }
                    tp.pokemonName = selected;
                    if (tp.evs == null) tp.evs = new int[6];
                    if (tp.ivs == null) tp.ivs = new int[6];
                    if (tp.moves == null) tp.moves = new String[4];

                    long id = db.saveTeamPokemon(tp);
                    if (id > 0) tp.id = id;

                    loadSlotImage(returnedSlot);

                    // Abrir editor inmediatamente para editar detalles (opcional)
                    Intent edit = new Intent(this, EditPokemonActivity.class);
                    edit.putExtra("teamId", teamId);
                    edit.putExtra("slot", returnedSlot);
                    edit.putExtra("pokemonName", selected);
                    startActivityForResult(edit, REQ_EDIT_POKEMON + returnedSlot);

                    Toast.makeText(this, "Pokémon " + selected + " asignado al slot " + returnedSlot, Toast.LENGTH_SHORT).show();
                    return;
                }
            } else {
                if (selectedSlot >= 0 && selectedSlot < slotButtons.length) loadSlotImage(selectedSlot);
                return;
            }
        }

        // Manejo de retorno desde EditPokemonActivity (si lo abres con REQ_EDIT_POKEMON)
        int baseEdit = REQ_EDIT_POKEMON;
        if (requestCode >= baseEdit && requestCode < baseEdit + slotButtons.length) {
            int editedSlot = requestCode - baseEdit;
            if (resultCode == RESULT_OK && data != null) {
                long returnedTeamId = data.getLongExtra("teamId", -1);
                int returnedSlot = data.getIntExtra("slot", -1);
                if (returnedTeamId == teamId && returnedSlot >= 0 && returnedSlot < slotButtons.length) {
                    loadSlotImage(returnedSlot);
                } else {
                    refreshAllSlots();
                }
            } else {
                if (editedSlot >= 0 && editedSlot < slotButtons.length) loadSlotImage(editedSlot);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }
}
