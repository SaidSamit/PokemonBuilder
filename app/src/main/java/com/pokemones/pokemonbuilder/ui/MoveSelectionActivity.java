package com.pokemones.pokemonbuilder.ui;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.pokemones.pokemonbuilder.R;
import com.pokemones.pokemonbuilder.api.PokeApiClient;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MoveSelectionActivity extends AppCompatActivity {
    private ListView lv;
    private ArrayAdapter<String> adapter;
    private List<String> moves = new ArrayList<>();
    private String pokemonName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_move_selection);

        lv = findViewById(R.id.lvMoves);
        pokemonName = getIntent().getStringExtra("pokemonName");

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, moves);
        lv.setAdapter(adapter);

        lv.setOnItemClickListener((parent, view, position, id) -> {
            String selected = moves.get(position);
            Intent res = new Intent();
            res.putExtra("selectedMove", selected);
            setResult(RESULT_OK, res);
            finish();
        });

        new LoadMovesTask().execute(pokemonName);
    }

    private class LoadMovesTask extends AsyncTask<String, Void, List<String>> {
        @Override
        protected List<String> doInBackground(String... params) {
            List<String> out = new ArrayList<>();
            try {
                JSONObject p = PokeApiClient.getPokemon(params[0]);
                String[] m = PokeApiClient.getMoves(p);
                for (String s : m) out.add(s);
            } catch (Exception e) { }
            return out;
        }

        @Override
        protected void onPostExecute(List<String> result) {
            moves.clear();
            moves.addAll(result);
            adapter.notifyDataSetChanged();
            if (moves.isEmpty()) Toast.makeText(MoveSelectionActivity.this, "No hay movimientos disponibles", Toast.LENGTH_SHORT).show();
        }
    }
}

