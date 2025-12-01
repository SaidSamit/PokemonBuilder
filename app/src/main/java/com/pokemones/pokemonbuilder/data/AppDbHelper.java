package com.pokemones.pokemonbuilder.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.pokemones.pokemonbuilder.models.TeamPokemon;
import com.pokemones.pokemonbuilder.models.Team;
import com.pokemones.pokemonbuilder.models.User;
import com.pokemones.pokemonbuilder.utils.DbUtils;

import java.util.ArrayList;
import java.util.List;

public class AppDbHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "pokemonbuilder.db";
    private static final int DB_VERSION = 2; // incrementado para nueva estructura

    public AppDbHelper(Context ctx) {
        super(ctx, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Users
        db.execSQL("CREATE TABLE users (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "username TEXT UNIQUE NOT NULL," +
                "password TEXT NOT NULL" +
                ")");

        // Teams
        db.execSQL("CREATE TABLE teams (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "user_id INTEGER NOT NULL," +
                "name TEXT," +
                "FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE" +
                ")");

        // Team pokemons (nueva estructura con columnas para ability, evs, ivs, moves y FK)
        db.execSQL("CREATE TABLE team_pokemons (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "team_id INTEGER NOT NULL," +
                "slot INTEGER NOT NULL," +
                "pokemon_name TEXT," +
                "custom_sprite_path TEXT," +
                "cry_path TEXT," +
                "ability TEXT," +
                "evs TEXT," +    // JSON string
                "ivs TEXT," +    // JSON string
                "moves TEXT," +  // JSON string
                "UNIQUE(team_id, slot)," +
                "FOREIGN KEY(team_id) REFERENCES teams(id) ON DELETE CASCADE" +
                ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Si no importa perder datos, la estrategia más simple es recrear tablas.
        // Aquí se muestra recreación segura: drop y create (perderá datos).
        if (oldVersion < 2) {
            // Para simplificar y cumplir la petición del usuario (no importa perder datos),
            // eliminamos la tabla antigua y creamos la nueva estructura.
            try {
                db.execSQL("DROP TABLE IF EXISTS team_pokemons");
                db.execSQL("DROP TABLE IF EXISTS teams");
                db.execSQL("DROP TABLE IF EXISTS users");
            } catch (Exception ignored) {}
            onCreate(db);
        }
    }

    // -------------------------
    // Users
    // -------------------------
    public long createUser(String username, String password) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("username", username);
        cv.put("password", password);
        long id = db.insert("users", null, cv);
        return id;
    }

    public User login(String username, String password) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query("users", null, "username = ? AND password = ?", new String[]{username, password}, null, null, null);
        try {
            if (c.moveToFirst()) {
                User u = new User();
                u.setId(c.getLong(c.getColumnIndexOrThrow("id")));
                u.setUsername(c.getString(c.getColumnIndexOrThrow("username")));
                u.setPassword(c.getString(c.getColumnIndexOrThrow("password")));
                return u;
            }
            return null;
        } finally {
            if (c != null) c.close();
        }
    }

    // -------------------------
    // Teams
    // -------------------------
    public long createTeam(long userId, String name) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("user_id", userId);
        cv.put("name", name);
        return db.insert("teams", null, cv);
    }

    public List<Team> getTeamsForUser(long userId) {
        List<Team> out = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query("teams", null, "user_id = ?", new String[]{String.valueOf(userId)}, null, null, null);
        try {
            while (c.moveToNext()) {
                Team t = new Team();
                t.id = c.getLong(c.getColumnIndexOrThrow("id"));
                t.userId = c.getLong(c.getColumnIndexOrThrow("user_id"));
                t.name = c.getString(c.getColumnIndexOrThrow("name"));
                out.add(t);
            }
            return out;
        } finally {
            if (c != null) c.close();
        }
    }

    public void updateTeamName(long teamId, String newName) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("name", newName);
        db.update("teams", cv, "id = ?", new String[]{String.valueOf(teamId)});
    }

    // -------------------------
    // TeamPokemon CRUD
    // -------------------------
    public long saveTeamPokemon(TeamPokemon tp) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("team_id", tp.teamId);
        cv.put("slot", tp.slot);
        cv.put("pokemon_name", tp.pokemonName);
        cv.put("custom_sprite_path", tp.customSpritePath);
        cv.put("cry_path", tp.cryPath);
        cv.put("ability", tp.ability);
        cv.put("evs", DbUtils.intArrayToJson(tp.evs));
        cv.put("ivs", DbUtils.intArrayToJson(tp.ivs));
        cv.put("moves", DbUtils.stringArrayToJson(tp.moves));

        if (tp.id > 0) {
            int updated = db.update("team_pokemons", cv, "id = ?", new String[]{String.valueOf(tp.id)});
            return updated > 0 ? tp.id : -1;
        } else {
            long id = db.insertWithOnConflict("team_pokemons", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
            return id;
        }
    }

    public TeamPokemon getTeamPokemonBySlot(long teamId, int slot) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query("team_pokemons", null, "team_id = ? AND slot = ?", new String[]{String.valueOf(teamId), String.valueOf(slot)}, null, null, null);
        try {
            if (c.moveToFirst()) {
                TeamPokemon tp = new TeamPokemon();
                tp.id = c.getLong(c.getColumnIndexOrThrow("id"));
                tp.teamId = c.getLong(c.getColumnIndexOrThrow("team_id"));
                tp.slot = c.getInt(c.getColumnIndexOrThrow("slot"));
                tp.pokemonName = c.getString(c.getColumnIndexOrThrow("pokemon_name"));
                tp.customSpritePath = c.getString(c.getColumnIndexOrThrow("custom_sprite_path"));
                tp.cryPath = c.getString(c.getColumnIndexOrThrow("cry_path"));
                tp.ability = c.getString(c.getColumnIndexOrThrow("ability"));
                tp.evs = DbUtils.jsonToIntArray(c.getString(c.getColumnIndexOrThrow("evs")), 6);
                tp.ivs = DbUtils.jsonToIntArray(c.getString(c.getColumnIndexOrThrow("ivs")), 6);
                tp.moves = DbUtils.jsonToStringArray(c.getString(c.getColumnIndexOrThrow("moves")), 4);
                return tp;
            }
            return null;
        } finally {
            if (c != null) c.close();
        }
    }

    public List<TeamPokemon> getTeamPokemons(long teamId) {
        List<TeamPokemon> out = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query("team_pokemons", null, "team_id = ?", new String[]{String.valueOf(teamId)}, null, null, "slot ASC");
        try {
            while (c.moveToNext()) {
                TeamPokemon tp = new TeamPokemon();
                tp.id = c.getLong(c.getColumnIndexOrThrow("id"));
                tp.teamId = c.getLong(c.getColumnIndexOrThrow("team_id"));
                tp.slot = c.getInt(c.getColumnIndexOrThrow("slot"));
                tp.pokemonName = c.getString(c.getColumnIndexOrThrow("pokemon_name"));
                tp.customSpritePath = c.getString(c.getColumnIndexOrThrow("custom_sprite_path"));
                tp.cryPath = c.getString(c.getColumnIndexOrThrow("cry_path"));
                tp.ability = c.getString(c.getColumnIndexOrThrow("ability"));
                tp.evs = DbUtils.jsonToIntArray(c.getString(c.getColumnIndexOrThrow("evs")), 6);
                tp.ivs = DbUtils.jsonToIntArray(c.getString(c.getColumnIndexOrThrow("ivs")), 6);
                tp.moves = DbUtils.jsonToStringArray(c.getString(c.getColumnIndexOrThrow("moves")), 4);
                out.add(tp);
            }
            return out;
        } finally {
            if (c != null) c.close();
        }
    }
}
