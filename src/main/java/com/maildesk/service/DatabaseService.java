package com.maildesk.service;

import com.maildesk.model.EmailMessage;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseService {

    private static final String DB_URL = "jdbc:sqlite:" +
            System.getProperty("user.home") + "/MailDesk/maildesk.db";

    private static DatabaseService instance;
    private Connection conn;

    private DatabaseService() {
        try {
            // Create app folder if missing
            java.io.File dir = new java.io.File(System.getProperty("user.home") + "/MailDesk");
            dir.mkdirs();

            conn = DriverManager.getConnection(DB_URL);
            conn.setAutoCommit(true);
            initTables();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to connect to SQLite: " + e.getMessage(), e);
        }
    }

    public static DatabaseService getInstance() {
        if (instance == null) instance = new DatabaseService();
        return instance;
    }

    // ── Schema ────────────────────────────────────────────────────────────────

    private void initTables() throws SQLException {
        Statement st = conn.createStatement();

        st.executeUpdate("""
            CREATE TABLE IF NOT EXISTS accounts (
                id        INTEGER PRIMARY KEY AUTOINCREMENT,
                email     TEXT UNIQUE NOT NULL,
                password  TEXT NOT NULL,
                imap_host TEXT DEFAULT 'imap.gmail.com',
                imap_port INTEGER DEFAULT 993,
                smtp_host TEXT DEFAULT 'smtp.gmail.com',
                smtp_port INTEGER DEFAULT 587
            )
        """);

        st.executeUpdate("""
            CREATE TABLE IF NOT EXISTS emails (
                id          INTEGER PRIMARY KEY AUTOINCREMENT,
                uid         TEXT,
                account     TEXT NOT NULL,
                folder      TEXT NOT NULL DEFAULT 'INBOX',
                subject     TEXT,
                sender      TEXT,
                recipients  TEXT,
                body        TEXT,
                received_at TEXT,
                is_read     INTEGER DEFAULT 0,
                UNIQUE(uid, account)
            )
        """);

        st.executeUpdate(
            "CREATE INDEX IF NOT EXISTS idx_emails_account_folder ON emails(account, folder)"
        );
    }

    // ── Account operations ────────────────────────────────────────────────────

    public boolean saveAccount(String email, String password) {
        try {
            PreparedStatement ps = conn.prepareStatement(
                "INSERT OR REPLACE INTO accounts (email, password) VALUES (?, ?)"
            );
            ps.setString(1, email);
            ps.setString(2, password);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public String getPassword(String email) {
        try {
            PreparedStatement ps = conn.prepareStatement(
                "SELECT password FROM accounts WHERE email = ?"
            );
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("password");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean accountExists(String email) {
        return getPassword(email) != null;
    }

    // ── Email operations ──────────────────────────────────────────────────────

    public void saveEmail(EmailMessage msg) {
        try {
            PreparedStatement ps = conn.prepareStatement("""
                INSERT OR IGNORE INTO emails
                  (uid, account, folder, subject, sender, recipients, body, received_at, is_read)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """);
            ps.setString(1, msg.getUid());
            ps.setString(2, msg.getAccount());
            ps.setString(3, msg.getFolder() != null ? msg.getFolder() : "INBOX");
            ps.setString(4, msg.getSubject());
            ps.setString(5, msg.getSender());
            ps.setString(6, msg.getRecipients());
            ps.setString(7, msg.getBody());
            ps.setString(8, msg.getReceivedAt());
            ps.setInt(9, msg.isRead() ? 1 : 0);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<EmailMessage> getEmails(String account, String folder) {
        List<EmailMessage> list = new ArrayList<>();
        try {
            PreparedStatement ps = conn.prepareStatement("""
                SELECT * FROM emails
                WHERE account = ? AND folder = ?
                ORDER BY received_at DESC
            """);
            ps.setString(1, account);
            ps.setString(2, folder);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                EmailMessage m = new EmailMessage();
                m.setId(rs.getInt("id"));
                m.setUid(rs.getString("uid"));
                m.setAccount(rs.getString("account"));
                m.setFolder(rs.getString("folder"));
                m.setSubject(rs.getString("subject"));
                m.setSender(rs.getString("sender"));
                m.setRecipients(rs.getString("recipients"));
                m.setBody(rs.getString("body"));
                m.setReceivedAt(rs.getString("received_at"));
                m.setRead(rs.getInt("is_read") == 1);
                list.add(m);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public void markRead(int emailId) {
        try {
            PreparedStatement ps = conn.prepareStatement(
                "UPDATE emails SET is_read = 1 WHERE id = ?"
            );
            ps.setInt(1, emailId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<EmailMessage> searchEmails(String account, String query) {
        List<EmailMessage> list = new ArrayList<>();
        try {
            String q = "%" + query + "%";
            PreparedStatement ps = conn.prepareStatement("""
                SELECT * FROM emails
                WHERE account = ? AND (subject LIKE ? OR sender LIKE ? OR body LIKE ?)
                ORDER BY received_at DESC
            """);
            ps.setString(1, account);
            ps.setString(2, q);
            ps.setString(3, q);
            ps.setString(4, q);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                EmailMessage m = new EmailMessage();
                m.setId(rs.getInt("id"));
                m.setSubject(rs.getString("subject"));
                m.setSender(rs.getString("sender"));
                m.setBody(rs.getString("body"));
                m.setRead(rs.getInt("is_read") == 1);
                list.add(m);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public void close() {
        try { if (conn != null) conn.close(); } catch (SQLException ignored) {}
    }
}
