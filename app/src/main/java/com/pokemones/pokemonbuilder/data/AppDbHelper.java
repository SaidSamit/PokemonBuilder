package com.pokemones.pokemonbuilder.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.pokemones.pokemonbuilder.models.Team;
import com.pokemones.pokemonbuilder.models.TeamPokemon;
import com.pokemones.pokemonbuilder.models.User;

import java.util.ArrayList;
import java.util.List;

public class AppDbHelper extends SQLiteOpenHelper {
    private static final String TAG = "AppDbHelper";
    private static final String DB_NAME = "pokemonbuilder.db";
    private static final int DB_VERSION = 3;

    // tablas
    private static final String TABLE_USERS = "users";
    private static final String TABLE_TEAMS = "teams";
    private static final String TABLE_TEAM_POKEMON = "team_pokemons";

    // columnas users
    private static final String U_ID = "id";
    private static final String U_USERNAME = "username";
    private static final String U_PASSWORD = "password";

    // columnas teams
    private static final String T_ID = "id";
    private static final String T_USER_ID = "user_id";
    private static final String T_NAME = "name";

    // columnas team_pokemons
    private static final String P_ID = "id";
    private static final String P_TEAM_ID = "team_id";
    private static final String P_SLOT = "slot";
    private static final String P_NAME = "pokemon_name";
    private static final String P_CUSTOM_SPRITE = "custom_sprite_path";
    private static final String P_CRY_PATH = "cry_path";

    public AppDbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createUsers = "CREATE TABLE " + TABLE_USERS + " (" +
                U_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                U_USERNAME + " TEXT UNIQUE, " +
                U_PASSWORD + " TEXT" +
                ");";

        String createTeams = "CREATE TABLE " + TABLE_TEAMS + " (" +
                T_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                T_USER_ID + " INTEGER, " +
                T_NAME + " TEXT" +
                ");";

        String createTeamPokemons = "CREATE TABLE " + TABLE_TEAM_POKEMON + " (" +
                P_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                P_TEAM_ID + " INTEGER, " +
                P_SLOT + " INTEGER, " +
                P_NAME + " TEXT, " +
                P_CUSTOM_SPRITE + " TEXT, " +
                P_CRY_PATH + " TEXT, " +
                "FOREIGN KEY(" + P_TEAM_ID + ") REFERENCES " + TABLE_TEAMS + "(" + T_ID + ")" +
                ");";

