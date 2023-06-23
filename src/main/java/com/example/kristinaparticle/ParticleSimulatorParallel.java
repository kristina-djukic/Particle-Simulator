package com.example.kristinaparticle;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.CheckBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.concurrent.Task;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;


public class ParticleSimulatorParallel extends Application {

    private List<Particles> particles;
    private int numOfThreads;
    double width = 800;
    double height = 600;
    int cycles;
    double totalTime;
    Label runTimeTotal; //declared globally because we need access in the button action listener.
    Canvas canvas;
    private AnimationTimer timer;
    private ExecutorService executor;
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
        BorderPane layout = new BorderPane(); //layout kako su canvas i vbox rasporedjene na window

        canvas = new Canvas(width, height-40); //prostor gdje crtam particles
        canvas.widthProperty().bind(layout.widthProperty());
        canvas.heightProperty().bind(layout.heightProperty().subtract(40)); // subtracting 40 to account for inputBox


        HBox input = new HBox(); //horizontal container where I put buttons, labels
        input.setSpacing(10);  //prostor izmedju buttons, text fields
        input.setAlignment(Pos.CENTER);
        input.setPadding(new Insets(0,0,20,0));
        input.setMinHeight(20);

        graphics = new CheckBox("Graphics");
        Label particlesLabel = new Label("Enter number of particles:");
        TextField particlesField = new TextField();
        Label cyclesLabel = new Label("Enter number of cycles:");
        TextField cyclesField = new TextField();
        Button runButton = new Button("Run");
        runTimeTotal = new Label("Runtime: null");

        input.getChildren().addAll(graphics, particlesLabel, particlesField, cyclesLabel, cyclesField, runButton, runTimeTotal); //place all of the above items in the HBox, order matters from left to right
        layout.setBottom(input); //place the inputBox (the this on the bottom) on the bottom border of the layout
        layout.setCenter(canvas); //place the canvas to the center


        numOfThreads = Runtime.getRuntime().availableProcessors(); //koliko kompjuter ima tredova

