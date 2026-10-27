package org.example;

import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.geometry.Insets;

public class HomePage extends GridPane {

    public HomePage() {

        Label smartCity = new Label("Smart City");
        smartCity.getStyleClass().add("page-title");

        HBox header = new HBox(smartCity);
        header.getStyleClass().add("header");
        header.setMaxWidth(Double.MAX_VALUE);

        ColumnConstraints column = new ColumnConstraints();
        column.setHgrow(Priority.ALWAYS);
        getColumnConstraints().add(column);

        add(header, 0, 0);

        VBox content = new VBox();
        add(content, 0, 1);

        RowConstraints contentRow = new RowConstraints();
        contentRow.setVgrow(Priority.ALWAYS);
        getRowConstraints().addAll(new RowConstraints(), contentRow);
    }
}