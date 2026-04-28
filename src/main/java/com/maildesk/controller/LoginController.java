package com.maildesk.controller;

import com.maildesk.service.AuthService;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class LoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label statusLabel;
    @FXML private Button loginButton;

    private final AuthService authService = new AuthService();

    @FXML
    public void handleLogin() {
        String email = emailField.getText().trim();
        String password = passwordField.getText();

        if (email.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Please enter email and password.");
            return;
        }

        loginButton.setDisable(true);
        statusLabel.setText("Signing in...");

        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() {
                return authService.login(email, password);
            }
        };

        task.setOnSucceeded(e -> {
            loginButton.setDisable(false);
            if (task.getValue()) {
                openDashboard();
            } else {
                statusLabel.setText("Invalid credentials. Check your email/password.");
            }
        });

        task.setOnFailed(e -> {
            loginButton.setDisable(false);
            statusLabel.setText("Error: " + task.getException().getMessage());
        });

        new Thread(task).start();
    }

    private void openDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/dashboard.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) emailField.getScene().getWindow();
            stage.setScene(new Scene(root, 900, 600));
            stage.setTitle("MailDesk — " + AuthService.getLoggedInEmail());
        } catch (Exception e) {
            statusLabel.setText("Failed to open dashboard.");
            e.printStackTrace();
        }
    }
}
