package com.maildesk.service;

public class AuthService {

    public boolean authenticate(String email, String password) {
        return !email.isEmpty() && !password.isEmpty();
    }
}
