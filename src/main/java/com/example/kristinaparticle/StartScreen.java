package com.example.kristinaparticle;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import java.io.File;
import java.util.Random;

public class StartScreen extends Application {

    private Canvas canvas;
    private GraphicsContext gc;

    @Override
    public void start(Stage primaryStage) {
        StackPane root = new StackPane();

        canvas = new Canvas(500, 400);
        gc = canvas.getGraphicsContext2D();

        Label titleLabel = new Label("Particles Simulator");
        titleLabel.setStyle("-fx-font-size: 35px; -fx-font-weight: bold;");

        VBox titleBox = new VBox();
        titleBox.setAlignment(Pos.CENTER);
        titleBox.getChildren().add(titleLabel);
        titleBox.setPadding(new Insets(0, 0, 100, 0));

        VBox vbox = new VBox();
        vbox.setAlignment(Pos.CENTER);
        vbox.setSpacing(10);
        vbox.setPadding(new Insets(200, 0, 0, 0));

        Label resultsLabel = new Label();

        Button seqButton = new Button("Sequential Simulator");
        seqButton.setPrefWidth(150);
        seqButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                ParticleSimulator simulator = new ParticleSimulator();
                simulator.start(new Stage());
            }
        });

        Button parButton = new Button("Parallel Simulator");
        parButton.setPrefWidth(150);
        parButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                ParticleSimulatorParallel simulator = new ParticleSimulatorParallel();
                simulator.start(new Stage());
            }
        });

        Button disButton = new Button("Distributive Simulator");
        disButton.setPrefWidth(150);
        disButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                try {

                    File location = new File("C:\\Users\\Kristina\\Desktop\\KristinaParticle\\src\\main\\java\\com\\example\\kristinaparticle");

                    ProcessBuilder compileBuilder = new ProcessBuilder("javac", "-cp", ".;%MPJ_HOME%\\lib\\mpj.jar", "ParticleSimulatorDistributive.java");
                    compileBuilder.directory(location);
                    Process compileProcess = compileBuilder.start();
                    compileProcess.waitFor();

                    ProcessBuilder mpjBuilder = new ProcessBuilder("C:\\Users\\Kristina\\Documents\\mpj-v0_44\\bin\\mpjrun.bat", "-np", "8", "-cp", "C:\\Users\\Kristina\\Desktop\\KristinaParticle\\src\\main\\java", "com.example.kristinaparticle.ParticlesSimulatorDistributive");
                    mpjBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
                    Process mpjProcess = mpjBuilder.start();
                    mpjProcess.waitFor();

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        vbox.getChildren().addAll(seqButton, parButton, disButton, resultsLabel);

        root.getChildren().addAll(canvas, titleBox, vbox);

        Scene scene = new Scene(root, 500, 400);
        primaryStage.setScene(scene);
        primaryStage.show();

        drawParticles();
    }


    private void drawParticles() {
        Random random = new Random();
        gc.setFill(Color.ORANGERED);

        for (int i = 0; i <70; i++) {
            double x = random.nextDouble() * canvas.getWidth();
            double y = random.nextDouble() * canvas.getHeight();

            gc.fillOval(x, y, 5, 5);
        }
        gc.setFill(Color.BLUEVIOLET);

        for (int i = 0; i <70; i++) {
            double x = random.nextDouble() * canvas.getWidth();
            double y = random.nextDouble() * canvas.getHeight();

            gc.fillOval(x, y, 5, 5);
        }
    }

    public static void main(String[] args) {
        launch();
    }
}
