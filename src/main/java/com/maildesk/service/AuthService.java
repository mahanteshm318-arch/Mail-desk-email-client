package com.maildesk.service;

public class AuthService {

    private static String loggedInEmail;
    private final DatabaseService db = DatabaseService.getInstance();

    // Validate credentials against SQLite and also verify IMAP connection
    public boolean login(String email, String password) {
        // If account in DB, check password
        if (db.accountExists(email)) {
            String stored = db.getPassword(email);
            if (stored != null && stored.equals(password)) {
                loggedInEmail = email;
                return true;
            }
            return false;
        }
        // New account — verify via IMAP then save
        try {
            MailService ms = new MailService(email, password);
            ms.testConnection(); // throws if bad credentials
            db.saveAccount(email, password);
            loggedInEmail = email;
            return true;
        } catch (Exception e) {
            System.out.println("Login failed: " + e.getMessage());
            return false;
        }
    }

    public static String getLoggedInEmail() {
        return loggedInEmail;
    }

    public static void logout() {
        loggedInEmail = null;
    }
}
