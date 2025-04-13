package org.mmarcin.project.location;

import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import java.util.stream.Collectors;

import javax.measure.Unit;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.jena.rdf.model.SimpleSelector;
import org.apache.jena.rdf.model.Statement;
import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.referencing.CRS;
import org.mmarcin.project.database.DatasetManager;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wolt.osm.parallelpbf.ParallelBinaryParser;
import com.wolt.osm.parallelpbf.entity.*;
import io.quarkus.logging.Log;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class TerrainTypeChecker {

    private final String POLAND_PBF_FILE = "polska.tif";
    @Inject
    private DatasetManager datasetManager;

    public String getCache() {
        if (datasetManager == null)
            datasetManager = new DatasetManager();
        Set<String> s = datasetManager.getSubjectsBySelector(new SimpleSelector() {
            public boolean selects(Statement a) {
                return a.getSubject().toString().startsWith("terrain/cache");
            }
        });
        String output = "";
        int i = 10;
        for (String str : s) {
            output += datasetManager.getStatement(datasetManager.getResource(str), "type ");
            i--;
            if (i == 0)
                return output;
        }
        return output;
    }

    public String getInfo(double latitude, double longitude) {
        try {

            File file = new File(POLAND_PBF_FILE);
            GeoTiffReader reader = new GeoTiffReader(file);
            GridCoverage2D coverage = reader.read(null);
            CoordinateReferenceSystem wgs84 = CRS.decode("EPSG:4326");
            CoordinateReferenceSystem laea = CRS.decode("EPSG:3035");
            MathTransform transform = CRS.findMathTransform(wgs84, laea, true);
            // Zdefiniuj współrzędne geograficzne punktu
            GridGeometry2D gridGeometry = coverage.getGridGeometry();
            double pos[] = { longitude, latitude };
            DirectPosition2D wgsPos = new DirectPosition2D(wgs84, latitude, longitude);
            DirectPosition2D laeaPos = new DirectPosition2D();
            transform.transform(wgsPos, laeaPos);
            // Przekształć współrzędne geograficzne na współrzędne piksela
            GridCoordinates2D gridCoords = gridGeometry.worldToGrid(laeaPos);
            Envelope envelope = gridGeometry.getEnvelope();
            

            // Wyświetl współrzędne zasięgu
            System.out.println("Minimalna długość geograficzna: " + envelope.getMinimum(0));
            System.out.println("Maksymalna długość geograficzna: " + envelope.getMaximum(0));
            System.out.println("Minimalna szerokość geograficzna: " + envelope.getMinimum(1));
            System.out.println("Maksymalna szerokość geograficzna: " + envelope.getMaximum(1));
            // Pobierz wartość piksela
            double[] pixel = new double[1];
            // RenderedImage renderedImage = coverage.getRenderedImage();
            coverage.evaluate((DirectPosition) wgsPos, pixel);
            // renderedImage.getData().getPixel((int) gridCoords.x, (int) gridCoords.y,
            // pixel);
            int pixelValue = (int) pixel[0];
            return Integer.toString(pixelValue);
        } catch (Exception e) {
            Log.warn(e.getClass() + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }


    public String generateCache() {
        try {
            InputStream input = new FileInputStream(POLAND_PBF_FILE);

            Log.info("Reading starts");
            TerrainTypeSink terrainTypeSink = new TerrainTypeSink();
            Log.info("Pre read " + input.toString());

            // datasetManager.startWrite();
            new ParallelBinaryParser(input, 32)
                    .onHeader((hd) -> {
                    })
                    .onNode(terrainTypeSink.processNodes)
                    .onWay(terrainTypeSink::processWays)
                    .onComplete(() -> {
                        // datasetManager.end();
                        Log.info("Post read");
                        // datasetManager.commit();
                    })
                    .parse();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return "ok";
    }

    public class TerrainTypeSink {
        private double latitude;
        private double longitude;
        private Map<Long, Node> nodes;
        private final AtomicLong wayCounter = new AtomicLong();
        @Inject
        DatasetManager datasetManager;

        public TerrainTypeSink() {
            if (datasetManager == null)
                datasetManager = new DatasetManager();
            nodes = new HashMap<Long, Node>();
        }

        public Consumer<Node> processNodes = (node) -> {
            if (nodes.size() % 10000 == 0)
                Log.info("Reading node " + nodes.size());
            nodes.put(node.getId(), node);
        };

        public void processWays(Way way) {
            wayCounter.incrementAndGet();
            if (wayCounter.get() % 10000 == 0)
                Log.info("Reading way" + wayCounter.get());

            List<Long> wayNodes = way.getNodes();

            String tagValue = "";
            for (Map.Entry<String, String> tag : way.getTags().entrySet()) {
                if (tag.getKey().equals("landuse"))
                    tagValue = tag.getValue();
                if (tag.getKey().equals("natural"))
                    tagValue = tag.getValue();
            }
            if (tagValue == "") {
                // Log.info("no terrain type data");
                return;
            }
            // for (WayNode wayNode : wayNodes) {
            // }
            MinMaxes minMaxes = getMinMaxPositions(wayNodes);
            if (minMaxes == null)
                return;
            List<Pair<Double, Double>> nodesLatLon = wayNodes.stream()
                    .map(wn -> Pair.of(nodes.get(wn).getLat(), nodes.get(wn).getLon())).collect(Collectors.toList());
            final ObjectMapper objectMapper = new ObjectMapper();
            final StringWriter stringWriter = new StringWriter();
            String nodesAsJson = "";
            try {
                objectMapper.writeValue(stringWriter, nodesLatLon);
                nodesAsJson = stringWriter.toString();
            } catch (Exception e) {
                e.printStackTrace();
                Log.warn(nodesAsJson);
            }
            final String uri = "terrain/cache/";

            String resourceName = uri + UUID.randomUUID();
            datasetManager.startWrite();
            datasetManager.createPropertyBulk(resourceName, "type", tagValue);
            datasetManager.createLiteralBulk(resourceName, "minLat", minMaxes.minLat);
            datasetManager.createLiteralBulk(resourceName, "maxLat", minMaxes.maxLat);
            datasetManager.createLiteralBulk(resourceName, "minLong", minMaxes.minLong);
            datasetManager.createLiteralBulk(resourceName, "maxLong", minMaxes.maxLong);
            datasetManager.createPropertyBulk(resourceName, "nodes", nodesAsJson);
            datasetManager.commit();
            datasetManager.end();
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

        private MinMaxes getMinMaxPositions(List<Long> wayNodes) {
            // Tutaj możesz przetworzyć węzeł (node) z pliku PBF i sprawdzić, czy znajduje
            // się w pobliżu
            // danej współrzędnej geograficznej (latitude, longitude)
            try {

                Double maxLat, minLat, maxLong, minLong;
                Node first = nodes.get(wayNodes.get(0));
                maxLat = minLat = first.getLat();
                maxLong = minLong = first.getLon();
                for (Long n : wayNodes) {
                    Node wn = nodes.get(n);
                    Double lat = wn.getLat();
                    Double lon = wn.getLon();
                    maxLat = Math.max(maxLat, lat);
                    minLat = Math.min(minLat, lat);
                    maxLong = Math.max(maxLong, lon);
                    minLong = Math.min(minLong, lon);
                }

                return new MinMaxes(maxLat, minLat, maxLong, minLong);
            } catch (Exception e) {
                return null;
            }
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

    }
}
