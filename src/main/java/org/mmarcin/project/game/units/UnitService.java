package org.mmarcin.project.game.units;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryException;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ModelFactoryBase;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.SimpleSelector;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.VCARD;
import org.jboss.logging.Logger;
import org.mmarcin.project.database.DatasetManager;
import org.mmarcin.project.location.AreaCalculator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonAppend.Prop;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class UnitService {
    private static final Logger LOG = Logger.getLogger(UnitService.class);
    private static final int MAX_UNITS_AROUND_PLAYER = 4;
    @Inject
    DatasetManager datasetManager;
    @Inject
    UnitGenerator unitGenerator;

    public String getUnits(double latitude, double longitude) {
        String output = "";
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode rootNode = mapper.createArrayNode();
        Set<String> unitsAround = getUnitsAround(latitude, longitude);
        Log.info(unitsAround);
        try {

            for (String unit : unitsAround) {

                ObjectNode childNode = mapper.createObjectNode();
                Log.info(unit);
                Resource object = datasetManager.getResource(unit);
                String name = datasetManager.getStatement(object, "FN").getString();
                Double valueLat = datasetManager.getStatement(object, "atLat").getDouble();
                Double valueLong = datasetManager.getStatement(object, "atLong").getDouble();
                String expireDate = datasetManager.getStatement(object, "expires").getObject().toString();
                childNode.put("lat", valueLat.toString());
                childNode.put("long", valueLong.toString());
                childNode.put("expires", expireDate.toString());
                childNode.put("name", name);
                rootNode.add(childNode);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
        }
        try {
            output = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(rootNode);
        } catch (Exception e) {
        }
        // LOG.info(output);

        return output;

    }

    public String createUnit(String name, double lat, double lon) {
        LOG.info("unit created " + name + " " + lat + " " + lon);
        String prefix = "units/instance/";
        String originURI = prefix + name + UUID.randomUUID();
        String warriorName = name;
        datasetManager.createProperty(originURI, "FN", warriorName);

        datasetManager.createLiteral(originURI, "atLat", lat);
        datasetManager.createLiteral(originURI, "atLong", lon);

        LocalDateTime expireLocalDateTime = LocalDateTime.now().plusMinutes(5);
        datasetManager.createLiteral(originURI, "expires", expireLocalDateTime.toString());
        return "ok";
    }

    public Set<String> getUnitsAround(double latitude, double longitude) {
        final int UNITS_AROUND_PLAYER = 3;
        double minLat, maxLat, minLong, maxLong;
        double delta = 0.001;
        minLat = latitude - delta;
        maxLat = latitude + delta;
        minLong = longitude - delta * 2;
        maxLong = longitude + delta * 2;

        Set<String> origins = datasetManager
                .getSubjectsBySelector(new SimpleSelector(null, datasetManager.getProperty("FN"), (RDFNode) null) {
                    public boolean selects(Statement s) {
                        double lat = s.getSubject().getProperty(datasetManager.getProperty("atLat")).getDouble();
                        double lon = s.getSubject().getProperty(datasetManager.getProperty("atLong")).getDouble();
                        return lat > minLat && lat < maxLat && lon > minLong && lon < maxLong &&
                                s.getSubject().toString().startsWith("units/instance/");
                    }
                });
        return origins;
    }

    public Boolean generateUnits(double latitude, double longitude) {

        unitGenerator.generateAroundToMax(latitude, longitude, MAX_UNITS_AROUND_PLAYER);
        return true;
    }

    public void removeExpiredUnits() {

        datasetManager.deleteBySelector(new SimpleSelector(null, null, (RDFNode) null) {
            public boolean selects(Statement s) {

                Property expires = datasetManager.getProperty("expires");
                Boolean expired = LocalDateTime.parse(s.getSubject().getProperty(expires).toString())
                        .isAfter(LocalDateTime.now());

                return s.getSubject().toString().startsWith("units/instance/") && !expired;
            }
        }
            
        );

    }

    public String deleteAll() {
        datasetManager.startWrite();
        datasetManager.getModel().removeAll();
        datasetManager.commit();
        datasetManager.end();
        return "deleted";
    }
}
