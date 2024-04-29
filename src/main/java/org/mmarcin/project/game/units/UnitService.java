package org.mmarcin.project.game.units;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

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
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class UnitService {
    private static final Logger LOG = Logger.getLogger(UnitService.class);

    @Inject
    DatasetManager datasetManager;

    public String getUnits(double latitude, double longitude) {
        String output = "";
        LOG.info(output);

        ObjectMapper mapper = new ObjectMapper();

        ObjectNode rootNode = mapper.createObjectNode();
        Set<String> unitsAround = getUnitsAround(latitude, longitude);
        datasetManager.startRead();
        Model model = datasetManager.getModel();

        try {

            for (String unit : unitsAround) {

                ObjectNode childNode = mapper.createObjectNode();

                Resource object = model.getResource(unit);
                String name = object.getProperty(VCARD.FN).getString();
                Property atLat = model.createProperty("atLat");
                Property atLon = model.createProperty("atLong");
                Property expires = model.createProperty("expires");

                Double valueLat = object.getProperty(atLat).getDouble();
                Double valueLong = object.getProperty(atLon).getDouble();
                String expireDate = object.getProperty(expires).toString();
                childNode.put("lat", valueLat.toString());
                childNode.put("long", valueLong.toString());
                childNode.put("expires", expireDate.toString());
                rootNode.set(name, childNode);
            }
        } catch (Exception e) {
        } finally {
            datasetManager.end();
        }
        try {
            output = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(rootNode);
        } catch (Exception e) {
        }
        // LOG.info(output);

        return output;

    }

    public String createUnit(String name, double lat, double lon) {
        LOG.info(name);
        datasetManager.startWrite();
        Model model = datasetManager.getModel();
        String prefix = "http://units/instance/";
        String originURI = prefix + name;
        String warriorName = name;

        Resource unit = model.createResource(originURI);
        unit.addProperty(VCARD.FN, warriorName);
        Property locationLat = model.createProperty("atLat");
        Property locationLong = model.createProperty("atLong");
        Property expires = model.createProperty("expires");
        LocalDateTime expireLocalDateTime = LocalDateTime.now().plusMinutes(5);

        unit.addLiteral(locationLat, ResourceFactory.createTypedLiteral(lat));
        unit.addLiteral(locationLong, ResourceFactory.createTypedLiteral(lon));
        unit.addLiteral(expires, ResourceFactory.createTypedLiteral(expireLocalDateTime.toString()));

        datasetManager.commit();
        datasetManager.end();
        return "ok";
    }

    public Set<String> getUnitsAround(double latitude, double longitude) {
        datasetManager.startRead();
        final int UNITS_AROUND_PLAYER = 3;
        double minLat, maxLat, minLong, maxLong;
        double delta = 0.004;
        minLat = 53.54107409151398 - delta;
        maxLat = 53.54107409151398 + delta;
        minLong = 19.395292892120136 - delta * 2;
        maxLong = 19.395292892120136 + delta * 2;

        Model model = datasetManager.getModel();
        Property locationLat = model.createProperty("atLat");
        Property locationLong = model.createProperty("atLong");
        StmtIterator iter = model.listStatements(new SimpleSelector(null, VCARD.FN, (RDFNode) null) {
            public boolean selects(Statement s) {

                double lat = s.getSubject().getProperty(locationLat).getDouble();
                double lon = s.getSubject().getProperty(locationLong).getDouble();
                return lat > minLat && lat < maxLat && lon > minLong && lon < maxLong &&
                        s.getSubject().toString().startsWith("http://units/instance/");
            }
        });
        Set<String> origins = new HashSet<String>();
        while (iter.hasNext()) {
            origins.add(iter.nextStatement().getSubject().toString());
        }
        datasetManager.end();
        return origins;
    }

    public Boolean generateUnits(double latitude, double longitude, int count) {

        getUnitsAround(latitude, longitude).size();

        return true;
    }

    public void removeExpiredUnits() {
        datasetManager.startRead();
        Model model = datasetManager.getModel();

        StmtIterator iter = model.listStatements(new SimpleSelector(null, null, (RDFNode) null) {
            public boolean selects(Statement s) {

                Property expires = model.createProperty("expires");
                Boolean expired = LocalDateTime.parse(s.getSubject().getProperty(expires).toString())
                        .isAfter(LocalDateTime.now());

                return s.getSubject().toString().startsWith("http://units/instance/") && !expired;
            }
        });
        datasetManager.end();
        datasetManager.startWrite();
        while (iter.hasNext()) {
            model.remove(iter.next());
        }
        datasetManager.commit();
        datasetManager.end();
    }

    public String deleteAll() {
        datasetManager.startWrite();
        datasetManager.getModel().removeAll();
        datasetManager.commit();
        datasetManager.end();
        return "deleted";
    }
}
