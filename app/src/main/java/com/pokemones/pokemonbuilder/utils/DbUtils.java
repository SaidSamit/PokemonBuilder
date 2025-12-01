package com.pokemones.pokemonbuilder.utils;

import org.json.JSONArray;

public class DbUtils {
    // Convierte int[] a JSON string
    public static String intArrayToJson(int[] arr) {
        if (arr == null) return null;
        try {
            JSONArray a = new JSONArray();
            for (int v : arr) a.put(v);
            return a.toString();
        } catch (Exception e) {
            return null;
        }
    }

    // Convierte JSON string a int[] con tamaño fijo (rellena con ceros si falta)
    public static int[] jsonToIntArray(String s, int expectedLength) {
        int[] out = new int[expectedLength];
        if (s == null) return out;
        try {
            JSONArray a = new JSONArray(s);
            for (int i = 0; i < expectedLength && i < a.length(); i++) out[i] = a.getInt(i);
        } catch (Exception e) {
            // devuelve array por defecto
        }
        return out;
    }

    // Convierte String[] a JSON string
    public static String stringArrayToJson(String[] arr) {
        if (arr == null) return null;
        try {
            JSONArray a = new JSONArray();
            for (String v : arr) a.put(v);
            return a.toString();
        } catch (Exception e) {
            return null;
        }
    }

    // Convierte JSON string a String[] con tamaño fijo (rellena con nulls si falta)
    public static String[] jsonToStringArray(String s, int expectedLength) {
        String[] out = new String[expectedLength];
        if (s == null) return out;
        try {
            JSONArray a = new JSONArray(s);
            for (int i = 0; i < expectedLength && i < a.length(); i++) out[i] = a.getString(i);
        } catch (Exception e) {
            // devuelve array por defecto
        }
        return out;
    }
}
