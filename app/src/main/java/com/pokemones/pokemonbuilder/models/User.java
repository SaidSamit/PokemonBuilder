package com.pokemones.pokemonbuilder.models;

public class User {
    public long id;
    public String username;
    public String password;

    public User() {}

    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }
}
