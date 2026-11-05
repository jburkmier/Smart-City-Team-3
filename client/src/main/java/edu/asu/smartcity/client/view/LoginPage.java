package org.example;

import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.Pos;

public class LoginPage extends GridPane {
    public LoginPage() {
        setAlignment(Pos.CENTER);
        getStyleClass().add("login-page");

        Label title = new Label("Log in or Register");
        title.getStyleClass().add("login-title");

        Label emailLabel = new Label("Email");
        TextField emailField = new TextField();
        emailLabel.setLabelFor(emailField);
        emailField.setPromptText("Enter your email");
        emailField.getStyleClass().add("login-field");

        Label passwordLabel = new Label("Password");
        PasswordField passwordField = new PasswordField();
        passwordLabel.setLabelFor(passwordField);
        passwordField.setPromptText("Enter your password");
        passwordField.getStyleClass().add("login-field");

        VBox form = new VBox(8, emailLabel, emailField, passwordLabel, passwordField);
        form.getStyleClass().add("login-form");

        Button loginButton = new Button("Login");
        Button registerButton = new Button("Register");
        Button backButton = new Button("Back to Home");
        loginButton.getStyleClass().add("button");
        registerButton.getStyleClass().add("button");
        backButton.getStyleClass().add("button");

        registerButton.setOnAction(event -> getScene().setRoot(new RegisterPage()));
        backButton.setOnAction(event -> getScene().setRoot(new HomePage()));

        VBox loginCard = new VBox(18, title, form, loginButton, registerButton, backButton);
        loginCard.getStyleClass().add("login-card");
        add(loginCard, 0, 0);
    }
}
