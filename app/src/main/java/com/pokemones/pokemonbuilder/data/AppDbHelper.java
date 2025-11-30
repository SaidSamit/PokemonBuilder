package com.pokemones.pokemonbuilder.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.pokemones.pokemonbuilder.models.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class AppDbHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "pokemonbuilder.db";
    private static final int DB_VERSION = 1;

    public AppDbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE users(id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT UNIQUE, password TEXT)");
        db.execSQL("CREATE TABLE teams(id INTEGER PRIMARY KEY AUTOINCREMENT, user_id INTEGER, name TEXT)");
        db.execSQL("CREATE TABLE team_pokemons(id INTEGER PRIMARY KEY AUTOINCREMENT, team_id INTEGER, slot INTEGER, pokemon_name TEXT, sprite TEXT, cry TEXT, ability TEXT, evs TEXT, ivs TEXT, moves TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
        db.execSQL("DROP TABLE IF EXISTS team_pokemons");
        db.execSQL("DROP TABLE IF EXISTS teams");
        db.execSQL("DROP TABLE IF EXISTS users");
        onCreate(db);
    }

    // --- Users ---
    public long createUser(String username, String password) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("username", username);
        v.put("password", password);
        return db.insert("users", null, v);
    }

    public User login(String username, String password) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT id, username, password FROM users WHERE username=? AND password=?", new String[]{username, password});
        if (c.moveToFirst()) {
            User u = new User();
            u.id = c.getLong(0);
            u.username = c.getString(1);
            u.password = c.getString(2);
            c.close();
            return u;
        }
        c.close();
        return null;
    }

    // --- Teams ---
    public long createTeam(long userId, String name) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("user_id", userId);
        v.put("name", name);
        return db.insert("teams", null, v);
    }

    public List<Team> getTeamsForUser(long userId) {
        List<Team> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT id, user_id, name FROM teams WHERE user_id=?", new String[]{String.valueOf(userId)});
        while (c.moveToNext()) {
            Team t = new Team();
            t.id = c.getLong(0);
            t.userId = c.getLong(1);
            t.name = c.getString(2);
            list.add(t);
        }
        c.close();
        return list;
    }

    // --- TeamPokemons ---
    private String arrayToJsonString(int[] arr) {
        JSONArray a = new JSONArray();
        for (int v : arr) a.put(v);
        return a.toString();
    }

    private String arrayToJsonString(String[] arr) {
        JSONArray a = new JSONArray();
        for (String s : arr) a.put(s == null ? JSONObjectNull() : s);
        return a.toString();
    }

    private String JSONObjectNull() { return JSONObject.NULL.toString(); }

    private int[] jsonStringToIntArray(String json) {
        try {
            JSONArray a = new JSONArray(json);
            int[] out = new int[a.length()];
            for (int i = 0; i < a.length(); i++) out[i] = a.getInt(i);
            return out;
        } catch (Exception e) {
            return new int[6];
        }
    }

    private String[] jsonStringToStringArray(String json) {
        try {
            JSONArray a = new JSONArray(json);
            String[] out = new String[a.length()];
            for (int i = 0; i < a.length(); i++) out[i] = a.optString(i, null);
            return out;
        } catch (Exception e) {
            return new String[4];
        }
    }

    public long saveTeamPokemon(TeamPokemon tp) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("team_id", tp.teamId);
        v.put("slot", tp.slot);
        v.put("pokemon_name", tp.pokemonName);
        v.put("sprite", tp.customSpritePath);
        v.put("cry", tp.cryPath);
        v.put("ability", tp.ability);
        v.put("evs", arrayToJsonString(tp.evs));
        v.put("ivs", arrayToJsonString(tp.ivs));
        v.put("moves", arrayToJsonString(tp.moves));
        if (tp.id > 0) {
            db.update("team_pokemons", v, "id=?", new String[]{String.valueOf(tp.id)});
            return tp.id;
        } else {
            return db.insert("team_pokemons", null, v);
        }
    }

    public List<TeamPokemon> getTeamPokemons(long teamId) {
        List<TeamPokemon> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT id, team_id, slot, pokemon_name, sprite, cry, ability, evs, ivs, moves FROM team_pokemons WHERE team_id=?", new String[]{String.valueOf(teamId)});
        while (c.moveToNext()) {
            TeamPokemon tp = new TeamPokemon();
            tp.id = c.getLong(0);
            tp.teamId = c.getLong(1);
            tp.slot = c.getInt(2);
            tp.pokemonName = c.getString(3);
            tp.customSpritePath = c.getString(4);
            tp.cryPath = c.getString(5);
            tp.ability = c.getString(6);
            tp.evs = jsonStringToIntArray(c.getString(7));
            tp.ivs = jsonStringToIntArray(c.getString(8));
            tp.moves = jsonStringToStringArray(c.getString(9));
            list.add(tp);
        }
        c.close();
        return list;
    }

    public TeamPokemon getTeamPokemonBySlot(long teamId, int slot) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT id, team_id, slot, pokemon_name, sprite, cry, ability, evs, ivs, moves FROM team_pokemons WHERE team_id=? AND slot=?", new String[]{String.valueOf(teamId), String.valueOf(slot)});
        if (c.moveToFirst()) {
            TeamPokemon tp = new TeamPokemon();
            tp.id = c.getLong(0);
            tp.teamId = c.getLong(1);
            tp.slot = c.getInt(2);
            tp.pokemonName = c.getString(3);
            tp.customSpritePath = c.getString(4);
            tp.cryPath = c.getString(5);
            tp.ability = c.getString(6);
            tp.evs = jsonStringToIntArray(c.getString(7));
            tp.ivs = jsonStringToIntArray(c.getString(8));
            tp.moves = jsonStringToStringArray(c.getString(9));
            c.close();
            return tp;
        }
        c.close();
        return null;
    }

    public void deleteTeam(long teamId) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete("team_pokemons", "team_id=?", new String[]{String.valueOf(teamId)});
        db.delete("teams", "id=?", new String[]{String.valueOf(teamId)});
    }
}
