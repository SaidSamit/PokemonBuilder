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

import com.pokemones.pokemonbuilder.R;
import com.pokemones.pokemonbuilder.api.PokeApiClient;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SortByTypeActivity extends AppCompatActivity {
    private static final String TAG = "SortByTypeActivity";

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
        setContentView(R.layout.activity_sort_by_type);

        lv = findViewById(R.id.lvType);
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

        // Carga por defecto: todos los tipos y sus pokémon en orden
        new LoadAllTypesTask().execute();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        try {
            getMenuInflater().inflate(R.menu.menu_pokedex, menu);
        } catch (Exception e) {
            Log.w(TAG, "No se pudo inflar menu_pokedex: " + e.getMessage());
        }

        androidx.appcompat.widget.SearchView sv = null;
        MenuItem searchItem = menu.findItem(R.id.action_search);
        if (searchItem != null) {
            sv = (androidx.appcompat.widget.SearchView) searchItem.getActionView();
        }
        if (sv != null) {
            sv.setQueryHint("Buscar por nombre");
            sv.setOnQueryTextListener(new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
                @Override public boolean onQueryTextSubmit(String query) {
                    searchByName(query);
                    return true;
                }
                @Override public boolean onQueryTextChange(String newText) { return false; }
            });
        }
        return true;
    }

    private void searchByName(String name) {
        new SearchPokemonTask().execute(name);
    }

    private class LoadAllTypesTask extends AsyncTask<Void, Void, List<String>> {
        @Override protected List<String> doInBackground(Void... voids) {
            List<String> out = new ArrayList<>();
            Set<String> seen = new HashSet<>();
            try {
                for (int t = 1; t <= 100; t++) {
                    try {
                        JSONObject typeObj = PokeApiClient.getType(String.valueOf(t));
                        if (typeObj == null) break;
                        JSONArray arr = typeObj.optJSONArray("pokemon");
                        if (arr == null) continue;
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject entry = arr.getJSONObject(i).getJSONObject("pokemon");
                            String name = entry.getString("name");
                            if (!seen.contains(name)) {
                                seen.add(name);
                                out.add(name);
                            }
                        }
                    } catch (Exception e) {
                        break;
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "Error cargando tipos", e);
            }
            return out;
        }

        @Override protected void onPostExecute(List<String> result) {
            if (result == null || result.isEmpty()) {
                Toast.makeText(SortByTypeActivity.this, "No se pudieron cargar tipos", Toast.LENGTH_SHORT).show();
                return;
            }
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
            } else {
                Toast.makeText(SortByTypeActivity.this, "Pokémon no encontrado", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
