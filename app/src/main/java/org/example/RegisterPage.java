package org.example;

import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.Pos;

public class RegisterPage extends GridPane {
    public RegisterPage() {
        setAlignment(Pos.CENTER);
        getStyleClass().add("register-page");

        Label title = new Label("Register");
        title.getStyleClass().add("register-title");

        Label emailLabel = new Label("Email");
        TextField emailField = new TextField();
        emailLabel.setLabelFor(emailField);
        emailField.setPromptText("Enter your email");
        emailField.getStyleClass().add("register-field");

        Label passwordLabel = new Label("Password");
        PasswordField passwordField = new PasswordField();
        passwordLabel.setLabelFor(passwordField);
        passwordField.setPromptText("Enter your password");
        passwordField.getStyleClass().add("register-field");

        Label confirmLabel = new Label("Confirm Password");
        PasswordField confirmPasswordField = new PasswordField();
        confirmLabel.setLabelFor(confirmPasswordField);
        confirmPasswordField.setPromptText("Confirm your password");
        confirmPasswordField.getStyleClass().add("register-field");

        VBox form = new VBox(8, emailLabel, emailField, passwordLabel, passwordField, confirmLabel, confirmPasswordField);
        form.getStyleClass().add("login-form");

        Button registerButton = new Button("Register");
        Button backButton = new Button("Back to Home");
        registerButton.getStyleClass().add("button");
        backButton.getStyleClass().add("button");

        registerButton.setOnAction(event -> getScene().setRoot(new LoginPage()));
        backButton.setOnAction(event -> getScene().setRoot(new HomePage()));

        VBox loginCard = new VBox(18, title, form, registerButton, backButton);
        loginCard.getStyleClass().add("login-card");
        add(loginCard, 0, 0);
    }
}
