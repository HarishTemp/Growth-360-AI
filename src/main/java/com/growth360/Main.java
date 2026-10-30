package com.growth360;

import com.growth360.util.DataSeeder;
import com.growth360.util.Schema;
import com.growth360.view.MainView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Main application entry point for Growth 360 AI.
 * Bootstraps database schema, initializes sample dataset if empty,
 * and launches the JavaFX desktop environment.
 */
public class Main extends Application {

    @Override
    public void init() throws Exception {
        System.out.println("==================================================");
        System.out.println("   TITAN WATCHES — Growth 360 AI Horology Analytics");
        System.out.println("   Titan Company Limited • Decision Intelligence");
        System.out.println("   CSE-AIML Mini Project • Team GIT BLAME");
        System.out.println("==================================================");

        // 1. Initialize DB Schema via pure Java DDL
        Schema.initSchema();

        // 2. Seed realistic Titan Watches business demo data
        DataSeeder.seedIfEmpty();
    }

    private MainView mainView;

    @Override
    public void start(Stage primaryStage) {
        mainView = new MainView();

        Scene scene = new Scene(mainView, 1360, 850);
        primaryStage.setTitle("Titan Watches — Growth 360 AI Decision Support & Horology Analytics");
        primaryStage.setMinWidth(1100);
        primaryStage.setMinHeight(700);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    @Override
    public void stop() throws Exception {
        if (mainView != null) {
            mainView.shutdown();
        }
        super.stop();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
