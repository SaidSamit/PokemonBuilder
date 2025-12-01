package com.pokemones.pokemonbuilder.data;

import android.content.Context;

public class DbProvider {
    private static AppDbHelper instance;

    public static synchronized AppDbHelper get(Context ctx) {
        if (instance == null) instance = new AppDbHelper(ctx.getApplicationContext());
        return instance;
    }
}
