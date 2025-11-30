package com.pokemones.pokemonbuilder.models;

public class TeamPokemon {
    public long id;
    public long teamId;
    public int slot; // 0..5
    public String pokemonName; // nombre o id para PokeAPI
    public String customSpritePath; // URI local si el usuario toma foto
    public String cryPath; // URI audio grabado
    public String ability;
    public int[] evs = new int[6];
    public int[] ivs = new int[6];
    public String[] moves = new String[4];

    public TeamPokemon() {}
}
