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

/**
 * Muestra todos los Pokémon ordenados por generación (1..N).
 * Por defecto carga todas las generaciones que devuelve la API en orden.
 */
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

        // Carga por defecto: todas las generaciones en orden
        new LoadAllGenerationsTask().execute();
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
     * Carga todas las generaciones listadas por la API y concatena sus pokemon_species
     * en orden de generación (1,2,3,...). Usa el endpoint /generation/{id}.
     */
    private class LoadAllGenerationsTask extends AsyncTask<Void, Void, List<String>> {
        @Override protected List<String> doInBackground(Void... voids) {
            List<String> out = new ArrayList<>();
            try {
                // Primero obtener la lista de generaciones desde la API: /generation
                // PokeApiClient no tiene getAllGenerations, así que pedimos por índices hasta que falle.
                // Alternativa: podrías llamar a "generation" con ids conocidos (1..8). Aquí intentamos 1..20 y paramos cuando falla.
                for (int gen = 1; gen <= 20; gen++) {
                    try {
                        JSONObject g = PokeApiClient.getGeneration(String.valueOf(gen));
                        if (g == null) break;
                        JSONArray arr = g.optJSONArray("pokemon_species");
                        if (arr == null) continue;
                        // pokemon_species viene sin orden por número; para mantener orden por número podríamos parsear y ordenar,
                        // pero aquí añadimos en el orden que devuelve la API (suele ser alfabético).
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject obj = arr.getJSONObject(i);
                            out.add(obj.getString("name"));
                        }
                    } catch (Exception e) {
                        // si falla para un gen concreto, asumimos que no hay más generaciones y rompemos
                        break;
                    }
                }
            } catch (Exception e) {
                // si algo falla devolvemos lo que tengamos
            }
            return out;
        }

        @Override protected void onPostExecute(List<String> result) {
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
     * Busca un Pokémon por nombre y lo muestra (reemplaza la lista por el resultado).
     */
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
                Toast.makeText(SortByGenerationActivity.this, "Pokémon no encontrado", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
