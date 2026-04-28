package com.maildesk.service;

import java.sql.Connection;
import java.sql.DriverManager;

public class DatabaseService {

    public Connection connect() throws Exception {
        return DriverManager.getConnection("jdbc:sqlite:maildesk.db");
    }
}
