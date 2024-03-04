package org.mmarcin.project.location;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jboss.logging.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AreaCalculator {

    public AreaCalculator() {
    }

    public Map<String, Double> calculateAreaPercentages(String json, double radius) throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readValue(json, ObjectNode.class);
        JsonNode elements = rootNode.get("elements");

        Map<String, Double> areaPercentages = new HashMap<>();

        // LOG.info(rootNode.toString());

        for (JsonNode element : rootNode.get("elements")) {
            JsonNode node;
            String type = "";
            node = element.get("tags").get("landuse");
            if (node != null)
                type = node.asText();
            node = element.get("tags").get("natural");
            if (node != null && type == "")
                type = node.asText();

            JsonNode geometry = element.get("geometry");

            double area = calculateArea(geometry, radius);
            if (type == "")
                continue;
            areaPercentages.put(type, areaPercentages.getOrDefault(type, 0.0) + area);
        }

        // Oblicz procentowy udział
        double totalArea = 0.0;
        for (Map.Entry<String, Double> entry : areaPercentages.entrySet()) {
            totalArea += entry.getValue();
        }
        for (Map.Entry<String, Double> entry : areaPercentages.entrySet()) {
            entry.setValue((entry.getValue() / totalArea) * 100);
        }
        LOG.info(areaPercentages.toString());
        return areaPercentages;
    }

    private static final Logger LOG = Logger.getLogger(AreaCalculator.class);

    private static double calculateArea(JsonNode geometry, double radius) {
        double area = 0.0;

        // Poprzednia wersja kodu błędnie obliczała obwód.
        // Poniższy kod poprawnie oblicza powierzchnię.
        JsonNode coords = geometry.get("coordinates");
        if (coords == null) {
            if (geometry.get("type").asText().equals("GeometryCollection")) {
                for (JsonNode geo : geometry.get("geometries")) {
                    area += calculateArea(geo, radius);
                }
                return area;
            } else
                return 0;
        }

        if (coords.size() <= 2)
            return 0;

        for (int i = 0; i < coords.size(); i++) {
            double latp, lonp;
            double lat1 = coords.get(i).get(0).asDouble();
            double lon1 = coords.get(i).get(1).asDouble();
            double lat2 = coords.get((i + 1) % coords.size()).get(0).asDouble();
            double lon2 = coords.get((i + 1) % coords.size()).get(1).asDouble();
            if (i != 0) {
                latp = coords.get((i - 1)).get(0).asDouble();
                lonp = coords.get((i - 1)).get(1).asDouble();
            } else {
                latp = coords.get(coords.size() - 1).get(0).asDouble();
                lonp = coords.get(coords.size() - 1).get(1).asDouble();
            }

            Double newArea = lon1 * (latp - lat2);
            // Double newArea = lat1 * (lonp - lon2);
            if (newArea != null)
                area += newArea;
        }

        return Math.abs(area) / 2.0;
    }

    private static double distance(double lat1, double lon1, double lat2, double lon2) {
        double theta = lon1 - lon2;
        double dist = Math.sin(deg2rad(lat1)) * Math.sin(deg2rad(lat2))
                + Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) * Math.cos(deg2rad(theta));
        dist = Math.acos(dist);
        dist = rad2deg(dist) * 60 * 1.1515;
        dist = dist * 1.609344;
        return dist;
    }

    private static double deg2rad(double deg) {
        return deg * (Math.PI / 180);
    }

    private static double rad2deg(double rad) {
        return rad * (180 / Math.PI);
    }

    public static double[] calculateRange(double lat, double lon) {

        final double EARTH_RADIUS = 6371000;

        double halfSideLength = 100 / Math.sqrt(2);

        double northLat = lat + halfSideLength / EARTH_RADIUS;
        double southLat = lat - halfSideLength / EARTH_RADIUS;
        double eastLon = lon + halfSideLength / (EARTH_RADIUS * Math.cos(lat * Math.PI / 180));
        double westLon = lon - halfSideLength / (EARTH_RADIUS * Math.cos(lat * Math.PI / 180));

        return new double[] { northLat, southLat, eastLon, westLon };
    }
}