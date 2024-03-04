package org.mmarcin.project.database;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.tdb.TDBFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Singleton;

@Singleton
public class DatasetManager {

    public Dataset dataset;

    public DatasetManager() {
        String directory = "target/tdb";
        dataset = TDBFactory.createDataset(directory);

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

    public void end() {
        dataset.end();
    }

    public void commit() {
        dataset.commit();

    }
}
