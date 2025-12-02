package com.pokemones.pokemonbuilder.ui;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.pokemones.pokemonbuilder.R;
import com.pokemones.pokemonbuilder.api.PokeApiClient;
import com.pokemones.pokemonbuilder.data.DbProvider;
import com.pokemones.pokemonbuilder.data.AppDbHelper;
import com.pokemones.pokemonbuilder.models.TeamPokemon;
import com.pokemones.pokemonbuilder.models.Team;
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
    private Team currentTeam;

    private EditText etTeamName;
    private Button btnSaveTeam;
    private Button btnDeleteTeam;
    private ImageButton[] slotButtons = new ImageButton[6]; // ajustar si tienes más/menos slots

    // URL del sprite por defecto (Poké Ball)
    private static final String DEFAULT_EMPTY_SLOT_SPRITE = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/items/poke-ball.png";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_team);

        db = DbProvider.get(this);
        teamId = getIntent().getLongExtra("teamId", -1);

        etTeamName = findViewById(R.id.etTeamName);
        btnSaveTeam = findViewById(R.id.btnSaveTeam);
        btnDeleteTeam = findViewById(R.id.btnDeleteTeam);

        slotButtons[0] = findViewById(R.id.slot0);
        slotButtons[1] = findViewById(R.id.slot1);
        slotButtons[2] = findViewById(R.id.slot2);
        slotButtons[3] = findViewById(R.id.slot3);
        slotButtons[4] = findViewById(R.id.slot4);
        slotButtons[5] = findViewById(R.id.slot5);

        if (teamId <= 0) {
            Toast.makeText(this, "Team inválido", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Cargar equipo desde DB y mostrar nombre
        loadTeam();

        // Listeners de slots: si ya hay pokemon -> abrir editor, si no -> abrir pokedex
        for (int i = 0; i < slotButtons.length; i++) {
            final int slot = i;
            slotButtons[i].setOnClickListener(v -> {
                TeamPokemon existing = db.getTeamPokemonBySlot(teamId, slot);
                if (existing != null && (existing.pokemonName != null || existing.id > 0)) {
                    Intent edit = new Intent(EditTeamActivity.this, EditPokemonActivity.class);
                    edit.putExtra("fromTeamEdit", true);
                    edit.putExtra("teamId", teamId);
                    edit.putExtra("slot", slot);
                    if (existing.pokemonName != null) edit.putExtra("pokemonName", existing.pokemonName);
                    startActivityForResult(edit, REQ_EDIT_POKEMON + slot);
                    return;
                }
                Intent it = new Intent(EditTeamActivity.this, PokedexActivity.class);
                it.putExtra("fromTeamEdit", true);
                it.putExtra("slot", slot);
                it.putExtra("teamId", teamId);
                startActivityForResult(it, REQ_SELECT_FROM_POKEDEX + slot);
            });
        }

        // Guardar nombre del equipo y volver a TeamsActivity
        btnSaveTeam.setOnClickListener(v -> saveTeamNameAndReturn());

        // Botón eliminar equipo con confirmación
        btnDeleteTeam.setOnClickListener(v -> {
            if (teamId <= 0) {
                Toast.makeText(EditTeamActivity.this, "Team inválido", Toast.LENGTH_SHORT).show();
                return;
            }
            new AlertDialog.Builder(EditTeamActivity.this)
                    .setTitle("Eliminar equipo")
                    .setMessage("¿Estás seguro que deseas eliminar este equipo? Esta acción eliminará también los Pokémon asignados y no se puede deshacer.")
                    .setPositiveButton("Eliminar", (dialog, which) -> {
                        boolean ok = db.deleteTeamById(teamId);
                        if (ok) {
                            Toast.makeText(EditTeamActivity.this, "Equipo eliminado", Toast.LENGTH_SHORT).show();
                            Intent res = new Intent();
                            res.putExtra("deletedTeamId", teamId);
                            setResult(RESULT_OK, res);
                            finish();
                        } else {
                            Toast.makeText(EditTeamActivity.this, "No se pudo eliminar el equipo", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
        });

        refreshAllSlots();
    }

    private void loadTeam() {
        currentTeam = db.getTeamById(teamId);
        if (currentTeam == null) {
            // Si no existe, crear uno nuevo con este id (opcional)
            currentTeam = new Team();
            currentTeam.id = teamId;
        }
        etTeamName.setText(currentTeam.name != null ? currentTeam.name : "");
    }

    private void saveTeamNameAndReturn() {
        String name = etTeamName.getText().toString().trim();
        if (name.isEmpty()) {
            Toast.makeText(this, "El nombre del equipo no puede estar vacío", Toast.LENGTH_SHORT).show();
            return;
        }
        currentTeam.name = name;
        long id = db.saveTeam(currentTeam); // asegúrate de que AppDbHelper tiene saveTeam(Team)
        if (id > 0) currentTeam.id = id;

        // Devolver resultado a la actividad que abrió EditTeamActivity (por ejemplo TeamsActivity)
        Intent res = new Intent();
        res.putExtra("teamId", currentTeam.id);
        res.putExtra("teamName", currentTeam.name);
        setResult(RESULT_OK, res);
        finish();
    }

    private void refreshAllSlots() {
        for (int i = 0; i < slotButtons.length; i++) loadSlotImage(i);
    }

    /**
     * Carga la imagen del slot:
     * - Si existe customSpritePath y es válido, lo muestra.
     * - Si existe pokemonName, carga el sprite por defecto del Pokémon desde la API.
     * - Si no hay Pokémon en el slot, muestra la Poké Ball por defecto (DEFAULT_EMPTY_SLOT_SPRITE).
     */
    private void loadSlotImage(int slot) {
        TeamPokemon tp = db.getTeamPokemonBySlot(teamId, slot);
        final ImageButton btn = slotButtons[slot];

        // Caso: no hay registro o no hay nombre ni sprite personalizado -> mostrar Poké Ball por defecto
        if (tp == null || (tp.customSpritePath == null && (tp.pokemonName == null || tp.pokemonName.isEmpty()))) {
            // Intentar cargar la Poké Ball remota en el botón
            ImageUtils.loadImageIntoButtonFromUrl(this, btn, DEFAULT_EMPTY_SLOT_SPRITE);
            return;
        }

        // Si hay sprite personalizado, intentar cargarlo desde archivo local
        if (tp.customSpritePath != null && !tp.customSpritePath.isEmpty()) {
            Bitmap b = ImageUtils.loadBitmapFromPath(tp.customSpritePath);
            if (b != null) {
                btn.setImageBitmap(b);
                return;
            }
            // si falla la carga local, continuar y tratar de cargar sprite por nombre o fallback
        }

        // Si hay nombre de Pokémon, obtener sprite por defecto desde la API en background
        if (tp.pokemonName != null && !tp.pokemonName.isEmpty()) {
            final String name = tp.pokemonName;
            executor.execute(() -> {
                try {
                    JSONObject p = PokeApiClient.getPokemon(name);
                    if (p == null) {
                        // fallback a Poké Ball si la API no responde
                        runOnUiThread(() -> ImageUtils.loadImageIntoButtonFromUrl(EditTeamActivity.this, btn, DEFAULT_EMPTY_SLOT_SPRITE));
                        return;
                    }
                    String spriteUrl = PokeApiClient.getDefaultSprite(p);
                    if (spriteUrl != null && !spriteUrl.isEmpty()) {
                        runOnUiThread(() -> ImageUtils.loadImageIntoButtonFromUrl(EditTeamActivity.this, btn, spriteUrl));
                    } else {
                        runOnUiThread(() -> ImageUtils.loadImageIntoButtonFromUrl(EditTeamActivity.this, btn, DEFAULT_EMPTY_SLOT_SPRITE));
                    }
                } catch (Exception e) {
                    runOnUiThread(() -> ImageUtils.loadImageIntoButtonFromUrl(EditTeamActivity.this, btn, DEFAULT_EMPTY_SLOT_SPRITE));
                }
            });
            return;
        }

        // Fallback final: Poké Ball
        ImageUtils.loadImageIntoButtonFromUrl(this, btn, DEFAULT_EMPTY_SLOT_SPRITE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult rc=" + requestCode + " rr=" + resultCode);

        // Resultado desde Pokedex (selección)
        int baseSelect = REQ_SELECT_FROM_POKEDEX;
        if (requestCode >= baseSelect && requestCode < baseSelect + slotButtons.length) {
            int selectedSlot = requestCode - baseSelect;
            if (resultCode == RESULT_OK && data != null) {
                String selected = data.getStringExtra("selectedPokemon");
                int returnedSlot = data.getIntExtra("slot", selectedSlot);
                long returnedTeamId = data.getLongExtra("teamId", teamId);

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

                    // Abrir editor para detalles (opcional) — pasar fromTeamEdit para evitar diálogo de borrador
                    Intent edit = new Intent(this, EditPokemonActivity.class);
                    edit.putExtra("fromTeamEdit", true);
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

        // Resultado desde EditPokemonActivity
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
