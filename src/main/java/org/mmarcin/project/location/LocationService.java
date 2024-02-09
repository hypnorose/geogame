package org.mmarcin.project.location;

import java.io.IOException;
import java.net.URLEncoder;

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

    public String location(float latitude, float longitude) {
        double radius = 100;
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
        // OsmConnection connection = new
        // OsmConnection("https://overpass-api.de/api/","osmapi unit test",null);
        // OverpassMapDataApi overpass = new OverpassMapDataApi(connection);

        // LocationHandler handler = new LocationHandler();
        // //overpass.queryElements(query,handler);
        // overpass.queryElements(
        // "[bbox:13.8,35.5,14.9,36.3]; nwr[shop]; out meta;",
        // handler
        // );
        try {
            // Tworzenie zapytania Overpass API w formacie Overpass QL

            // Kodowanie zapytania do formatu URL
            String encodedQuery = URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8);

            // Wywołanie zapytania Overpass API
            HttpClient httpClient = HttpClients.createDefault();
            HttpGet httpGet = new HttpGet("https://overpass-api.de/api/interpreter?data=" + encodedQuery);

            // Przetwarzanie odpowiedzi
            HttpResponse response = httpClient.execute(httpGet);
            if (response.getStatusLine().getStatusCode() == 200) {
                String output = "";
                String jsonResponse = EntityUtils.toString(response.getEntity());
                ObjectMapper mapper = new ObjectMapper();
                JsonNode node = mapper.readValue(jsonResponse, ObjectNode.class);
                for (JsonNode elementNode : node.get("elements")) {
                    JsonNode landuseNode = elementNode.get("tags").get("landuse");
                    JsonNode naturalNode = elementNode.get("tags").get("natural");
                    JsonNode manmadeNode = elementNode.get("tags").get("man_made");
                    JsonNode buildingNode = elementNode.get("tags").get("building");
                    String word = "";
                    String landuse = landuseNode != null ? landuseNode.asText() : "";
                    String natural = naturalNode != null ? naturalNode.asText() : "";
                    String man_made = manmadeNode != null ? manmadeNode.asText() : "";
                    String building = buildingNode != null ? buildingNode.asText() : "";
                    if (!(landuse == "" && natural == "")) {
                        output += landuse + "" + natural + "\n";
                    }
                }
                return output;
            } else {
                System.out.println(
                        "Błąd podczas pobierania danych z Overpass API: " + response.getStatusLine().getStatusCode());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "nothing";
    }
}
