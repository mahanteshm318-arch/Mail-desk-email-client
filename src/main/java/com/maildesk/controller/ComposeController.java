package com.maildesk.controller;

import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public class ComposeController {

    @FXML
    private TextField toField;

    @FXML
    private TextField subjectField;

    @FXML
    private TextArea bodyField;

    @FXML
    public void sendMail() {
        System.out.println("To: " + toField.getText());
        System.out.println("Subject: " + subjectField.getText());
        System.out.println("Body: " + bodyField.getText());
    }
}