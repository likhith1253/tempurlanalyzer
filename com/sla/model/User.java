package com.sla.model;

public class User {
    public final String username;
    public final String userId;
    public final Role role;
    
    public User(String username, String userId, Role role) {
        this.username = username;
        this.userId = userId;
        this.role = role;
    }
}
