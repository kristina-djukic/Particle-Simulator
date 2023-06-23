package com.example.kristinaparticle;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.geometry.Pos;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class ParticleSimulator extends Application {

    private List<Particles> particles;

    double width = 800;
    double height = 600;
    int cycles;
    double totalTime;
    Label runTimeTotal;
    Canvas canvas;
    private AnimationTimer timer;
    CheckBox graphics;

    private static class Particles {
        double positionXAxis, positionYAxis;
        double speedX, speedY;
        double charge;
        double radius;

        public Particles(double positionXAxis, double positionYAxis, double speedX, double speedY, double charge, double radius) {
            this.positionXAxis = positionXAxis;
            this.positionYAxis = positionYAxis;
            this.speedX = speedX;
            this.speedY = speedY;
            this.charge = charge;
            this.radius = radius;
        }
    }

    @Override
    public void start(Stage stage) {
        BorderPane layout = new BorderPane();
        canvas = new Canvas(width, height-40);
        canvas.widthProperty().bind(layout.widthProperty());
        canvas.heightProperty().bind(layout.heightProperty().subtract(40));

        HBox input = new HBox();
        input.setSpacing(10);
        input.setAlignment(Pos.CENTER);
        input.setPadding(new Insets(0,0,20,0));
        input.setMinHeight(20);


        graphics = new CheckBox("Graphics");
        Label particlesText = new Label("Enter number of particles:");
        TextField particlesField = new TextField();
        Label cyclesText = new Label("Enter number of cycles:");
        TextField cyclesField = new TextField();
        Button run = new Button("Run");
        runTimeTotal = new Label("Runtime: null");

        input.getChildren().addAll(graphics, particlesText, particlesField, cyclesText, cyclesField, run, runTimeTotal);
        layout.setBottom(input);
        layout.setCenter(canvas);


        run.setOnAction(event -> {
            if(graphics.isSelected()){
            if (timer != null) {//provjeravam je li prethodni tajmer zavrsio ako nije onda stop
                timer.stop();
            }
            int numParticles = Integer.parseInt(particlesField.getText());
            int endCycles = Integer.parseInt(cyclesField.getText());
            runSimulation(numParticles, endCycles, "C:\\Users\\Kristina\\Desktop\\positions.txt");
            runTimeTotal.setText("Runtime: Measuring...");}else {
                System.out.println("Measuring...");
                int numParticles = Integer.parseInt(particlesField.getText());
                int endCycles = Integer.parseInt(cyclesField.getText());
                stage.close();
                runSimulationWithoutGraphics(numParticles, endCycles, "C:\\Users\\Kristina\\Desktop\\positions.txt");
            }
        });

        Scene scene = new Scene(layout, width, height);
        stage.setScene(scene);
        stage.show();
        stage.toFront();
    }


    private void runSimulationWithoutGraphics(int numParticles, int endCycles, String filePath){

        long startTime = System.nanoTime();
        particles = new ArrayList<>();
        readInitialParticlePositions(filePath);
        /*for (int i = 0; i < numParticles; i++) {
            double posX = Math.random() * width;
            double posY = Math.random() * height;
            double speedX = Math.random() * 10 - 5;
            double speedY = Math.random() * 10 - 5;
            double charge;
            if(Math.random() > 0.5){
                charge = 1;
            }else{
                charge = -1;
            }
            double radius = 6;
            particles.add(new Particles(posX, posY, speedX, speedY, charge, radius));
        }*/

        for (cycles = 0; cycles <= endCycles; cycles++) {

            for (Particles particle : particles) {
                particle.positionXAxis += particle.speedX;
                particle.positionYAxis += particle.speedY;

                boundaries(particle);
            }

            formulaCalculation(particles);

        }

            long totalTimeNao = System.nanoTime() - startTime;
            totalTime = (double) (totalTimeNao / 1000000000.0);
            System.out.println("Runtime (in seconds): " + totalTime);

    }

    private void runSimulation(int numParticles, int endCycles, String filePath) {
        long startTime = System.nanoTime();
        particles = new ArrayList<>();
        readInitialParticlePositions(filePath);
        /*
        for (int i = 0; i < numParticles; i++) {
            double posX = Math.random() * width;
            double posY = Math.random() * height;
            double speedX = Math.random() * 10 - 5;
            double speedY = Math.random() * 10 - 5;
            double charge;
            if(Math.random() > 0.5){
                charge = 1;
            }else{
                charge = -1;
            }
            double radius = 6;
            particles.add(new Particles(posX, posY, speedX, speedY, charge, radius));
        }
         */

            cycles = 0;
            timer = new AnimationTimer() {

                @Override
                public void handle(long now) {

                    cycles++;

                    canvas.getGraphicsContext2D().clearRect(0, 0, canvas.getWidth(), canvas.getHeight());//we must clear the canvas in the beginning such that we do not have a trail behind the particles


                    for (Particles particle : particles) {
                        particle.positionXAxis += particle.speedX;
                        particle.positionYAxis += particle.speedY;

                        boundaries(particle);

                        drawParticles(particle);

                    }
                    formulaCalculation(particles);

                    if (cycles >= endCycles) {
                        this.stop();
                        long totalTimeNao = System.nanoTime() - startTime;
                        totalTime = (double) (totalTimeNao / 1000000000.0);
                        System.out.println("Runtime (in seconds): " + totalTime);
                        runTimeTotal.setText("Runtime: " + totalTime + " sec.");
                    }
                }
            };

            timer.start();

        }
    private void readInitialParticlePositions(String filePath) {
        particles.clear();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 2) {
                    double positionXAxis = Double.parseDouble(parts[0]);
                    double positionYAxis = Double.parseDouble(parts[1]);
                    double speedX = Math.random() * 10 - 5;
                    double speedY = Math.random() * 10 - 5;
                    double charge = Math.random() > 0.5 ? 1 : -1;
                    double radius = 6;
                    particles.add(new Particles(positionXAxis, positionYAxis, speedX, speedY, charge, radius));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void boundaries(Particles particle) {
        if (particle.positionXAxis < particle.radius) {
            particle.positionXAxis = particle.radius;
            particle.speedX = -particle.speedX;
        } else if (particle.positionXAxis > canvas.getWidth() - particle.radius) {
            particle.positionXAxis = canvas.getWidth() - particle.radius;
            particle.speedX = -particle.speedX;
        }
        if (particle.positionYAxis < particle.radius) {
            particle.positionYAxis = particle.radius;
            particle.speedY = -particle.speedY;
        } else if (particle.positionYAxis > (canvas.getHeight()) - particle.radius) {
            particle.positionYAxis = (canvas.getHeight()) - particle.radius;
            particle.speedY = -particle.speedY;
        }
    }

    private void drawParticles(Particles particle) {
        if (particle.charge == -1) {
            canvas.getGraphicsContext2D().setFill(Color.ORANGERED);
        } else {
            canvas.getGraphicsContext2D().setFill(Color.BLUEVIOLET);
        }
        canvas.getGraphicsContext2D().fillOval(particle.positionXAxis - particle.radius,particle.positionYAxis - particle.radius, 2 * particle.radius, 2 * particle.radius);
    }

    private void formulaCalculation(List<Particles> particles) {
        double k =10;
        double minDistance = 20;

        for (int i = 0; i < particles.size(); i++) {
            Particles particle1 = particles.get(i);
            for (int j = i + 1; j < particles.size(); j++) {
                Particles particle2 = particles.get(j);
                double distanceX = particle2.positionXAxis - particle1.positionXAxis;
                double distanceY = particle2.positionYAxis - particle1.positionYAxis;
                double distance = Math.sqrt(distanceX * distanceX + distanceY * distanceY);

                distance = Math.max(distance, minDistance);

                double force = -k * (particle1.charge * particle2.charge) / (distance * distance);
                double forceX = force * distanceX;
                double forceY = force * distanceY;
                particle1.speedX += forceX;
                particle1.speedY += forceY;
                particle2.speedX -= forceX;
                particle2.speedY -= forceY;
            }
        }
    }

    public static void main(String[] args) {
        launch();
    }
}