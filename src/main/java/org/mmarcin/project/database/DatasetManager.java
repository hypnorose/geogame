package org.mmarcin.project.database;


import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Selector;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.tdb.TDBFactory;

import io.quarkus.dev.testing.ContinuousTestingSharedStateManager.State;
import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Singleton;

@Singleton
@Startup
public class DatasetManager {

    public Dataset dataset;
    public HashMap<String,Property> properties;
    public DatasetManager() {
        String directory = "target/tdb";
        dataset = TDBFactory.createDataset(directory);
        properties = new HashMap<>();
        addProperty("atLat");
        addProperty("atLong");
        addProperty("FN");
        addProperty("expires");
    }
    public void addProperty(String key){
        Model m = ModelFactory.createDefaultModel();
        properties.put(key,m.createProperty(key));
    }
    public Property getProperty(String key){
        //return properties.get(key);
        Model m = ModelFactory.createDefaultModel();
        return m.createProperty(key);
    }
    public Model getModel() {
        return dataset.getDefaultModel();
    }

    public void startWrite() {
        dataset.begin(ReadWrite.WRITE);
    }

    public void startRead() {
        dataset.begin(ReadWrite.READ);

    }
    public void writeList(List<Statement> stts){

    }
    public void createLiteral(String subject, String property, Object object){
        startWrite();
        Model model = getModel();
        Resource resource = model.createResource(subject);
        resource.addLiteral(model.createProperty(property), ResourceFactory.createTypedLiteral(object));
        commit();
        end();        
    }
    public void createProperty(String subject, String property , String object){
        startWrite();
        Model model = getModel();
        Resource resource = model.createResource(subject);
        resource.addProperty(model.createProperty(property),object);
        commit();    
        end();    
    }
    public void createPropertyBulk(String subject, String property , String object){
        Model model = getModel();
        Resource resource = model.createResource(subject);
        resource.addProperty(model.createProperty(property),object);
    }
    public void createLiteralBulk(String subject, String property, Object object){
        Model model = getModel();
        Resource resource = model.createResource(subject);
        resource.addLiteral(model.createProperty(property), ResourceFactory.createTypedLiteral(object));
    }
    public Set<String> getSubjectsBySelector(Selector selector){
        startRead();
        Model model = getModel();
        StmtIterator iter = model.listStatements(selector);
        Set<String> data = new HashSet<String>();
        while (iter.hasNext()) {
            data.add(iter.nextStatement().getSubject().toString());
        }
        end();
        return data;
    }
    public Resource getResource(String name){
        startRead();
        Model model = getModel();
        Resource resource = model.getResource(name);
        end();
        return resource;
    }
    public Statement getStatement(Resource resource, String property){
        startRead();
        Model model = getModel();
        Statement st = resource.getProperty(getProperty(property));
        end();
        return st;
    }
    public void deleteBySelector(Selector selector){
        startRead();
        Model model = getModel();
        StmtIterator iter = model.listStatements(selector);
        end();
        startWrite();
        while (iter.hasNext()) {
            getModel().remove(iter.next());
        }
        commit();
        end();
    }

    public void end() {
        dataset.end();
    }

    public void commit() {
        dataset.commit();

    }
}
