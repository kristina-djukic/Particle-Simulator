package com.example.kristinaparticle;

import mpi.*;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


class ParticlesSimulatorDistributive {

    private static class Particles implements Serializable { //da bih mogla da broadcastujem mora se pretvoriti u bajt seq a to radi serializable
        double positionXAxis, positionYAxis;
        double SpeedX, SpeedY;
        double charge;
        double radius;

        public Particles(double PositionXAxis, double PositionYAxis, double speedX, double speedY, double charge, double radius) {
            this.positionXAxis = PositionXAxis;
            this.positionYAxis = PositionYAxis;
            this.SpeedX = speedX;
            this.SpeedY = speedY;
            this.charge = charge;
            this.radius = radius;
        }
    }

    public static void main(String[] args) {
        MPI.Init(args); //pripremam kod za koristenje MPI (kao start)

        int processNum = MPI.COMM_WORLD.Rank();
        int totalNumOfProc = MPI.COMM_WORLD.Size();

        int numOfParticles = 3000;
        int totalCycles = 1000;
        List<Particles> particlesArrL = new ArrayList<>();

        Particles[] particlesArray = new Particles[numOfParticles];//initializujem na svim procesima

        if(processNum == 0) { // pravim main proces gdje sve funk norm dobijaju se radnom pozicije i brzine
            for (int i = 0; i < numOfParticles; i++) {
                double x = Math.random() * 800;   //ovdje moram assumovati da je window zadane velicine jer kako drugacije
                double y = Math.random() * 600;  //isto
                double speedX = Math.random() * 10 - 5;
                double speedY = Math.random() * 10 - 5;
                double charge = (Math.random() > 0.5) ? 1 : -1;
                double radius = 6;
                particlesArrL.add(new Particles(x, y, speedX, speedY, charge, radius));
            }
            particlesArray = particlesArrL.toArray(new Particles[0]);//broadcast radi sa arrayom
        }

        MPI.COMM_WORLD.Bcast(particlesArray, 0, particlesArray.length, MPI.OBJECT, 0);//pocni slati od nula indexa, koji array saljem, length, objekat, i broj main procesa je 0
        particlesArrL = Arrays.asList(particlesArray);//vracam nazad u list array


        int particlesPerProcess = numOfParticles / totalNumOfProc;
        int indexOfParticle = processNum * particlesPerProcess;//isto kao u paralelnom racunam
        int endingIndexOfParticle;
        if (processNum == totalNumOfProc - 1){//isto i ovo vazi
            endingIndexOfParticle = numOfParticles;
        } else{
            endingIndexOfParticle = indexOfParticle + particlesPerProcess;
        }

        long startTime = System.nanoTime();
        ParticleProcess task = new ParticleProcess(particlesArrL, indexOfParticle, endingIndexOfParticle);
        for (int cycle = 0; cycle < totalCycles; cycle++) {//za svaki cycle
            task.run();

            Particles[] updatedParticles = new Particles[numOfParticles];//ovdje drzim updated particle info
            MPI.COMM_WORLD.Gather(particlesArrL.toArray(new Particles[0]), 0, particlesPerProcess, MPI.OBJECT, updatedParticles, 0, particlesPerProcess, MPI.OBJECT, 0);//skupljam sa ostalih procesa na main proces apdejtovane cestice
            particlesArrL = Arrays.asList(updatedParticles);

            MPI.COMM_WORLD.Bcast(updatedParticles, 0, updatedParticles.length, MPI.OBJECT, 0);//sad apdjetovane opet broadcastujem
        }

        long runTime = System.nanoTime() - startTime;

        if(processNum == 0) {
            double runTimeNano = (double)(runTime / 1000000000.0);
            System.out.println("Runtime (in seconds): " + runTimeNano);
            try (PrintWriter out = new PrintWriter(new FileOutputStream("simulation_results.txt", true))) {
                out.println("Runtime (in seconds): " + runTimeNano);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }


        MPI.Finalize();
    }

    private static class ParticleProcess {
        private final List<Particles> particles;
        private final int indexOfParticle;
        private final int endingIndexOfParticle;

        public ParticleProcess(List<Particles> particles, int indexOfParticle, int endingIndexOfParticle) {
            this.particles = particles;
            this.indexOfParticle = indexOfParticle;
            this.endingIndexOfParticle = endingIndexOfParticle;
        }

        public void run() {
            for (int i = indexOfParticle; i < endingIndexOfParticle; i++) {
                Particles particle1 = particles.get(i);
                for (int j = 0; j < particles.size(); j++) {
                    if (i == j) continue;
                    Particles particle2 = particles.get(j);
                    double distanceX = particle2.positionXAxis - particle1.positionXAxis;
                    double distanceY = particle2.positionYAxis - particle1.positionYAxis;
                    double distance = Math.sqrt(distanceX * distanceX + distanceY * distanceY);

                    double minDistance = 20;
                    distance = Math.max(distance, minDistance);

                    double k = 10;
                    double force = -k * (particle1.charge * particle2.charge) / (distance * distance);
                    double forceX = force * distanceX / distance;
                    double forceY = force * distanceY / distance;
                    particle1.SpeedX += forceX;
                    particle1.SpeedY += forceY;
                    particle2.SpeedX -= forceX;
                    particle2.SpeedY -= forceY;
                }
                particle1.positionXAxis += particle1.SpeedX;
                particle1.positionYAxis += particle1.SpeedY;
                boundaries(particle1);
            }
        }

        private void boundaries(Particles particle) {
            if (particle.positionXAxis < particle.radius) {
                particle.positionXAxis = particle.radius;
                particle.SpeedX = -particle.SpeedX;
            } else if (particle.positionXAxis > 800 - particle.radius) {
                particle.positionXAxis = 800 - particle.radius;
                particle.SpeedX = -particle.SpeedX;
            }
            if (particle.positionYAxis < particle.radius) {
                particle.positionYAxis = particle.radius;
                particle.SpeedY = -particle.SpeedY;
            } else if (particle.positionYAxis > 600 - particle.radius) {
                particle.positionYAxis = 600 - particle.radius;
                particle.SpeedY = -particle.SpeedY;
            }
        }
    }
}


