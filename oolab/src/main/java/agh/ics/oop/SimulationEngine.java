package agh.ics.oop;

import agh.ics.oop.gui.MainViewApp;
import javafx.application.Platform;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;


public class SimulationEngine implements Runnable{
    SimulationVar variables;

    private Map map;
    private int howManyDays = 0;
    private MainViewApp observer;
    private boolean threadSuspended=false;


    public SimulationEngine(SimulationVar variables, MainViewApp observer){
        this.map = new Map(variables);
        this.variables = variables;
        this.observer=observer;
    }

    public void dayRitual(){
        howManyDays += 1;
        //korowód
        ArrayList<Animal> diedAnimalsToday = new ArrayList<>();

        for (Animal animal: map.getAnimalsOnField()){
            if (animal.diedDate != 0){
                diedAnimalsToday.add(animal);
            }
        }
        for (Animal animal : diedAnimalsToday){
            map.removeAnimal(animal);
        }



        //poruszamy wszystkie zwierzątka
        for (Animal animal:map.getAnimalsOnField()){
            animal.age += 1;
            variables.getMapType().moveOnMap(animal, variables, map);

        }

        ArrayList<Animal> animals = map.getAnimalsOnField();
        animals.sort(Comparator.<Animal>comparingInt(el -> el.position.x).thenComparingInt(el -> el.position.y));
        Vector2d lastVector = new Vector2d(10^9, 10^9);
        int lastIndex = 0;

        //jedzonko i rozmnażanie
        ArrayList<Animal> babies = new ArrayList<Animal>();
        for (Animal animal: map.getAnimalsOnField()){
            if (!animal.getPosition().equals(lastVector)){ //jeśli wektor zwierzątka nie jest identyczny do ostatniego -> aby nie sprawdzac kilka razy tej samej pozycji zwierząt
                lastVector = animal.getPosition();
                if (map.howManyAnimalsOnSpot(animal.getPosition())!= 1){ //jeśli jest wiecej niż 1 zwierze na danym polu

                    int howManyOnThisSpot = map.howManyAnimalsOnSpot(animal.getPosition());

                    ArrayList<Animal> possibleMatch = new ArrayList<>();
                    for(int i = 0; i < howManyOnThisSpot; i++){
                        possibleMatch.add(animals.get(i + lastIndex));
                    }
                    possibleMatch.sort(Comparator.<Animal>comparingInt(el -> -el.energy).thenComparingInt(el -> -el.age).thenComparingInt(el -> -el.children));
                    lastIndex = howManyOnThisSpot;
                    Animal winner = possibleMatch.get(0); //wygrał trawkę

                    if (map.isGrassThere(winner.position)) {
                        winner.energy += variables.getGrassEnergyProfit();
                        winner.grassEaten += 1;
                        map.removeGrass(new Grass(winner.position));
                    }

                    for (int i = 1; i < possibleMatch.size(); i+=2){
                            Animal first = possibleMatch.get(i-1);
                            Animal second = possibleMatch.get(i);
                            if (first.energy >= variables.getMinEnergyForCopulation() && second.energy >= variables.getMinEnergyForCopulation()) {
                                Animal baby = new Animal(variables, first, second); //nowe bobo
                                babies.add(baby);
                            }
                        }
                    }

                else{
                    if (map.isGrassThere(animal.position)) {
                        animal.energy += variables.getGrassEnergyProfit();
                        animal.grassEaten += 1;
                        map.removeGrass(new Grass(animal.position));
                    }

                }

            }

            }
        for (Animal baby: babies){
            map.placeAnimalOnMap(baby); //umieszczamy na mapie
        }





        //zasianie roślin
        variables.getGardener().seedGrass(variables, map);
        //update informacji o maksymalnej enrgii
        map.updateMaxEnergy();
        observer.newDayUpdate();
    }

    protected int getDays(){
        return howManyDays;
    }
    public void pause(){
        threadSuspended=true;
    }
    public void play(){
        if (threadSuspended) {
            threadSuspended = false;
            synchronized(this) {
                notify();
            }
        }
    }
    public void stop(){}

    @Override
    public void run() {
//        Object[] statistics = new Object[]{
//                map.getStats().getAmountOfAnimals(),
//                map.getStats().getAmountOfGrass(),
//                map.getStats().freeSpots(),
//                map.getStats().theMostCommonGenotype(),
//                map.getStats().averageEnergyAlive(),
//                map.getStats().averageAgeDead()
//
//        };

        while(map.getStats().getAmountOfAnimals()>0){
            Platform.runLater(()->{dayRitual();});
            Platform.runLater(()->{observer.newDayUpdate();});
            try {
                Thread.sleep(variables.getRefreshTime());
                synchronized(this) {
                    while (threadSuspended)
                        wait();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public Statistics getMapStats(){return map.getStats();}
    public int getMapHeight(){return map.getHeight();}
    public int getMapWidth(){return map.getWidth();}

    public Map getMap() {
        return map;
    }
}
