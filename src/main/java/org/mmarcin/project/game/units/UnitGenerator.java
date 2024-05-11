package org.mmarcin.project.game.units;

import java.util.Random;
import java.util.Set;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@ApplicationScoped
public class UnitGenerator {

    @Inject
    UnitService unitService;
    public void generateAroundToMax(Double lat, Double lon, Integer n){
        Set<String> units = unitService.getUnitsAround(lat, lon);
        Double delta = 0.001;
        Random random = new Random();
        int i = 10;
        while(units.size() < n && i-- > 0){
            unitService.createUnit(generateRandomName(), lat + random.nextDouble(- delta, +delta), lon + random.nextDouble( - delta*2, delta*2));
            units = unitService.getUnitsAround(lat, lon);
            Log.info(units);
            Log.info(units.size() +"/" + n);
        }
    }

    public String generateRandomName(){
        String[] prefixes = {"Thomas", "Adam", "Jack", "Martin", "Joe"};
        String[] suffixes = {"Gray", "White", "Black", "Potter", "Shreder"};
        Random random = new Random();

        return prefixes[random.nextInt(prefixes.length)] + " "+ suffixes[random.nextInt(suffixes.length)];
    }
}
