package com.pokemones.pokemonbuilder.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.pokemones.pokemonbuilder.R;
import com.pokemones.pokemonbuilder.api.PokeApiClient;
import com.pokemones.pokemonbuilder.data.AppDbHelper;
import com.pokemones.pokemonbuilder.models.TeamPokemon;
import com.pokemones.pokemonbuilder.utils.AudioUtils;
import com.pokemones.pokemonbuilder.utils.ImageUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class EditPokemonActivity extends AppCompatActivity {
    private static final String TAG = "EditPokemonActivity";
    private static final int REQ_SELECT_MOVE = 7001;
    private static final int REQ_CAMERA = 8001;
    private static final int REQ_SELECT_POKEMON = 9001;
    private static final String PREFS_DRAFTS = "pokemon_drafts";

    // UI
    private TextView tvPokemonName;
    private Button btnChangePokemon;
    private ImageView ivSprite;
    private ImageView ivCryPlay;
    private Button btnRecordCry;
    private Spinner spinnerAbilities;
    private SeekBar[] evSeek = new SeekBar[6];
    private SeekBar[] ivSeek = new SeekBar[6];
    private Button[] btnMoves = new Button[4];
    private Button btnSave;
    private Button btnResetSprite;
    private Button btnDeletePokemon;

    // DB helper
    private AppDbHelper db;

    // Intent extras
    private String pokemonName;
    private boolean fromTeamEdit = false;
    private long teamId = -1;
    private int slot = -1;

    // Working model
    private TeamPokemon currentTp;
    private final String[] moves = new String[4];
    private int currentMoveIndex = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_pokemon);

        db = new AppDbHelper(this);

        // Bind UI
        tvPokemonName = findViewById(R.id.tvPokemonName);
        btnChangePokemon = findViewById(R.id.btnChangePokemon);
        ivSprite = findViewById(R.id.ivSprite);
        ivCryPlay = findViewById(R.id.ivCryPlay);
        btnRecordCry = findViewById(R.id.btnRecordCry);
        spinnerAbilities = findViewById(R.id.spinnerAbilities);

        evSeek[0] = findViewById(R.id.ev0);
        evSeek[1] = findViewById(R.id.ev1);
        evSeek[2] = findViewById(R.id.ev2);
        evSeek[3] = findViewById(R.id.ev3);
        evSeek[4] = findViewById(R.id.ev4);
        evSeek[5] = findViewById(R.id.ev5);

        ivSeek[0] = findViewById(R.id.iv0);
        ivSeek[1] = findViewById(R.id.iv1);
        ivSeek[2] = findViewById(R.id.iv2);
        ivSeek[3] = findViewById(R.id.iv3);
        ivSeek[4] = findViewById(R.id.iv4);
        ivSeek[5] = findViewById(R.id.iv5);

        btnMoves[0] = findViewById(R.id.move0);
        btnMoves[1] = findViewById(R.id.move1);
        btnMoves[2] = findViewById(R.id.move2);
        btnMoves[3] = findViewById(R.id.move3);

        btnSave = findViewById(R.id.btnSavePokemon);
        btnResetSprite = findViewById(R.id.btnResetSprite);
        btnDeletePokemon = findViewById(R.id.btnDeletePokemon);

        // Extras
        pokemonName = getIntent().getStringExtra("pokemonName");
        fromTeamEdit = getIntent().getBooleanExtra("fromTeamEdit", false);
        teamId = getIntent().getLongExtra("teamId", -1);
        slot = getIntent().getIntExtra("slot", -1);

        if (pokemonName == null) pokemonName = "";

        tvPokemonName.setText(pokemonName.isEmpty() ? "Editar Pokémon" : pokemonName);
        setTitle(pokemonName.isEmpty() ? "Editar Pokémon" : "Editar: " + pokemonName);

        // Inicializar spinner con placeholder mientras cargamos habilidades reales
        spinnerAbilities.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Cargando habilidades..."}));

        // Move buttons listeners
        for (int i = 0; i < btnMoves.length; i++) {
            final int idx = i;
            btnMoves[i].setOnClickListener(v -> {
                currentMoveIndex = idx;
                Intent sel = new Intent(EditPokemonActivity.this, MoveSelectionActivity.class);
                sel.putExtra("pokemonName", pokemonName);
                startActivityForResult(sel, REQ_SELECT_MOVE);
            });
        }

        // Cambiar Pokémon
        btnChangePokemon.setOnClickListener(v -> {
            Intent it = new Intent(EditPokemonActivity.this, PokedexActivity.class);
            it.putExtra("fromTeamEdit", true);
            it.putExtra("teamId", teamId);
            it.putExtra("slot", slot);
            startActivityForResult(it, REQ_SELECT_POKEMON);
        });

        // Hacer la imagen clickeable para tomar foto
        ivSprite.setClickable(true);
        ivSprite.setOnClickListener(v -> {
            Intent takePic = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (takePic.resolveActivity(getPackageManager()) != null) {
                startActivityForResult(takePic, REQ_CAMERA);
            } else {
                Toast.makeText(this, "No hay cámara disponible", Toast.LENGTH_SHORT).show();
            }
        });

        // Cry play / record
        ivCryPlay.setOnClickListener(v -> {
            if (currentTp != null && currentTp.cryPath != null) {
                AudioUtils.play(EditPokemonActivity.this, currentTp.cryPath);
            } else {
                Toast.makeText(this, "No hay grito grabado", Toast.LENGTH_SHORT).show();
            }
        });

        btnRecordCry.setOnClickListener(v -> {
            if (AudioUtils.isRecording()) {
                boolean stopped = AudioUtils.stopRecording();
                if (stopped) {
                    String last = AudioUtils.getLastPath();
                    if (currentTp == null) currentTp = new TeamPokemon();
                    currentTp.cryPath = last;
                    Toast.makeText(this, "Grabación guardada", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "No se pudo detener la grabación", Toast.LENGTH_SHORT).show();
                }
            } else {
                String path = AudioUtils.startRecording(EditPokemonActivity.this, "cry_" + System.currentTimeMillis());
                if (path != null) {
                    Toast.makeText(this, "Grabando... pulsa de nuevo para detener", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "No se pudo iniciar la grabación (revisa permisos)", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Reset sprite personalizado
        btnResetSprite.setOnClickListener(v -> {
            if (teamId < 0 || slot < 0) {
                Toast.makeText(this, "Operación disponible solo para edición de equipo", Toast.LENGTH_SHORT).show();
                return;
            }
            new AlertDialog.Builder(this)
                    .setTitle("Quitar foto personalizada")
                    .setMessage("¿Deseas quitar la foto personalizada y volver al sprite por defecto?")
                    .setPositiveButton("Quitar", (dialog, which) -> {
                        boolean ok = db.clearCustomSpriteForSlot(teamId, slot);
                        if (ok) {
                            if (currentTp != null) currentTp.customSpritePath = null;
                            // recargar sprite por defecto desde API
                            loadSpriteAndAbilitiesFromApi();
                            Toast.makeText(EditPokemonActivity.this, "Foto personalizada eliminada", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(EditPokemonActivity.this, "No se pudo eliminar la foto personalizada", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
        });

        // Eliminar Pokémon del equipo
        btnDeletePokemon.setOnClickListener(v -> {
            if (teamId < 0 || slot < 0) {
                Toast.makeText(this, "Operación disponible solo para edición de equipo", Toast.LENGTH_SHORT).show();
                return;
            }
            new AlertDialog.Builder(this)
                    .setTitle("Eliminar Pokémon")
                    .setMessage("¿Deseas eliminar este Pokémon del equipo? Esta acción no se puede deshacer.")
                    .setPositiveButton("Eliminar", (dialog, which) -> {
                        boolean deleted = db.deleteTeamPokemonBySlot(teamId, slot);
                        if (deleted) {
                            Toast.makeText(EditPokemonActivity.this, "Pokémon eliminado del equipo", Toast.LENGTH_SHORT).show();
                            // Devolver resultado para que EditTeamActivity refresque
                            Intent out = new Intent();
                            out.putExtra("teamId", teamId);
                            out.putExtra("slot", slot);
                            setResult(RESULT_OK, out);
                            finish();
                        } else {
                            Toast.makeText(EditPokemonActivity.this, "No se pudo eliminar el Pokémon", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
        });

        // Save button
        btnSave.setText("Guardar Pokémon");
        btnSave.setOnClickListener(v -> savePokemon());

        // Cargar datos existentes si corresponde
        loadExistingDataIfAny();

        // Cargar sprite y habilidades desde API en background
        loadSpriteAndAbilitiesFromApi();
    }

    private void loadExistingDataIfAny() {
        currentTp = null;

        if (fromTeamEdit && teamId >= 0 && slot >= 0) {
            try {
                TeamPokemon tp = db.getTeamPokemonBySlot(teamId, slot);
                if (tp != null) {
                    currentTp = tp;
                    if (tp.pokemonName != null && !tp.pokemonName.isEmpty()) {
                        pokemonName = tp.pokemonName;
                        tvPokemonName.setText(pokemonName);
                        setTitle("Editar: " + pokemonName);
                    }
                    // Sprite local
                    if (tp.customSpritePath != null) {
                        Bitmap b = ImageUtils.loadBitmapFromPath(tp.customSpritePath);
                        if (b != null) ivSprite.setImageBitmap(b);
                    }
                    // EVs / IVs
                    if (tp.evs != null && tp.evs.length == 6) {
                        for (int i = 0; i < 6; i++) evSeek[i].setProgress(tp.evs[i]);
                    }
                    if (tp.ivs != null && tp.ivs.length == 6) {
                        for (int i = 0; i < 6; i++) ivSeek[i].setProgress(tp.ivs[i]);
                    }
                    // Moves
                    if (tp.moves != null) {
                        for (int i = 0; i < Math.min(tp.moves.length, 4); i++) {
                            moves[i] = tp.moves[i];
                        }
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "Error cargando TeamPokemon: " + e.getMessage(), e);
            }
        } else {
            loadDraftIfAny();
        }

        refreshMoveButtons();
    }

    private void loadSpriteAndAbilitiesFromApi() {
        if (pokemonName == null || pokemonName.isEmpty()) return;

        new Thread(() -> {
            try {
                JSONObject p = PokeApiClient.getPokemon(pokemonName.toLowerCase());
                if (p != null) {
                    JSONObject sprites = p.optJSONObject("sprites");
                    String front = null;
                    if (sprites != null) front = sprites.optString("front_default", null);
                    final String spriteUrl = front;

                    List<String> abilities = new ArrayList<>();
                    JSONArray arr = p.optJSONArray("abilities");
                    if (arr != null) {
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject entry = arr.getJSONObject(i);
                            JSONObject abilityObj = entry.optJSONObject("ability");
                            if (abilityObj != null) {
                                String aname = abilityObj.optString("name", null);
                                if (aname != null) abilities.add(aname);
                            }
                        }
                    }

                    runOnUiThread(() -> {
                        // Cargar sprite remoto si no hay sprite local
                        if ((currentTp == null || currentTp.customSpritePath == null) && spriteUrl != null) {
                            ImageUtils.loadImageIntoImageView(EditPokemonActivity.this, ivSprite, spriteUrl);
                        }
                        // Llenar spinner con habilidades reales (o fallback)
                        if (!abilities.isEmpty()) {
                            spinnerAbilities.setAdapter(new ArrayAdapter<>(EditPokemonActivity.this,
                                    android.R.layout.simple_spinner_dropdown_item, abilities));
                            if (currentTp != null && currentTp.ability != null) {
                                ArrayAdapter adapter = (ArrayAdapter) spinnerAbilities.getAdapter();
                                for (int i = 0; i < adapter.getCount(); i++) {
                                    if (currentTp.ability.equals(adapter.getItem(i))) {
                                        spinnerAbilities.setSelection(i);
                                        break;
                                    }
                                }
                            }
                        } else {
                            spinnerAbilities.setAdapter(new ArrayAdapter<>(EditPokemonActivity.this,
                                    android.R.layout.simple_spinner_dropdown_item, new String[]{"Sin datos"}));
                        }
                    });
                } else {
                    runOnUiThread(() -> spinnerAbilities.setAdapter(new ArrayAdapter<>(EditPokemonActivity.this,
                            android.R.layout.simple_spinner_dropdown_item, new String[]{"Sin datos"})));
                }
            } catch (Exception e) {
                Log.w(TAG, "Error cargando datos API: " + e.getMessage(), e);
                runOnUiThread(() -> spinnerAbilities.setAdapter(new ArrayAdapter<>(EditPokemonActivity.this,
                        android.R.layout.simple_spinner_dropdown_item, new String[]{"Sin datos"})));
            }
        }).start();
    }

    private void refreshMoveButtons() {
        for (int i = 0; i < btnMoves.length; i++) {
            btnMoves[i].setText(moves[i] == null ? ("Movimiento " + (i + 1)) : moves[i]);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Resultado selección de movimiento
        if (requestCode == REQ_SELECT_MOVE && resultCode == RESULT_OK && data != null) {
            String selected = data.getStringExtra("selectedMove");
            if (selected != null && currentMoveIndex >= 0 && currentMoveIndex < moves.length) {
                moves[currentMoveIndex] = selected;
                refreshMoveButtons();
            }
            currentMoveIndex = -1;
            return;
        }

        // Resultado cámara (thumbnail)
        if (requestCode == REQ_CAMERA && resultCode == RESULT_OK && data != null) {
            Bundle extras = data.getExtras();
            if (extras != null) {
                Bitmap bmp = (Bitmap) extras.get("data");
                if (bmp != null) {
                    String path = ImageUtils.saveBitmapToCache(this, bmp, "sprite_" + System.currentTimeMillis());
                    if (path != null) {
                        if (currentTp == null) currentTp = new TeamPokemon();
                        currentTp.customSpritePath = path;
                        ivSprite.setImageBitmap(bmp);
                        Toast.makeText(this, "Foto guardada", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "No se pudo guardar la foto", Toast.LENGTH_SHORT).show();
                    }
                }
            }
            return;
        }

        // Resultado selección de Pokémon (viene de PokedexActivity)
        if (requestCode == REQ_SELECT_POKEMON && resultCode == RESULT_OK && data != null) {
            String selected = data.getStringExtra("selectedPokemon");
            int returnedSlot = data.getIntExtra("slot", slot);
            long returnedTeamId = data.getLongExtra("teamId", teamId);

            if (selected != null && returnedTeamId == teamId && returnedSlot == slot) {
                pokemonName = selected;
                tvPokemonName.setText(pokemonName);
                setTitle("Editar: " + pokemonName);

                if (currentTp == null) currentTp = new TeamPokemon();
                currentTp.pokemonName = pokemonName;

                // Resetear movimientos (opcional)
                for (int i = 0; i < moves.length; i++) moves[i] = null;
                refreshMoveButtons();

                // Cargar sprite y habilidades del nuevo Pokémon
                loadSpriteAndAbilitiesFromApi();

                // Guardar inmediatamente el cambio de nombre en DB (mantener slot)
                try {
                    currentTp.teamId = teamId;
                    currentTp.slot = slot;
                    if (currentTp.evs == null) currentTp.evs = new int[6];
                    if (currentTp.ivs == null) currentTp.ivs = new int[6];
                    if (currentTp.moves == null) currentTp.moves = new String[4];
                    long res = db.saveTeamPokemon(currentTp);
                    if (res > 0) currentTp.id = res;
                } catch (Exception e) {
                    Log.w(TAG, "No se pudo guardar cambio de pokemon inmediatamente: " + e.getMessage(), e);
                }
            }
            return;
        }
    }

    private void savePokemon() {
        if (pokemonName == null || pokemonName.isEmpty()) {
            Toast.makeText(this, "Nombre de Pokémon inválido", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentTp == null) {
            currentTp = new TeamPokemon();
            currentTp.teamId = teamId;
            currentTp.slot = slot;
        }
        currentTp.pokemonName = pokemonName;

        // Ability
        Object sel = spinnerAbilities.getSelectedItem();
        currentTp.ability = sel == null ? null : sel.toString();

        // EVs
        int[] evs = new int[6];
        for (int i = 0; i < 6; i++) evs[i] = evSeek[i].getProgress();
        currentTp.evs = evs;

        // IVs
        int[] ivs = new int[6];
        for (int i = 0; i < 6; i++) ivs[i] = ivSeek[i].getProgress();
        currentTp.ivs = ivs;

        // Moves
        currentTp.moves = new String[4];
        for (int i = 0; i < 4; i++) currentTp.moves[i] = moves[i];

        if (fromTeamEdit && teamId >= 0 && slot >= 0) {
            try {
                long res = db.saveTeamPokemon(currentTp);
                if (res > 0) {
                    Toast.makeText(this, "Pokémon guardado en el equipo", Toast.LENGTH_SHORT).show();
                    Intent out = new Intent();
                    out.putExtra("selectedPokemon", pokemonName);
                    out.putExtra("slot", slot);
                    out.putExtra("teamId", teamId);
                    setResult(RESULT_OK, out);
                    finish();
                } else {
                    Toast.makeText(this, "Error guardando Pokémon en DB", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Log.e(TAG, "savePokemon error: " + e.getMessage(), e);
                Toast.makeText(this, "Error guardando Pokémon (revisa logs)", Toast.LENGTH_SHORT).show();
            }
        } else {
            new AlertDialog.Builder(this)
                    .setTitle("Guardar borrador")
                    .setMessage("No estás editando un equipo. ¿Deseas guardar esta configuración como borrador local?")
                    .setPositiveButton("Guardar borrador", (dialog, which) -> {
                        saveDraftLocally(currentTp);
                        Toast.makeText(EditPokemonActivity.this, "Borrador guardado localmente", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
        }
    }

    private void saveDraftLocally(TeamPokemon tp) {
        try {
            SharedPreferences prefs = getSharedPreferences(PREFS_DRAFTS, Context.MODE_PRIVATE);
            SharedPreferences.Editor ed = prefs.edit();
            String key = "draft_" + pokemonName.toLowerCase();
            JSONObject j = new JSONObject();
            j.put("pokemonName", tp.pokemonName);
            j.put("ability", tp.ability == null ? JSONObject.NULL : tp.ability);
            j.put("custom_sprite_path", tp.customSpritePath == null ? JSONObject.NULL : tp.customSpritePath);
            j.put("cry_path", tp.cryPath == null ? JSONObject.NULL : tp.cryPath);
            j.put("moves", new JSONArray(tp.moves == null ? new String[0] : tp.moves));
            j.put("evs", new JSONArray(tp.evs == null ? new int[6] : tp.evs));
            j.put("ivs", new JSONArray(tp.ivs == null ? new int[6] : tp.ivs));
            ed.putString(key, j.toString());
            ed.apply();
        } catch (Exception e) {
            Log.w(TAG, "saveDraftLocally error: " + e.getMessage(), e);
        }
    }

    private void loadDraftIfAny() {
        try {
            SharedPreferences prefs = getSharedPreferences(PREFS_DRAFTS, Context.MODE_PRIVATE);
            String key = "draft_" + pokemonName.toLowerCase();
            String s = prefs.getString(key, null);
            if (s != null) {
                JSONObject j = new JSONObject(s);
                if (j.has("moves")) {
                    JSONArray ma = j.optJSONArray("moves");
                    if (ma != null) {
                        for (int i = 0; i < Math.min(4, ma.length()); i++) {
                            moves[i] = ma.optString(i, null);
                        }
                    }
                }
                if (j.has("evs")) {
                    JSONArray ea = j.optJSONArray("evs");
                    if (ea != null) {
                        for (int i = 0; i < Math.min(6, ea.length()); i++) {
                            evSeek[i].setProgress(ea.optInt(i, 0));
                        }
                    }
                }
                if (j.has("ivs")) {
                    JSONArray ia = j.optJSONArray("ivs");
                    if (ia != null) {
                        for (int i = 0; i < Math.min(6, ia.length()); i++) {
                            ivSeek[i].setProgress(ia.optInt(i, 0));
                        }
                    }
                }
                String spritePath = j.optString("custom_sprite_path", null);
                if (spritePath != null && !spritePath.equals("null")) {
                    Bitmap b = ImageUtils.loadBitmapFromPath(spritePath);
                    if (b != null) {
                        ivSprite.setImageBitmap(b);
                        if (currentTp == null) currentTp = new TeamPokemon();
                        currentTp.customSpritePath = spritePath;
                    }
                }
                String ability = j.optString("ability", null);
                if (ability != null && !ability.equals("null")) {
                    ArrayAdapter adapter = (ArrayAdapter) spinnerAbilities.getAdapter();
                    for (int i = 0; i < adapter.getCount(); i++) {
                        if (ability.equals(adapter.getItem(i))) {
                            spinnerAbilities.setSelection(i);
                            break;
                        }
                    }
                }
                refreshMoveButtons();
            }
        } catch (Exception e) {
            Log.w(TAG, "loadDraftIfAny error: " + e.getMessage(), e);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        AudioUtils.stopPlay();
    }
}
