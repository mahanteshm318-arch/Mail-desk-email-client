package com.maildesk.controller;

import com.maildesk.model.EmailMessage;
import com.maildesk.service.AuthService;
import com.maildesk.service.DatabaseService;
import com.maildesk.service.MailService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import java.util.List;

public class DashboardController {

    @FXML private VBox sidebar;
    @FXML private ListView<EmailMessage> inboxList;
    @FXML private WebView emailPreview;
    @FXML private Label statusLabel;
    @FXML private TextField searchField;

    private final DatabaseService db = DatabaseService.getInstance();
    private String currentAccount;
    private String currentFolder = "INBOX";
    private final ObservableList<EmailMessage> emails = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        currentAccount = AuthService.getLoggedInEmail();
        inboxList.setItems(emails);

        // Show email in preview when selected
        inboxList.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (selected != null) showEmail(selected);
        });

        // Custom cell to show subject + sender
        inboxList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(EmailMessage item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.toString());
                setStyle((!empty && item != null && !item.isRead())
                        ? "-fx-font-weight: bold;" : "-fx-font-weight: normal;");
            }
        });

        loadFromDB();
        syncFromServer();
    }

    // Load cached emails from SQLite instantly
    private void loadFromDB() {
        List<EmailMessage> cached = db.getEmails(currentAccount, currentFolder);
        emails.setAll(cached);
        if (statusLabel != null)
            statusLabel.setText("Loaded " + cached.size() + " emails from cache.");
    }

    // Fetch new emails from IMAP in background, save to SQLite
    private void syncFromServer() {
        if (statusLabel != null) statusLabel.setText("Syncing...");

        Task<List<EmailMessage>> task = new Task<>() {
            @Override
            protected List<EmailMessage> call() throws Exception {
                String password = db.getPassword(currentAccount);
                MailService ms = new MailService(currentAccount, password);
                List<EmailMessage> fetched = ms.fetchEmails(currentFolder, 50);
                for (EmailMessage m : fetched) db.saveEmail(m);
                return fetched;
            }
        };

        task.setOnSucceeded(e -> {
            loadFromDB(); // reload from SQLite with new messages merged
            if (statusLabel != null)
                statusLabel.setText("Synced. " + emails.size() + " emails.");
        });

        task.setOnFailed(e -> {
            if (statusLabel != null)
                statusLabel.setText("Sync failed — showing cached emails.");
        });

        new Thread(task).start();
    }

    private void showEmail(EmailMessage msg) {
        db.markRead(msg.getId());
        msg.setRead(true);
        inboxList.refresh();

        WebEngine engine = emailPreview.getEngine();
        String body = msg.getBody() != null ? msg.getBody() : "";
        if (!body.trim().startsWith("<")) {
            // Plain text — wrap in basic HTML
            body = "<html><body style='font-family:sans-serif;padding:16px;white-space:pre-wrap'>"
                    + body.replace("&", "&amp;").replace("<", "&lt;") + "</body></html>";
        }
        engine.loadContent(body);
    }

    @FXML
    public void handleCompose() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/compose.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("New Message");
            stage.setScene(new Scene(root, 600, 450));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleSearch() {
        String query = searchField != null ? searchField.getText().trim() : "";
        if (query.isEmpty()) {
            loadFromDB();
            return;
        }
        List<EmailMessage> results = db.searchEmails(currentAccount, query);
        emails.setAll(results);
        if (statusLabel != null)
            statusLabel.setText("Found " + results.size() + " results for \"" + query + "\"");
    }

    @FXML
    public void handleInbox() {
        currentFolder = "INBOX";
        loadFromDB();
        syncFromServer();
    }

    @FXML
    public void handleSent() {
        currentFolder = "[Gmail]/Sent Mail";
        loadFromDB();
        syncFromServer();
    }

    @FXML
    public void handleLogout() {
        AuthService.logout();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) inboxList.getScene().getWindow();
            stage.setScene(new Scene(root, 800, 500));
            stage.setTitle("MailDesk");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleRefresh() {
        syncFromServer();
    }
}
