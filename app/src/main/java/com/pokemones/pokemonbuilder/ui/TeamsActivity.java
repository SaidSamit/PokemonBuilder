package com.pokemones.pokemonbuilder.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.pokemones.pokemonbuilder.R;
import com.pokemones.pokemonbuilder.data.AppDbHelper;
import com.pokemones.pokemonbuilder.models.Team;

import java.util.ArrayList;
import java.util.List;

public class TeamsActivity extends AppCompatActivity {
    private static final int REQ_EDIT_TEAM = 5000;

    private AppDbHelper db;
    private ListView lv;
    private Button btnAdd;
    private long userId;
    private List<Team> teams;
    private ArrayAdapter<String> adapter;
    private ArrayList<String> names;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teams);

        db = new AppDbHelper(this);
        lv = findViewById(R.id.lvTeams);
        btnAdd = findViewById(R.id.btnAddTeam);
        userId = getIntent().getLongExtra("userId", -1);

        btnAdd.setOnClickListener(v -> {
            long id = db.createTeam(userId, "Nuevo equipo");
            if (id > 0) {
                Intent i = new Intent(this, EditTeamActivity.class);
                i.putExtra("teamId", id);
                i.putExtra("userId", userId);
                startActivityForResult(i, REQ_EDIT_TEAM);
            } else {
                Toast.makeText(this, "No se pudo crear el equipo", Toast.LENGTH_SHORT).show();
            }
        });

        lv.setOnItemClickListener((parent, view, position, id) -> {
            Team t = teams.get(position);
            Intent i = new Intent(this, EditTeamActivity.class);
            i.putExtra("teamId", t.id);
            i.putExtra("userId", userId);
            startActivityForResult(i, REQ_EDIT_TEAM);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTeams();
    }

    private void loadTeams() {
        teams = db.getTeamsForUser(userId);
        names = new ArrayList<>();
        if (teams != null) {
            for (Team t : teams) names.add(t.name == null ? "Equipo " + t.id : t.name);
        }
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, names);
        lv.setAdapter(adapter);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQ_EDIT_TEAM) return;

        // Si EditTeamActivity devolvió RESULT_OK, recargamos la lista.
        if (resultCode == RESULT_OK) {
            if (data != null) {
                long deletedTeamId = data.getLongExtra("deletedTeamId", -1);
                if (deletedTeamId > 0) {
                    // Si se eliminó un equipo, recargar lista y mostrar mensaje
                    loadTeams();
                    Toast.makeText(this, "Equipo eliminado", Toast.LENGTH_SHORT).show();
                    return;
                }

                long returnedTeamId = data.getLongExtra("teamId", -1);
                String returnedTeamName = data.getStringExtra("teamName");
                if (returnedTeamId > 0) {
                    // Si se creó/actualizó un equipo, recargar lista y opcionalmente mostrar mensaje
                    loadTeams();
                    if (returnedTeamName != null) {
                        Toast.makeText(this, "Equipo guardado: " + returnedTeamName, Toast.LENGTH_SHORT).show();
                    }
                    return;
                }
            }
            // Resultado OK sin extras: recargar por seguridad
            loadTeams();
        } else {
            // Si no fue OK, recargamos el slot correspondiente por si hubo cambios parciales
            loadTeams();
        }
    }
}