        db.execSQL(createUsers);
        db.execSQL(createTeams);
        db.execSQL(createTeamPokemons);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TEAM_POKEMON);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TEAMS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        onCreate(db);
    }

    // -----------------------
    // Métodos de users
    // -----------------------

    // Crea un usuario (username, password). Devuelve id o -1
    public long createUser(String username, String password) {
        long id = -1;
        SQLiteDatabase db = null;
        try {
            db = getWritableDatabase();
            ContentValues cv = new ContentValues();
            cv.put(U_USERNAME, username);
            cv.put(U_PASSWORD, password);
            id = db.insert(TABLE_USERS, null, cv);
            Log.i(TAG, "createUser id=" + id);
        } catch (Exception e) {
            Log.e(TAG, "createUser error", e);
        } finally {
            if (db != null) db.close();
        }
        return id;
    }

    // Login: devuelve User si coincide username+password, o null
    public User login(String username, String password) {
        SQLiteDatabase db = null;
        Cursor c = null;
        try {
            db = getReadableDatabase();
            c = db.query(TABLE_USERS, null, U_USERNAME + "=? AND " + U_PASSWORD + "=?", new String[]{username, password}, null, null, null);
            if (c != null && c.moveToFirst()) {
                User u = new User();
                u.setId(c.getLong(c.getColumnIndexOrThrow(U_ID)));
                u.setUsername(c.getString(c.getColumnIndexOrThrow(U_USERNAME)));
                u.setPassword(c.getString(c.getColumnIndexOrThrow(U_PASSWORD)));
                return u;
            }
        } catch (Exception e) {
            Log.e(TAG, "login error", e);
        } finally {
            if (c != null) c.close();
            if (db != null) db.close();
        }
        return null;
    }

    // -----------------------
    // Métodos teams y pokemons
    // -----------------------

    public long createTeam(long userId, String name) {
        long id = -1;
        SQLiteDatabase db = null;
        try {
            db = getWritableDatabase();
            ContentValues cv = new ContentValues();
            cv.put(T_USER_ID, userId);
            cv.put(T_NAME, name);
            id = db.insert(TABLE_TEAMS, null, cv);
            Log.i(TAG, "createTeam id=" + id);
        } catch (Exception e) {
            Log.e(TAG, "createTeam error", e);
        } finally {
            if (db != null) db.close();
        }
        return id;
    }

    public List<Team> getTeamsForUser(long userId) {
        List<Team> out = new ArrayList<>();
        SQLiteDatabase db = null;
        Cursor c = null;
        try {
            db = getReadableDatabase();
            c = db.query(TABLE_TEAMS, null, T_USER_ID + "=?", new String[]{String.valueOf(userId)}, null, null, null);
            while (c.moveToNext()) {
                Team t = new Team();
                t.id = c.getLong(c.getColumnIndexOrThrow(T_ID));
                t.userId = c.getLong(c.getColumnIndexOrThrow(T_USER_ID));
                t.name = c.getString(c.getColumnIndexOrThrow(T_NAME));
                out.add(t);
            }
        } catch (Exception e) {
            Log.e(TAG, "getTeamsForUser error", e);
        } finally {
            if (c != null) c.close();
            if (db != null) db.close();
        }
        return out;
    }

    public long saveTeamPokemon(TeamPokemon tp) {
        long res = -1;
        SQLiteDatabase db = null;
        Cursor c = null;
        try {
            db = getWritableDatabase();
            ContentValues cv = new ContentValues();
            cv.put(P_TEAM_ID, tp.teamId);
            cv.put(P_SLOT, tp.slot);
            cv.put(P_NAME, tp.pokemonName);
            cv.put(P_CUSTOM_SPRITE, tp.customSpritePath);
            cv.put(P_CRY_PATH, tp.cryPath);

            int updated = db.update(TABLE_TEAM_POKEMON, cv, P_TEAM_ID + "=? AND " + P_SLOT + "=?", new String[]{String.valueOf(tp.teamId), String.valueOf(tp.slot)});
            if (updated > 0) {
                c = db.query(TABLE_TEAM_POKEMON, new String[]{P_ID}, P_TEAM_ID + "=? AND " + P_SLOT + "=?", new String[]{String.valueOf(tp.teamId), String.valueOf(tp.slot)}, null, null, null);
                if (c != null && c.moveToFirst()) {
                    res = c.getLong(c.getColumnIndexOrThrow(P_ID));
                }
            } else {
                res = db.insert(TABLE_TEAM_POKEMON, null, cv);
            }
            Log.i(TAG, "saveTeamPokemon res=" + res);
        } catch (Exception e) {
            Log.e(TAG, "saveTeamPokemon error", e);
        } finally {
            if (c != null) c.close();
            if (db != null) db.close();
        }
        return res;
    }

    public List<TeamPokemon> getTeamPokemons(long teamId) {
        List<TeamPokemon> out = new ArrayList<>();
        SQLiteDatabase db = null;
        Cursor c = null;
        try {
            db = getReadableDatabase();
            c = db.query(TABLE_TEAM_POKEMON, null, P_TEAM_ID + "=?", new String[]{String.valueOf(teamId)}, null, null, P_SLOT + " ASC");
            while (c.moveToNext()) {
                TeamPokemon tp = new TeamPokemon();
                tp.id = c.getLong(c.getColumnIndexOrThrow(P_ID));
                tp.teamId = c.getLong(c.getColumnIndexOrThrow(P_TEAM_ID));
                tp.slot = c.getInt(c.getColumnIndexOrThrow(P_SLOT));
                tp.pokemonName = c.getString(c.getColumnIndexOrThrow(P_NAME));
                tp.customSpritePath = c.getString(c.getColumnIndexOrThrow(P_CUSTOM_SPRITE));
                tp.cryPath = c.getString(c.getColumnIndexOrThrow(P_CRY_PATH));
                out.add(tp);
            }
        } catch (Exception e) {
            Log.e(TAG, "getTeamPokemons error", e);
        } finally {
            if (c != null) c.close();
            if (db != null) db.close();
        }
        return out;
    }

    public TeamPokemon getTeamPokemonBySlot(long teamId, int slot) {
        SQLiteDatabase db = null;
        Cursor c = null;
        try {
            db = getReadableDatabase();
            c = db.query(TABLE_TEAM_POKEMON, null, P_TEAM_ID + "=? AND " + P_SLOT + "=?", new String[]{String.valueOf(teamId), String.valueOf(slot)}, null, null, null);
            if (c != null && c.moveToFirst()) {
                TeamPokemon tp = new TeamPokemon();
                tp.id = c.getLong(c.getColumnIndexOrThrow(P_ID));
                tp.teamId = c.getLong(c.getColumnIndexOrThrow(P_TEAM_ID));
                tp.slot = c.getInt(c.getColumnIndexOrThrow(P_SLOT));
                tp.pokemonName = c.getString(c.getColumnIndexOrThrow(P_NAME));
                tp.customSpritePath = c.getString(c.getColumnIndexOrThrow(P_CUSTOM_SPRITE));
                tp.cryPath = c.getString(c.getColumnIndexOrThrow(P_CRY_PATH));
                return tp;
            }
        } catch (Exception e) {
            Log.e(TAG, "getTeamPokemonBySlot error", e);
        } finally {
            if (c != null) c.close();
            if (db != null) db.close();
        }
        return null;
    }
}
