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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Muestra todos los Pokémon ordenados por tipo.
 * Por defecto obtiene la lista de tipos y concatena los Pokémon de cada tipo en ese orden,
 * evitando duplicados (un Pokémon aparece en el primer tipo en el que aparece).
 */
public class SortByTypeActivity extends AppCompatActivity {
    private ListView lv;
    private ArrayAdapter<String> adapter;
    private List<String> names = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sort_by_type);

        lv = findViewById(R.id.lvType);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, names);
        lv.setAdapter(adapter);

        lv.setOnItemClickListener((parent, view, position, id) -> {
            String selected = names.get(position);
            Intent i = new Intent(this, EditPokemonActivity.class);
            i.putExtra("pokemonName", selected);
            startActivity(i);
        });

        // Carga por defecto: todos los tipos y sus pokémon en orden
        new LoadAllTypesTask().execute();
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

    /**
     * Obtiene la lista de tipos desde /type (pide por índices 1..n) y para cada tipo obtiene su lista de pokémon.
     * Evita duplicados usando un Set.
     */
    private class LoadAllTypesTask extends AsyncTask<Void, Void, List<String>> {
        @Override protected List<String> doInBackground(Void... voids) {
            List<String> out = new ArrayList<>();
            Set<String> seen = new HashSet<>();
            try {
                // Intentamos pedir tipos por id hasta que falle (p. ej. 1..100)
                for (int t = 1; t <= 100; t++) {
                    try {
                        JSONObject typeObj = PokeApiClient.getType(String.valueOf(t));
                        if (typeObj == null) break;
                        // cada type devuelve "pokemon": [{pokemon:{name, url}, slot:...}, ...]
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
                        // si falla para un id concreto, asumimos que no hay más tipos y rompemos
                        break;
                    }
                }
            } catch (Exception e) {
                // ignore
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
