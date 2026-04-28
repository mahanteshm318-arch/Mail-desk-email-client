package com.maildesk.controller;

import com.maildesk.model.EmailMessage;
import com.maildesk.service.AuthService;
import com.maildesk.service.DatabaseService;
import com.maildesk.service.MailService;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.time.LocalDateTime;

public class ComposeController {

    @FXML private TextField toField;
    @FXML private TextField subjectField;
    @FXML private TextArea bodyField;
    @FXML private Label statusLabel;
    @FXML private Button sendButton;

    private final DatabaseService db = DatabaseService.getInstance();

    @FXML
    public void sendMail() {
        String to = toField.getText().trim();
        String subject = subjectField.getText().trim();
        String body = bodyField.getText();
        String account = AuthService.getLoggedInEmail();

        if (to.isEmpty()) {
            statusLabel.setText("Please enter a recipient.");
            return;
        }

        sendButton.setDisable(true);
        statusLabel.setText("Sending...");

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                String password = db.getPassword(account);
                MailService ms = new MailService(account, password);
                ms.sendEmail(to, subject, body);

                // Save to Sent in SQLite
                EmailMessage sent = new EmailMessage();
                sent.setUid("sent-" + System.currentTimeMillis());
                sent.setAccount(account);
                sent.setFolder("Sent");
                sent.setSubject(subject);
                sent.setSender(account);
                sent.setRecipients(to);
                sent.setBody(body);
                sent.setReceivedAt(LocalDateTime.now().toString());
                sent.setRead(true);
                db.saveEmail(sent);
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            statusLabel.setText("Sent!");
            sendButton.setDisable(false);
            // Close after short delay
            new Thread(() -> {
                try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
                javafx.application.Platform.runLater(() ->
                    ((Stage) sendButton.getScene().getWindow()).close()
                );
            }).start();
        });

        task.setOnFailed(e -> {
            statusLabel.setText("Failed: " + task.getException().getMessage());
            sendButton.setDisable(false);
        });

        new Thread(task).start();
    }

    @FXML
    public void handleCancel() {
        ((Stage) sendButton.getScene().getWindow()).close();
    }
}
