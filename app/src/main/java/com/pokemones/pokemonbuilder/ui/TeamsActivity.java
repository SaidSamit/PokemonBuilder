package com.pokemones.pokemonbuilder.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.pokemones.pokemonbuilder.R;
import com.pokemones.pokemonbuilder.data.AppDbHelper;
import com.pokemones.pokemonbuilder.models.Team;

import java.util.ArrayList;
import java.util.List;

public class TeamsActivity extends AppCompatActivity {
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
                startActivity(i);
            } else {
                Toast.makeText(this, "No se pudo crear el equipo", Toast.LENGTH_SHORT).show();
            }
        });

        lv.setOnItemClickListener((parent, view, position, id) -> {
            Team t = teams.get(position);
            Intent i = new Intent(this, EditTeamActivity.class);
            i.putExtra("teamId", t.id);
            i.putExtra("userId", userId);
            startActivity(i);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        teams = db.getTeamsForUser(userId);
        names = new ArrayList<>();
        for (Team t : teams) names.add(t.name == null ? "Equipo " + t.id : t.name);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, names);
        lv.setAdapter(adapter);
    }
}
