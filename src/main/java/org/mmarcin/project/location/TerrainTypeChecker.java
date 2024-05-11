package org.mmarcin.project.location;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.SimpleSelector;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.shared.uuid.UUIDFactory;
import org.mmarcin.project.database.DatasetManager;
import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.Entity;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Relation;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.domain.v0_6.WayNode;
import org.openstreetmap.osmosis.core.task.v0_6.RunnableSource;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;
import org.openstreetmap.osmosis.osmbinary.file.BlockInputStream;
import org.openstreetmap.osmosis.xml.common.CompressionMethod;
import org.openstreetmap.osmosis.xml.v0_6.XmlReader;

import com.fasterxml.jackson.databind.ObjectMapper;

import crosby.binary.osmosis.OsmosisReader;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

public class TerrainTypeChecker {

    private static final String POLAND_PBF_FILE = "pomorskie-latest.osm.pbf";
    @Inject
    private static DatasetManager datasetManager;

    public static String getCache() {
        if (datasetManager == null)
            datasetManager = new DatasetManager();
        Set<String> s = datasetManager.getSubjectsBySelector(new SimpleSelector() {
            public boolean selects(Statement a) {
                return a.getSubject().toString().startsWith("terrain/cache");
            }
        });
        String output = "";
        int i = 10;
        for(String str : s){
            output += datasetManager.getStatement(datasetManager.getResource(str), "type ");
            i--;
            if(i==0)return output;
        }
        return output;
    }

    public static String getTerrainType(double latitude, double longitude) {
        try {
            FileInputStream fileInputStream = new FileInputStream(POLAND_PBF_FILE);
            OsmosisReader osmosisReader = new OsmosisReader(fileInputStream);
            osmosisReader.setSink(new TerrainTypeSink(latitude, longitude));

            Log.info("Reading starts");
            if (datasetManager == null)
            datasetManager = new DatasetManager();  
            datasetManager.startWrite();
            osmosisReader.run();
            datasetManager.commit();
            datasetManager.end();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        return "ok";
    }

    static class TerrainTypeSink implements Sink {
        private double latitude;
        private double longitude;

        @Inject
        DatasetManager datasetManager;

        TerrainTypeSink(double latitude, double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
            datasetManager = new DatasetManager();
        }

        @Override
        public void process(EntityContainer entityContainer) {
            Entity entity = entityContainer.getEntity();
            if (entity instanceof Way) {
                Way way = (Way) entity;
                List<WayNode> wayNodes = way.getWayNodes();

                String tagValue = "";
                for (Tag tag : way.getTags()) {
                    if (tag.getKey().equals("landuse"))
                        tagValue = tag.getValue();
                    if (tag.getKey().equals("natural"))
                        tagValue = tag.getValue();
                }
                if (tagValue == "") {
                    Log.info("no terrain type data");
                    return;
                }
                // for (WayNode wayNode : wayNodes) {
                // }
                MinMaxes minMaxes = getMinMaxPositions(wayNodes);
                List<Pair<Double, Double>> nodes = wayNodes.stream()
                        .map(wn -> Pair.of(wn.getLatitude(), wn.getLongitude())).collect(Collectors.toList());
                final ObjectMapper objectMapper = new ObjectMapper();
                final StringWriter stringWriter = new StringWriter();
                String nodesAsJson = "";
                try {
                    objectMapper.writeValue(stringWriter, nodes);
                    nodesAsJson = stringWriter.toString();
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.warn(nodesAsJson);
                }
                final String uri = "terrain/cache/";

                String resourceName = uri + UUID.randomUUID();
                datasetManager.createPropertyBulk(resourceName, "type", tagValue);
                datasetManager.createLiteralBulk(resourceName, "minLat", minMaxes.minLat);
                datasetManager.createLiteralBulk(resourceName, "maxLat", minMaxes.maxLat);
                datasetManager.createLiteralBulk(resourceName, "minLong", minMaxes.minLong);
                datasetManager.createLiteralBulk(resourceName, "maxLong", minMaxes.maxLong);
                datasetManager.createPropertyBulk(resourceName, "nodes", nodesAsJson);

            } else if (entity instanceof Node) {
                // Node node = (Node) entity;
                // processNode(node.getId());
            } else if (entity instanceof Relation) {
                // Możesz obsłużyć relacje, jeśli mają znaczenie dla typu terenu
            }
        }

        private class MinMaxes {
            public Double maxLat;
            public Double minLat;
            public Double maxLong;
            public Double minLong;

            public MinMaxes(Double maxLat, Double minLat, Double maxLong, Double minLong) {
                this.maxLat = maxLat;
                this.minLat = minLat;
                this.maxLong = maxLong;
                this.minLong = minLong;
            }
        }

        private MinMaxes getMinMaxPositions(List<WayNode> nodes) {
            // Tutaj możesz przetworzyć węzeł (node) z pliku PBF i sprawdzić, czy znajduje
            // się w pobliżu
            // danej współrzędnej geograficznej (latitude, longitude)
            Double maxLat, minLat, maxLong, minLong;
            maxLat = minLat = nodes.get(0).getLatitude();
            maxLong = minLong = nodes.get(0).getLongitude();
            for (WayNode wn : nodes) {
                Double lat = wn.getLatitude();
                Double lon = wn.getLongitude();
                maxLat = Math.max(maxLat, lat);
                minLat = Math.min(minLat, lat);
                maxLong = Math.max(maxLong, lon);
                minLong = Math.min(minLong, lon);
            }

            return new MinMaxes(maxLat, minLat, maxLong, minLong);
        }

        private boolean isWithinRange(double lat1, double lon1, double lat2, double lon2, double range) {
            // Funkcja pomocnicza sprawdzająca, czy dana współrzędna (lat2, lon2) znajduje
            // się w odległości 'range'
            // od współrzędnych (lat1, lon1)
            double earthRadius = 6371e3; // Promień Ziemi w metrach
            double dLat = Math.toRadians(lat2 - lat1);
            double dLon = Math.toRadians(lon2 - lon1);
            double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                    Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                            Math.sin(dLon / 2) * Math.sin(dLon / 2);
            double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
            double distance = earthRadius * c;
            return distance <= range;
        }

        @Override
        public void initialize(Map<String, Object> metaData) {
        }

        @Override
        public void complete() {

        }

        @Override
        public void close() {

        }

    }
}
