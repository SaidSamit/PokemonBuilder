package com.pokemones.pokemonbuilder.ui;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SearchView;

import androidx.appcompat.app.AppCompatActivity;

import com.pokemones.pokemonbuilder.R;
import com.pokemones.pokemonbuilder.api.PokeApiClient;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class PokedexActivity extends AppCompatActivity {
    private ListView lv;
    private ArrayAdapter<String> adapter;
    private List<String> names = new ArrayList<>();
    private boolean fromTeamEdit;
    private int slot = -1;
    private long teamId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pokedex);

        lv = findViewById(R.id.lvPokedex);
        Button btnByGen = findViewById(R.id.btnByGeneration);
        Button btnByType = findViewById(R.id.btnByType);

        fromTeamEdit = getIntent().getBooleanExtra("fromTeamEdit", false);
        slot = getIntent().getIntExtra("slot", -1);
        teamId = getIntent().getLongExtra("teamId", -1);

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, names);
        lv.setAdapter(adapter);

        lv.setOnItemClickListener((parent, view, position, id) -> {
            String selected = names.get(position);
            if (fromTeamEdit) {
                Intent res = new Intent();
                res.putExtra("selectedPokemon", selected);
                res.putExtra("slot", slot);
                setResult(RESULT_OK, res);
                finish();
            } else {
                Intent i = new Intent(this, EditPokemonActivity.class);
                i.putExtra("pokemonName", selected);
                startActivity(i);
            }
        });

        // Carga inicial simple (puedes ajustar rango)
        new LoadPokemonListTask().execute(1, 151);

        btnByGen.setOnClickListener(v -> {
            Intent i = new Intent(this, SortByGenerationActivity.class);
            startActivity(i);
        });

        btnByType.setOnClickListener(v -> {
            Intent i = new Intent(this, SortByTypeActivity.class);
            startActivity(i);
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_pokedex, menu);
        MenuItem searchItem = menu.findItem(R.id.action_search);
        androidx.appcompat.widget.SearchView sv = (androidx.appcompat.widget.SearchView) searchItem.getActionView();
        sv.setQueryHint("Buscar por nombre");
        sv.setOnQueryTextListener(new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String query) {
                searchByName(query);
                return true;
            }
            @Override public boolean onQueryTextChange(String newText) { return false; }
        });
        return true;
    }

    private void searchByName(String name) {
        new SearchPokemonTask().execute(name);
    }

    private class LoadPokemonListTask extends AsyncTask<Integer, Void, List<String>> {
        @Override protected List<String> doInBackground(Integer... params) {
            int from = params[0], to = params[1];
            List<String> out = new ArrayList<>();
            try {
                for (int i = from; i <= to; i++) {
                    JSONObject p = PokeApiClient.getPokemon(String.valueOf(i));
                    out.add(p.getString("name"));
                }
            } catch (Exception e) {
                // manejo simple
            }
            return out;
        }
        @Override protected void onPostExecute(List<String> result) {
            names.clear();
            names.addAll(result);
            adapter.notifyDataSetChanged();
        }
    }

    private class SearchPokemonTask extends AsyncTask<String, Void, String> {
        @Override protected String doInBackground(String... params) {
            try {
                JSONObject p = PokeApiClient.getPokemon(params[0].toLowerCase());
                return p.getString("name");
            } catch (Exception e) { return null; }
        }
        @Override protected void onPostExecute(String s) {
            if (s != null) {
                names.clear();
                names.add(s);
                adapter.notifyDataSetChanged();
            }
        }
    }
}
