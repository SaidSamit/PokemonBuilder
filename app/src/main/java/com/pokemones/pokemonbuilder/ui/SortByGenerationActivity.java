package com.pokemones.pokemonbuilder.ui;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.pokemones.pokemonbuilder.R;
import com.pokemones.pokemonbuilder.api.PokeApiClient;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class SortByGenerationActivity extends AppCompatActivity {
    private ListView lv;
    private ArrayAdapter<String> adapter;
    private List<String> names = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sort_by_generation);
        lv = findViewById(R.id.lvGeneration);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, names);
        lv.setAdapter(adapter);

        lv.setOnItemClickListener((parent, view, position, id) -> {
            String selected = names.get(position);
            Intent i = new Intent(this, EditPokemonActivity.class);
            i.putExtra("pokemonName", selected);
            startActivity(i);
        });

        // Carga por defecto: generación 1 (puedes cambiar UI para elegir generación)
        new LoadGenerationTask().execute("1");
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

    private class LoadGenerationTask extends AsyncTask<String, Void, List<String>> {
        @Override protected List<String> doInBackground(String... params) {
            String gen = params[0];
            List<String> out = new ArrayList<>();
            try {
                JSONObject g = PokeApiClient.getGeneration(gen);
                // generation endpoint returns "pokemon_species" array
                JSONArray arr = g.getJSONArray("pokemon_species");
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject obj = arr.getJSONObject(i);
                    out.add(obj.getString("name"));
                }
            } catch (Exception e) { }
            return out;
        }
        @Override protected void onPostExecute(List<String> result) {
            names.clear();
            names.addAll(result);
            adapter.notifyDataSetChanged();
            if (names.isEmpty()) Toast.makeText(SortByGenerationActivity.this, "No se encontraron Pokémon", Toast.LENGTH_SHORT).show();
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
