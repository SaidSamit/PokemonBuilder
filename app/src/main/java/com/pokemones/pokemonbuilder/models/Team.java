package com.pokemones.pokemonbuilder.models;

public class Team {
    public long id;
    public long userId;
    public String name;

    public Team() {}

    public Team(long userId, String name) {
        this.userId = userId;
        this.name = name;
    }
}
