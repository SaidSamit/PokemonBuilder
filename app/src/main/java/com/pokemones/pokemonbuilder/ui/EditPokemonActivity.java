package com.pokemones.pokemonbuilder.ui;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
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
import com.pokemones.pokemonbuilder.utils.PermissionUtils;

import org.json.JSONObject;

public class EditPokemonActivity extends AppCompatActivity {
    private String pokemonName;
    private ImageView ivSprite;
    private ImageView ivCryPlay;
    private Button btnRecordCry;
    private Spinner spinnerAbilities;
    private SeekBar[] evSeek = new SeekBar[6];
    private SeekBar[] ivSeek = new SeekBar[6];
    private Button[] moveButtons = new Button[4];
    private TextView tvName;
    private TeamPokemon currentTp;
    private AppDbHelper db;

    private static final int REQ_TAKE_PHOTO = 2002;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_pokemon);

        // Pedimos permisos si no están (por si el usuario rechazó en login)
        PermissionUtils.requestCameraAndAudioPermissions(this);

        db = new AppDbHelper(this);
        pokemonName = getIntent().getStringExtra("pokemonName");

        tvName = findViewById(R.id.tvPokemonName);
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

        moveButtons[0] = findViewById(R.id.move0);
        moveButtons[1] = findViewById(R.id.move1);
        moveButtons[2] = findViewById(R.id.move2);
        moveButtons[3] = findViewById(R.id.move3);

        tvName.setText(pokemonName);

        // Inicializar currentTp si la actividad fue abierta para editar un slot de equipo
        long teamId = getIntent().getLongExtra("teamId", -1);
        int slot = getIntent().getIntExtra("slot", -1);
        if (teamId != -1 && slot >= 0) {
            currentTp = db.getTeamPokemonBySlot(teamId, slot);
            if (currentTp == null) {
                currentTp = new TeamPokemon();
                currentTp.teamId = teamId;
                currentTp.slot = slot;
                currentTp.pokemonName = pokemonName;
                long id = db.saveTeamPokemon(currentTp);
                if (id > 0) currentTp.id = id;
            } else {
                // mostrar sprite local si existe
                if (currentTp.customSpritePath != null && !currentTp.customSpritePath.isEmpty()) {
                    Bitmap b = ImageUtils.loadBitmapFromPath(currentTp.customSpritePath);
                    if (b != null) ivSprite.setImageBitmap(b);
                }
            }
        }

        ivSprite.setOnClickListener(v -> {
            // Verificamos permisos de cámara antes de abrir
            if (!PermissionUtils.hasAllPermissions(EditPokemonActivity.this)) {
                PermissionUtils.requestCameraAndAudioPermissions(EditPokemonActivity.this);
                Toast.makeText(this, "Concede permisos para usar la cámara", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent take = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            startActivityForResult(take, REQ_TAKE_PHOTO);
        });

        ivCryPlay.setOnClickListener(v -> {
            String last = AudioUtils.getLastPath();
            if (last != null) AudioUtils.play(this, last);
            else Toast.makeText(this, "No hay grito grabado", Toast.LENGTH_SHORT).show();
        });

        btnRecordCry.setOnClickListener(v -> {
            // Verificamos permisos de audio antes de grabar
            if (!PermissionUtils.hasAllPermissions(EditPokemonActivity.this)) {
                PermissionUtils.requestCameraAndAudioPermissions(EditPokemonActivity.this);
                Toast.makeText(this, "Concede permisos de audio para grabar", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!AudioUtils.isRecording()) {
                String path = AudioUtils.startRecording(this, "cry_" + pokemonName);
                if (path == null) {
                    Toast.makeText(this, "No se pudo iniciar la grabación", Toast.LENGTH_SHORT).show();
                } else {
                    btnRecordCry.setText("Detener grabación");
                    Toast.makeText(this, "Grabando...", Toast.LENGTH_SHORT).show();
                }
            } else {
                AudioUtils.stopRecording();
                btnRecordCry.setText("Grabar grito");
                Toast.makeText(this, "Grabación guardada", Toast.LENGTH_SHORT).show();
                // guardar ruta en currentTp si aplica
                if (currentTp != null) {
                    currentTp.cryPath = AudioUtils.getLastPath();
                    long savedId = db.saveTeamPokemon(currentTp);
                    if (savedId > 0) currentTp.id = savedId;
                }
            }
        });

        for (int i = 0; i < 4; i++) {
            final int idx = i;
            moveButtons[i].setOnClickListener(v -> {
                Intent intent = new Intent(this, MoveSelectionActivity.class);
                intent.putExtra("pokemonName", pokemonName);
                intent.putExtra("moveIndex", idx);
                startActivityForResult(intent, 3000 + idx);
            });
        }

        new LoadPokemonTask().execute(pokemonName);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode >= 3000 && requestCode < 3004 && resultCode == RESULT_OK && data != null) {
            String move = data.getStringExtra("selectedMove");
            int idx = requestCode - 3000;
            moveButtons[idx].setText(move);
        } else if (requestCode == REQ_TAKE_PHOTO && resultCode == RESULT_OK && data != null) {
            Bundle extras = data.getExtras();
            if (extras != null) {
                Bitmap photo = (Bitmap) extras.get("data");
                ivSprite.setImageBitmap(photo);
                String path = ImageUtils.saveBitmapToCache(this, photo, "sprite_" + pokemonName);
                if (currentTp != null) {
                    currentTp.customSpritePath = path;
                    long savedId = db.saveTeamPokemon(currentTp);
                    if (savedId > 0) currentTp.id = savedId;
                }
            }
        }
    }

    private class LoadPokemonTask extends AsyncTask<String, Void, JSONObject> {
        @Override
        protected JSONObject doInBackground(String... params) {
            try {
                return PokeApiClient.getPokemon(params[0]);
            } catch (Exception e) {
                return null;
            }
        }

        @Override
        protected void onPostExecute(JSONObject p) {
            if (p == null) return;
            String spriteUrl = PokeApiClient.getDefaultSprite(p);
            String[] abilities = PokeApiClient.getAbilities(p);
            String[] moves = PokeApiClient.getMoves(p);

            if (spriteUrl != null) ImageUtils.loadImageIntoImageView(EditPokemonActivity.this, ivSprite, spriteUrl);

            if (abilities != null && abilities.length > 0) {
                ArrayAdapter<String> a = new ArrayAdapter<>(EditPokemonActivity.this, android.R.layout.simple_spinner_item, abilities);
                a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerAbilities.setAdapter(a);
            }

            for (int i = 0; i < 4; i++) {
                if (moves != null && i < moves.length) moveButtons[i].setText(moves[i]);
                else moveButtons[i].setText("Movimiento " + (i + 1));
            }
        }
    }
}
