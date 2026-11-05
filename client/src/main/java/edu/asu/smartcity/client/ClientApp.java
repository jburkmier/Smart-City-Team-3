package org.example;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage primaryStage) {

        // Display HomePage instead of the old button
        HomePage homePage = new HomePage();

        Scene scene = new Scene(homePage, 600, 500);

        scene.getStylesheets().add(
            getClass().getResource("/style.css").toExternalForm()
        );

        primaryStage.setTitle("Smart City Application");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}