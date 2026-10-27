package org.example;

import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;


public class HomePage extends GridPane {

    public HomePage() {

        Label smartCity = new Label("Smart City");
        smartCity.getStyleClass().add("page-title");

        //Login button
        Button loginRegisterButton = new Button("Login or Register");
        loginRegisterButton.getStyleClass().add("button");
        loginRegisterButton.setOnAction(event -> getScene().setRoot(new LoginPage()));

        HBox buttons = new HBox(10);
        buttons.getChildren().addAll(loginRegisterButton);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);


        HBox header = new HBox(15);
        header.getChildren().addAll(
            smartCity,
            spacer,
            buttons
        );
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("header");
        header.setMaxWidth(Double.MAX_VALUE);

        ColumnConstraints column = new ColumnConstraints();
        column.setHgrow(Priority.ALWAYS);
        getColumnConstraints().add(column);

        add(header, 0, 0);

        VBox content = new VBox();
        content.getStyleClass().add("content");
        add(content, 0, 1);

        RowConstraints headerRow = new RowConstraints();
        RowConstraints contentRow = new RowConstraints();
        contentRow.setVgrow(Priority.ALWAYS);

        getRowConstraints().addAll(headerRow, contentRow);
    }
}