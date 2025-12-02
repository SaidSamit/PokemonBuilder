package com.pokemones.pokemonbuilder.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.pokemones.pokemonbuilder.R;
import com.pokemones.pokemonbuilder.api.PokeApiClient;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

public class PokedexActivity extends AppCompatActivity {
    private static final String TAG = "PokedexActivity";
    private static final int REQ_SORT_BY_GENERATION = 6001;
    private static final int REQ_SORT_BY_TYPE = 6002;
    private static final int ID_MENU_SORT_GENERATION = 1001;
    private static final int ID_MENU_SORT_TYPE = 1002;

    private ListView lv;
    private List<String> names = new ArrayList<>();
    private Button btnByGeneration;
    private Button btnByType;
    private boolean fromTeamEdit = false;
    private int slot = -1;
    private long teamId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pokedex);

        // Registrar toolbar para que el menu se muestre (tema NoActionBar)
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) setSupportActionBar(toolbar);

        lv = findViewById(R.id.lvPokedex);
        btnByGeneration = findViewById(R.id.btnByGeneration);
        btnByType = findViewById(R.id.btnByType);

        fromTeamEdit = getIntent().getBooleanExtra("fromTeamEdit", false);
        slot = getIntent().getIntExtra("slot", -1);
        teamId = getIntent().getLongExtra("teamId", -1);

        Log.d(TAG, "onCreate fromTeamEdit=" + fromTeamEdit + " slot=" + slot + " teamId=" + teamId);

        btnByGeneration.setOnClickListener(v -> {
            Intent i = new Intent(PokedexActivity.this, SortByGenerationActivity.class);
            if (fromTeamEdit) {
                i.putExtra("fromTeamEdit", true);
                i.putExtra("slot", slot);
                i.putExtra("teamId", teamId);
            }
            startActivityForResult(i, REQ_SORT_BY_GENERATION);
        });

        btnByType.setOnClickListener(v -> {
            Intent i = new Intent(PokedexActivity.this, SortByTypeActivity.class);
            if (fromTeamEdit) {
                i.putExtra("fromTeamEdit", true);
                i.putExtra("slot", slot);
                i.putExtra("teamId", teamId);
            }
            startActivityForResult(i, REQ_SORT_BY_TYPE);
        });

        new Thread(() -> {
            try {
                JSONArray arr = PokeApiClient.getAllPokemonNames();
                if (arr != null) {
                    for (int i = 0; i < arr.length(); i++) names.add(arr.optString(i));
                } else {
                    names.add("bulbasaur");
                    names.add("charmander");
                    names.add("squirtle");
                }
            } catch (Exception e) {
                Log.w(TAG, "Error cargando lista completa de la API, usando fallback", e);
                names.clear();
                names.add("bulbasaur");
                names.add("charmander");
                names.add("squirtle");
            }
            runOnUiThread(() -> {
                ArrayAdapter<String> a = new ArrayAdapter<>(PokedexActivity.this, android.R.layout.simple_list_item_1, names);
                lv.setAdapter(a);
            });
        }).start();

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

            Intent i = new Intent(PokedexActivity.this, EditPokemonActivity.class);
            i.putExtra("pokemonName", selected);
            startActivity(i);
        });
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

        if (menu.findItem(ID_MENU_SORT_GENERATION) == null) {
            menu.add(Menu.NONE, ID_MENU_SORT_GENERATION, Menu.NONE, "Ordenar por generación")
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        }
        if (menu.findItem(ID_MENU_SORT_TYPE) == null) {
            menu.add(Menu.NONE, ID_MENU_SORT_TYPE, Menu.NONE, "Ordenar por tipo")
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        }

        return true;
    }

    private void searchByName(String name) {
        if (name == null || name.trim().isEmpty()) return;
        final String q = name.trim().toLowerCase();
        new Thread(() -> {
            try {
                org.json.JSONObject p = PokeApiClient.getPokemon(q);
                if (p != null) {
                    final String found = p.getString("name");
                    runOnUiThread(() -> {
                        names.clear();
                        names.add(found);
                        if (lv.getAdapter() == null) {
                            lv.setAdapter(new ArrayAdapter<>(PokedexActivity.this, android.R.layout.simple_list_item_1, names));
                        } else {
                            ((ArrayAdapter) lv.getAdapter()).notifyDataSetChanged();
                        }
                    });
                } else {
                    runOnUiThread(() -> Toast.makeText(PokedexActivity.this, "Pokémon no encontrado", Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                Log.w(TAG, "searchByName error", e);
                runOnUiThread(() -> Toast.makeText(PokedexActivity.this, "Pokémon no encontrado", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    // onActivityResult y resto del código se mantienen igual (omitido aquí por brevedad)
}
