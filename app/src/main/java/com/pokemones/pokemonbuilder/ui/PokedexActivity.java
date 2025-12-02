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

import com.pokemones.pokemonbuilder.R;
import com.pokemones.pokemonbuilder.api.PokeApiClient;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

public class PokedexActivity extends AppCompatActivity {
    private static final String TAG = "PokedexActivity";

    // Request codes para activities de ordenamiento
    private static final int REQ_SORT_BY_GENERATION = 6001;
    private static final int REQ_SORT_BY_TYPE = 6002;

    // Ids programáticos para los items de menú (porque menu_pokedex solo tiene action_search)
    private static final int ID_MENU_SORT_GENERATION = 1001;
    private static final int ID_MENU_SORT_TYPE = 1002;

    private ListView lv;
    private List<String> names = new ArrayList<>();

    // Botones para navegar a ordenamientos
    private Button btnByGeneration;
    private Button btnByType;

    // Si viene desde edición de equipo, devolvemos la selección con slot/teamId
    private boolean fromTeamEdit = false;
    private int slot = -1;
    private long teamId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pokedex);

        lv = findViewById(R.id.lvPokedex);

        // Inicializar botones (asegúrate que los ids existen en tu layout)
        btnByGeneration = findViewById(R.id.btnByGeneration);
        btnByType = findViewById(R.id.btnByType);

        fromTeamEdit = getIntent().getBooleanExtra("fromTeamEdit", false);
        slot = getIntent().getIntExtra("slot", -1);
        teamId = getIntent().getLongExtra("teamId", -1);

        Log.d(TAG, "onCreate fromTeamEdit=" + fromTeamEdit + " slot=" + slot + " teamId=" + teamId);

        // Listeners de botones para abrir las Activities correspondientes
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

        // Cargar lista en background
        new Thread(() -> {
            try {
                JSONArray arr = PokeApiClient.getAllPokemonNames();
                if (arr != null) {
                    for (int i = 0; i < arr.length(); i++) names.add(arr.optString(i));
                } else {
                    // fallback mínimo
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
                // Devolver selección al EditTeamActivity (o quien haya pedido)
                Intent res = new Intent();
                res.putExtra("selectedPokemon", selected);
                res.putExtra("slot", slot);
                res.putExtra("teamId", teamId);
                setResult(RESULT_OK, res);
                finish();
                return;
            }

            // Si no venimos desde edición de equipo, abrimos el editor normal
            Intent i = new Intent(PokedexActivity.this, EditPokemonActivity.class);
            i.putExtra("pokemonName", selected);
            startActivity(i);
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflar el menú existente (contiene action_search)
        try {
            getMenuInflater().inflate(R.menu.menu_pokedex, menu);
        } catch (Exception e) {
            Log.w(TAG, "No se pudo inflar menu_pokedex: " + e.getMessage());
        }

        // Añadir programáticamente opciones para ordenar por generación / tipo si no están en el XML
        if (menu.findItem(ID_MENU_SORT_GENERATION) == null) {
            menu.add(Menu.NONE, ID_MENU_SORT_GENERATION, Menu.NONE, "Ordenar por generación")
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        }
        if (menu.findItem(ID_MENU_SORT_TYPE) == null) {
            menu.add(Menu.NONE, ID_MENU_SORT_TYPE, Menu.NONE, "Ordenar por tipo")
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        }

        // Configurar SearchView si existe en el XML
        MenuItem searchItem = menu.findItem(R.id.action_search);
        if (searchItem != null) {
            androidx.appcompat.widget.SearchView sv = (androidx.appcompat.widget.SearchView) searchItem.getActionView();
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
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == ID_MENU_SORT_GENERATION) {
            Intent i = new Intent(this, SortByGenerationActivity.class);
            if (fromTeamEdit) {
                i.putExtra("fromTeamEdit", true);
                i.putExtra("slot", slot);
                i.putExtra("teamId", teamId);
            }
            startActivityForResult(i, REQ_SORT_BY_GENERATION);
            return true;
        } else if (id == ID_MENU_SORT_TYPE) {
            Intent i = new Intent(this, SortByTypeActivity.class);
            if (fromTeamEdit) {
                i.putExtra("fromTeamEdit", true);
                i.putExtra("slot", slot);
                i.putExtra("teamId", teamId);
            }
            startActivityForResult(i, REQ_SORT_BY_TYPE);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void searchByName(String name) {
        if (name == null || name.trim().isEmpty()) return;
        final String q = name.trim().toLowerCase();
        new Thread(() -> {
            try {
                // Reutilizamos PokeApiClient.getPokemon para validar existencia
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

    // Manejar resultados desde SortByGenerationActivity / SortByTypeActivity
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult rc=" + requestCode + " rr=" + resultCode);

        if ((requestCode == REQ_SORT_BY_GENERATION || requestCode == REQ_SORT_BY_TYPE) && resultCode == RESULT_OK && data != null) {
            String selected = data.getStringExtra("selectedPokemon");
            int returnedSlot = data.getIntExtra("slot", slot);
            long returnedTeamId = data.getLongExtra("teamId", teamId);

            if (selected != null) {
                Log.d(TAG, "Recibido de ordenamiento: " + selected + " slot=" + returnedSlot + " teamId=" + returnedTeamId);
                if (fromTeamEdit) {
                    // Propagar resultado al llamador original (EditTeamActivity)
                    Intent res = new Intent();
                    res.putExtra("selectedPokemon", selected);
                    res.putExtra("slot", returnedSlot);
                    res.putExtra("teamId", returnedTeamId);
                    setResult(RESULT_OK, res);
                    finish();
                    return;
                } else {
                    // Abrir editor normal
                    Intent i = new Intent(this, EditPokemonActivity.class);
                    i.putExtra("pokemonName", selected);
                    startActivity(i);
                    return;
                }
            }
        }

        // Si la activity de ordenamiento no devolvió selección, simplemente refrescamos la lista visual
        if (requestCode == REQ_SORT_BY_GENERATION || requestCode == REQ_SORT_BY_TYPE) {
            if (lv.getAdapter() != null) ((ArrayAdapter) lv.getAdapter()).notifyDataSetChanged();
        }
    }
}
