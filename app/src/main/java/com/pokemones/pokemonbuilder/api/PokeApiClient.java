package com.pokemones.pokemonbuilder.api;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Cliente simple y sincrónico para PokeAPI.
 * Llamar a estos métodos desde un hilo en background (no en el hilo UI).
 * Asegúrate de tener <uses-permission android:name="android.permission.INTERNET" /> en AndroidManifest.xml.
 */
public class PokeApiClient {
    private static final String BASE = "https://pokeapi.co/api/v2";

    // -------------------------
    // Endpoints básicos
    // -------------------------

    /**
     * Obtiene el JSONObject del endpoint /pokemon/{nameOrId}
     */
    public static JSONObject getPokemon(String nameOrId) {
        if (nameOrId == null) return null;
        String url = BASE + "/pokemon/" + nameOrId.toLowerCase();
        try {
            String s = httpGet(url);
            if (s == null) return null;
            return new JSONObject(s);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Obtiene el JSONObject del endpoint /generation/{idOrName}
     */
    public static JSONObject getGeneration(String idOrName) {
        if (idOrName == null) return null;
        String url = BASE + "/generation/" + idOrName.toLowerCase();
        try {
            String s = httpGet(url);
            if (s == null) return null;
            return new JSONObject(s);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Obtiene el JSONObject del endpoint /type/{idOrName}
     */
    public static JSONObject getType(String idOrName) {
        if (idOrName == null) return null;
        String url = BASE + "/type/" + idOrName.toLowerCase();
        try {
            String s = httpGet(url);
            if (s == null) return null;
            return new JSONObject(s);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Devuelve la URL del sprite por defecto (front_default) si existe
     */
    public static String getDefaultSprite(JSONObject pokemonJson) {
        if (pokemonJson == null) return null;
        try {
            JSONObject sprites = pokemonJson.optJSONObject("sprites");
            if (sprites == null) return null;
            return sprites.optString("front_default", null);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Extrae nombres de abilities desde el JSONObject de pokemon
     */
    public static String[] getAbilities(JSONObject pokemonJson) {
        try {
            if (pokemonJson == null) return new String[0];
            JSONArray arr = pokemonJson.optJSONArray("abilities");
            if (arr == null) return new String[0];
            List<String> out = new ArrayList<>();
            for (int i = 0; i < arr.length(); i++) {
                JSONObject item = arr.getJSONObject(i);
                JSONObject ability = item.optJSONObject("ability");
                if (ability != null) out.add(ability.optString("name"));
            }
            return out.toArray(new String[0]);
        } catch (Exception e) {
            return new String[0];
        }
    }

    /**
     * Extrae nombres de movimientos desde el JSONObject de pokemon
     */
    public static String[] getMoves(JSONObject pokemonJson) {
        try {
            if (pokemonJson == null) return new String[0];
            JSONArray arr = pokemonJson.optJSONArray("moves");
            if (arr == null) return new String[0];
            List<String> out = new ArrayList<>();
            for (int i = 0; i < arr.length(); i++) {
                JSONObject item = arr.getJSONObject(i);
                JSONObject move = item.optJSONObject("move");
                if (move != null) out.add(move.optString("name"));
            }
            return out.toArray(new String[0]);
        } catch (Exception e) {
            return new String[0];
        }
    }

    // -------------------------
    // Listados y utilidades
    // -------------------------

    /**
     * Devuelve un JSONArray con todos los nombres de Pokémon (puede ser grande).
     * Usa ?limit=100000 para obtener todos en una sola llamada.
     */
    public static JSONArray getAllPokemonNames() {
        String url = BASE + "/pokemon?limit=100000&offset=0";
        try {
            String s = httpGet(url);
            if (s == null) return null;
            JSONObject root = new JSONObject(s);
            JSONArray results = root.optJSONArray("results");
            if (results == null) return null;
            JSONArray names = new JSONArray();
            for (int i = 0; i < results.length(); i++) {
                JSONObject item = results.getJSONObject(i);
                String name = item.optString("name", null);
                if (name != null) names.put(name);
            }
            return names;
        } catch (JSONException je) {
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Alternativa que devuelve List<String> con los nombres (más cómodo en Java).
     */
    public static List<String> getAllPokemonNamesList() {
        JSONArray arr = getAllPokemonNames();
        List<String> out = new ArrayList<>();
        if (arr == null) return out;
        for (int i = 0; i < arr.length(); i++) out.add(arr.optString(i));
        return out;
    }

    /**
     * Devuelve la lista de nombres de Pokémon incluidos en una generación.
     * Recibe id o nombre de la generación (por ejemplo "1" o "generation-i").
     */
    public static List<String> getPokemonNamesByGeneration(String generationIdOrName) {
        List<String> out = new ArrayList<>();
        try {
            JSONObject gen = getGeneration(generationIdOrName);
            if (gen == null) return out;
            JSONArray species = gen.optJSONArray("pokemon_species");
            if (species == null) return out;
            for (int i = 0; i < species.length(); i++) {
                JSONObject item = species.getJSONObject(i);
                String name = item.optString("name", null);
                if (name != null) out.add(name);
            }
        } catch (Exception e) {
            // ignore and return empty
        }
        return out;
    }

    /**
     * Devuelve la lista de nombres de Pokémon asociados a un tipo.
     * Recibe id o nombre del tipo (por ejemplo "1" o "fire").
     */
    public static List<String> getPokemonNamesByType(String typeIdOrName) {
        List<String> out = new ArrayList<>();
        try {
            JSONObject typeObj = getType(typeIdOrName);
            if (typeObj == null) return out;
            JSONArray pokemonArr = typeObj.optJSONArray("pokemon");
            if (pokemonArr == null) return out;
            for (int i = 0; i < pokemonArr.length(); i++) {
                JSONObject item = pokemonArr.getJSONObject(i);
                JSONObject pokemon = item.optJSONObject("pokemon");
                if (pokemon != null) {
                    String name = pokemon.optString("name", null);
                    if (name != null) out.add(name);
                }
            }
        } catch (Exception e) {
            // devolver lista vacía en caso de error
        }
        return out;
    }

    // -------------------------
    // Helper HTTP GET
    // -------------------------
    private static String httpGet(String urlStr) {
        HttpURLConnection conn = null;
        BufferedReader reader = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(15000);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            int code = conn.getResponseCode();
            InputStream is = (code >= 200 && code < 400) ? conn.getInputStream() : conn.getErrorStream();
            if (is == null) return null;
            reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            return sb.toString();
        } catch (Exception e) {
            return null;
        } finally {
            try { if (reader != null) reader.close(); } catch (Exception ignored) {}
            if (conn != null) conn.disconnect();
        }
    }
}
