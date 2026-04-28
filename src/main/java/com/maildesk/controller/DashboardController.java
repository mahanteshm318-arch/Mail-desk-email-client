package com.maildesk.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

public class DashboardController {

    @FXML
    private VBox sidebar;

    @FXML
    private ListView<String> inboxList;

    @FXML
    private WebView emailPreview;

    private final ObservableList<String> emails = FXCollections.observableArrayList();

    @FXML
    public void initialize() {

        emails.addAll(
                "Welcome Mail",
                "Your Account Activated",
                "Security Alert",
                "System Notification"
        );

        inboxList.setItems(emails);

        // Default preview
        WebEngine engine = emailPreview.getEngine();
        engine.loadContent("<h2>Welcome to MailDesk</h2><p>Select an email to preview</p>");
    }

    // Toggle Sidebar
    @FXML
    public void toggleSidebar() {
        boolean visible = sidebar.isVisible();
        sidebar.setVisible(!visible);
        sidebar.setManaged(!visible);
    }

    // Open Email Preview (HTML)
    @FXML
    public void openEmail() {
        String selected = inboxList.getSelectionModel().getSelectedItem();

        if (selected == null) return;

        WebEngine engine = emailPreview.getEngine();

        String html = """
                <html>
                <body style="font-family:Arial;padding:20px;">
                    <h2>%s</h2>
                    <hr>
                    <p>This is a <b>rich HTML email preview</b>.</p>
                    <p>You selected: <b>%s</b></p>
                    <p style="color:gray;">MailDesk Email Client UI</p>
                </body>
                </html>
                """.formatted(selected, selected);

        engine.loadContent(html);
    }

    // Open Compose Window
    @FXML
    public void openCompose() {
        try {
            Stage stage = new Stage();
            stage.setScene(new javafx.scene.Scene(
                    FXMLLoader.load(getClass().getResource("/fxml/compose.fxml"))
            ));
            stage.setTitle("Compose Email");
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Logout
    @FXML
    public void logout(javafx.event.ActionEvent event) {
        try {
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

            stage.setScene(new javafx.scene.Scene(
                    FXMLLoader.load(getClass().getResource("/fxml/login.fxml"))
            ));

            stage.setTitle("Login");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}