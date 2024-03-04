package org.mmarcin.project.location;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.Map;
import java.util.Random;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import de.westnordost.osmapi.ApiResponseReader;
import de.westnordost.osmapi.OsmConnection;
import de.westnordost.osmapi.map.data.BoundingBox;
import de.westnordost.osmapi.map.data.Node;
import de.westnordost.osmapi.map.data.Relation;
import de.westnordost.osmapi.map.data.Way;
import de.westnordost.osmapi.map.handler.MapDataHandler;
import de.westnordost.osmapi.overpass.OverpassMapDataApi;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@ApplicationScoped
public class LocationService {
    private static final double EARTH_RADIUS = 6371000;

    public String location(Double originLat, Double originLon) {

        Double latitude = originLat;
        Double longitude = originLon;
        float radius = 200;
        String query = "[out:json];("
                + "way(around:" + radius + "," + latitude + "," + longitude + ")[\"man_made\"];"
                + "node(around:" + radius + "," + latitude + "," + longitude + ")[\"man_made\"];"
                + "relation(around:" + radius + "," + latitude + "," + longitude + ")[\"man_made\"];"
                + "way(around:" + radius + "," + latitude + "," + longitude + ")[\"natural\"];"
                + "node(around:" + radius + "," + latitude + "," + longitude + ")[\"natural\"];"
                + "relation(around:" + radius + "," + latitude + "," + longitude + ")[\"natural\"];"
                + "way(around:" + radius + "," + latitude + "," + longitude + ")[\"landuse\"];"
                + "node(around:" + radius + "," + latitude + "," + longitude + ")[\"landuse\"];"
                + "relation(around:" + radius + "," + latitude + "," + longitude + ")[\"landuse\"];"
                + "way(around:" + radius + "," + latitude + "," + longitude + ")[\"building\"];"
                + "node(around:" + radius + "," + latitude + "," + longitude + ")[\"building\"];"
                + "relation(around:" + radius + "," + latitude + "," + longitude + ")[\"building\"];"
                + ");\r\n" + //
                "convert item ::=::,::geom=geom(),_osm_type=type();\r\n" + //
                "out geom;\r\n" + //
                ">;\r\n" + //
                "out skel qt;";

        try {

            String encodedQuery = URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8);
            HttpClient httpClient = HttpClients.createDefault();
            HttpGet httpGet = new HttpGet("https://overpass-api.de/api/interpreter?data=" + encodedQuery);
            HttpResponse response = httpClient.execute(httpGet);
            if (response.getStatusLine().getStatusCode() == 200) {
                String jsonResponse = EntityUtils.toString(response.getEntity());
                AreaCalculator areaCalculator = new AreaCalculator();
                try {
                    return areaCalculator.calculateAreaPercentages(jsonResponse,
                            radius).toString();
                } catch (Exception e) {
                    return e.getMessage();
                }

            } else {

                return "Błąd podczas pobierania danych z Overpass API: " + response.getStatusLine().getStatusCode();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "server error";
    }

}
