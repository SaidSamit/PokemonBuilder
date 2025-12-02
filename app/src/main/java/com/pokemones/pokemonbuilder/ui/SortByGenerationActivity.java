package com.pokemones.pokemonbuilder.ui;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.pokemones.pokemonbuilder.R;
import com.pokemones.pokemonbuilder.api.PokeApiClient;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class SortByGenerationActivity extends AppCompatActivity {
    private static final String TAG = "SortByGenerationActivity";

    private ListView lv;
    private ArrayAdapter<String> adapter;
    private List<String> names = new ArrayList<>();

    // Si viene desde edición de equipo, devolvemos la selección con slot/teamId
    private boolean fromTeamEdit = false;
    private int slot = -1;
    private long teamId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sort_by_generation);

        // Registrar toolbar para que el menu se muestre (tema NoActionBar)
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) setSupportActionBar(toolbar);

        lv = findViewById(R.id.lvGeneration);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, names);
        lv.setAdapter(adapter);

        fromTeamEdit = getIntent().getBooleanExtra("fromTeamEdit", false);
        slot = getIntent().getIntExtra("slot", -1);
        teamId = getIntent().getLongExtra("teamId", -1);

        lv.setOnItemClickListener((parent, view, position, id) -> {
            String selected = names.get(position);
            Log.d(TAG, "Seleccionado: " + selected + " fromTeamEdit=" + fromTeamEdit + " slot=" + slot + " teamId=" + teamId);

            if (fromTeamEdit) {
                Intent res = new Intent();
                res.putExtra("selectedPokemon", selected);
                res.putExtra("slot", slot);
                res.putExtra("teamId", teamId);
                setResult(RESULT_OK, res);
                finish();
                return;
            }

            Intent i = new Intent(this, EditPokemonActivity.class);
            i.putExtra("pokemonName", selected);
            startActivity(i);
        });

        // Carga por defecto: todas las generaciones en orden
        new LoadAllGenerationsTask().execute();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        try {
            getMenuInflater().inflate(R.menu.menu_pokedex, menu);
        } catch (Exception e) {
            Log.w(TAG, "No se pudo inflar menu_pokedex: " + e.getMessage());
        }

        MenuItem searchItem = menu.findItem(R.id.action_search);
        if (searchItem != null) {
            androidx.appcompat.widget.SearchView sv = (androidx.appcompat.widget.SearchView) searchItem.getActionView();
            if (sv != null) {
                sv.setQueryHint("Buscar por nombre");
                sv.setOnQueryTextListener(new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
                    @Override public boolean onQueryTextSubmit(String query) {
                        searchByName(query);
                        searchItem.collapseActionView();
                        return true;
                    }
                    @Override public boolean onQueryTextChange(String newText) { return false; }
                });
            }
        }
        return true;
    }

    private void searchByName(String name) {
        if (name == null || name.trim().isEmpty()) return;
        new SearchPokemonTask().execute(name);
    }

    /**
     * AsyncTask que carga todas las generaciones (usa PokeApiClient.getGeneration)
     * Mantenerla como clase interna evita problemas de visibilidad.
     */
    private class LoadAllGenerationsTask extends AsyncTask<Void, Void, List<String>> {
        @Override
        protected List<String> doInBackground(Void... voids) {
            List<String> out = new ArrayList<>();
            try {
                for (int gen = 1; gen <= 20; gen++) {
                    try {
                        JSONObject g = PokeApiClient.getGeneration(String.valueOf(gen));
                        if (g == null) break;
                        JSONArray arr = g.optJSONArray("pokemon_species");
                        if (arr == null) continue;
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject obj = arr.getJSONObject(i);
                            out.add(obj.getString("name"));
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "Error en generación " + gen + ": " + e.getMessage());
                        break;
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "Error cargando generaciones", e);
            }
            return out;
        }

        @Override
        protected void onPostExecute(List<String> result) {
            if (result == null || result.isEmpty()) {
                Toast.makeText(SortByGenerationActivity.this, "No se pudieron cargar generaciones", Toast.LENGTH_SHORT).show();
                return;
            }
            names.clear();
            names.addAll(result);
            adapter.notifyDataSetChanged();
        }
    }

    /**
     * AsyncTask para buscar un Pokémon por nombre usando PokeApiClient.getPokemon
     */
    private class SearchPokemonTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            try {
                JSONObject p = PokeApiClient.getPokemon(params[0].toLowerCase());
                if (p != null) return p.getString("name");
            } catch (Exception e) {
                Log.w(TAG, "searchByName error: " + e.getMessage());
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            if (s != null) {
                names.clear();
                names.add(s);
                adapter.notifyDataSetChanged();
            } else {
                Toast.makeText(SortByGenerationActivity.this, "Pokémon no encontrado", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