        runButton.setOnAction(event -> {//Run button on action
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

        Scene scene = new Scene(layout, width, height);//stavljam layout u scene, kako da budu elementi rasporedjeni u sceni
        stage.setScene(scene);//stavljam scenu u stage, sta je u programu
        stage.show();//pokazuje stage
        stage.toFront();
    }

    private void runSimulationWithoutGraphics(int numParticles, int endCycles, String filePath){
        executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
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

        for (cycles = 0; cycles <= endCycles; cycles++) {
            for (Particles particle : particles) {
                particle.positionXAxis += particle.speedX;//dodamo xVelocity vector na xPos of particle (kao vektori iz ALg 1)
                particle.positionYAxis += particle.speedY; //dodamo yVelocity vector na yPos of particle
                boundaries(particle);
            }
            int particlesPerThread = numParticles / numOfThreads;//sada dijelimo koliko cu imati particles per thread
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < numOfThreads; i++) {
                int startingIndex = i * particlesPerThread;
                int endingIndex;
                if (i == numOfThreads - 1) {
                    endingIndex = numParticles; //ako je posljednji onda je krajnji broj cestica ending
                } else {
                    endingIndex = startingIndex + particlesPerThread;//ako nije onda onda pocetni plus broj cestica po tredu
                }
                ParticleTask task = new ParticleTask(particles, startingIndex, endingIndex);
                futures.add(executor.submit(task));
            }
            for (Future<?> future : futures) {
                try {
                    future.get(); // waiting for all tasks to complete before proceeding to the next cycle
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }

            }
        }

        if (executor != null) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(800, TimeUnit.MILLISECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
            }
        }
        long totalTimeNao = System.nanoTime() - startTime;
        totalTime = (double) (totalTimeNao / 1000000000.0);
        System.out.println("Runtime (in seconds): " + totalTime);

    }





    private void runSimulation(int numParticles, int endCycles, String filePath) {
        executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        long startTime = System.nanoTime();
        particles = new ArrayList<>();
        readInitialParticlePositions(filePath);
        /*
        for (int i = 0; i < numParticles; i++) { //create the particles, n number of them
            double x = Math.random() * width; //random coordinates in accordance to width and height
            double y = Math.random() * height;
            double xVelocity = Math.random() * 10 - 5;
            double yVelocity = Math.random() * 10 - 5;
            double charge;
            if(Math.random() > 0.5){
                charge = 1;
            }else{
                charge = -1;
            }
            double radius = 6; //velicina cestice
            particles.add(new Particles(x, y, xVelocity, yVelocity, charge, radius));
        }

         */

        cycles = 0;

        timer = new AnimationTimer() {

            @Override
            public void handle(long now) {
                cycles++;

                canvas.getGraphicsContext2D().clearRect(0, 0, canvas.getWidth(), canvas.getHeight());//we must clear the canvas in the beginning such that we do not have a trail behind the particles

                for (Particles particle : particles) {
                    particle.positionXAxis += particle.speedX;//dodamo xVelocity vector na xPos of particle (kao vektori iz ALg 1)
                    particle.positionYAxis += particle.speedY; //dodamo yVelocity vector na yPos of particle
                    boundaries(particle);
                    drawParticle(particle);
                }


                int particlesPerThread = numParticles / numOfThreads;//sada dijelimo koliko cu imati particles per thread
                List<Future<?>> futures = new ArrayList<>();
                for (int i = 0; i < numOfThreads; i++) {
                    int startingIndex = i * particlesPerThread;
                    int endingIndex;
                    if (i == numOfThreads - 1) {
                        endingIndex = numParticles; //ako je posljednji onda je krajnji broj cestica ending
                    } else {
                        endingIndex = startingIndex + particlesPerThread;//ako nije onda onda pocetni plus broj cestica po tredu
                    }
                    ParticleTask task = new ParticleTask(particles, startingIndex, endingIndex);
                    futures.add(executor.submit(task));
                }
                for (Future<?> future : futures) {
                    try {
                        future.get(); // Wait for all tasks to complete before proceeding to the next cycle
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }
                }

                if (cycles >= endCycles) {

                    if (executor != null) {
                        executor.shutdown();
                        try {
                            if (!executor.awaitTermination(800, TimeUnit.MILLISECONDS)) {
                                executor.shutdownNow();
                            }
                        } catch (InterruptedException e) {
                            executor.shutdownNow();
                        }
                    }

                    this.stop();
                    long totalTimeNano = System.nanoTime() - startTime;
                    totalTime = (double)(totalTimeNano / 1000000000.0);
                    System.out.println("Runtime (in seconds): " + totalTime);
                    runTimeTotal.setText("Runtime: "+ totalTime +" sec.");
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
                    particles.add(new ParticleSimulatorParallel.Particles(positionXAxis, positionYAxis, speedX, speedY, charge, radius));
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

    private void drawParticle(Particles particle) {
        if (particle.charge == -1) {
            canvas.getGraphicsContext2D().setFill(Color.ORANGERED);
        } else {
            canvas.getGraphicsContext2D().setFill(Color.BLUEVIOLET);
        }
        canvas.getGraphicsContext2D().fillOval(particle.positionXAxis - particle.radius,particle.positionYAxis - particle.radius, 2 * particle.radius, 2 * particle.radius);
    }

    public static class ParticleTask extends Task<Void> {
        private final List<Particles> particles;
        private int startingIndex;
        private int endingIndex;

        public ParticleTask(List<Particles> particles, int startingIndex, int endingIndex) {
            this.particles = particles;
            this.startingIndex = startingIndex;
            this.endingIndex = endingIndex;
        }


        @Override
        public Void call() {
            for (int i = startingIndex; i < endingIndex; i++) {
                Particles particle1 = particles.get(i);
                for (int j = i + 1; j < particles.size(); j++) {
                    Particles particle2 = particles.get(j);
                    double distanceX = particle2.positionXAxis - particle1.positionXAxis;
                    double distanceY = particle2.positionYAxis - particle1.positionYAxis;
                    double distance = Math.sqrt(distanceX * distanceX + distanceY * distanceY);

                    double minDistance = 20;
                    distance = Math.max(distance, minDistance); //ako je distance manji od 20 onda uzimam 20 inace bi previse particles djelovalo izdedju sebe

                    double k = 10;
                    double force = -k * (particle1.charge * particle2.charge) / (distance * distance);
                    double forceX = force * distanceX;
                    double forceY = force * distanceY;
                        particle1.speedX += forceX;
                        particle1.speedY += forceY;

                        particle2.speedX -= forceX;
                        particle2.speedY -= forceY;


                }
            }
            return null;
        }
    }

    public static void main(String[] args) {
        launch();
    }
}


