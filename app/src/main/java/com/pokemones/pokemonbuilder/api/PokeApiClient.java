package com.pokemones.pokemonbuilder.api;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Scanner;

public class PokeApiClient {
    private static final String BASE = "https://pokeapi.co/api/v2/";

    private static String getJson(String endpoint) throws IOException {
        URL url = new URL(BASE + endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);
        InputStream in = conn.getInputStream();
        Scanner s = new Scanner(in).useDelimiter("\\A");
        String json = s.hasNext() ? s.next() : "";
        in.close();
        conn.disconnect();
        return json;
    }

    public static JSONObject getPokemon(String nameOrId) throws IOException, JSONException {
        String enc = URLEncoder.encode(nameOrId, "UTF-8");
        String json = getJson("pokemon/" + enc);
        return new JSONObject(json);
    }

    public static JSONObject getType(String nameOrId) throws IOException, JSONException {
        String enc = URLEncoder.encode(nameOrId, "UTF-8");
        String json = getJson("type/" + enc);
        return new JSONObject(json);
    }

    public static JSONObject getGeneration(String nameOrId) throws IOException, JSONException {
        String enc = URLEncoder.encode(nameOrId, "UTF-8");
        String json = getJson("generation/" + enc);
        return new JSONObject(json);
    }

    // Helpers to extract common fields
    public static String getDefaultSprite(JSONObject pokemon) {
        try {
            JSONObject sprites = pokemon.optJSONObject("sprites");
            if (sprites != null) {
                String s = sprites.optString("front_default", null);
                if (s != null && !s.equals("null")) return s;
            }
        } catch (Exception ignored) {}
        return null;
    }

    public static String[] getAbilities(JSONObject pokemon) {
        try {
            JSONArray arr = pokemon.getJSONArray("abilities");
            String[] out = new String[arr.length()];
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i).getJSONObject("ability");
                out[i] = obj.getString("name");
            }
            return out;
        } catch (Exception e) {
            return new String[0];
        }
    }

    public static String[] getMoves(JSONObject pokemon) {
        try {
            JSONArray arr = pokemon.getJSONArray("moves");
            String[] out = new String[arr.length()];
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i).getJSONObject("move");
                out[i] = obj.getString("name");
            }
            return out;
        } catch (Exception e) {
            return new String[0];
        }
    }

    public static JSONObject getMove(String nameOrId) throws IOException, JSONException {
        String enc = URLEncoder.encode(nameOrId, "UTF-8");
        String json = getJson("move/" + enc);
        return new JSONObject(json);
    }

    public static JSONObject getAbility(String nameOrId) throws IOException, JSONException {
        String enc = URLEncoder.encode(nameOrId, "UTF-8");
        String json = getJson("ability/" + enc);
        return new JSONObject(json);
    }
}
