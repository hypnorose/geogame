package org.mmarcin.project.location;

import de.westnordost.osmapi.map.data.BoundingBox;
import de.westnordost.osmapi.map.data.Node;
import de.westnordost.osmapi.map.data.Relation;
import de.westnordost.osmapi.map.data.Way;
import de.westnordost.osmapi.map.handler.MapDataHandler;
import jakarta.enterprise.context.Dependent;


public class LocationHandler implements MapDataHandler {
    public String output = "asd";
    @Override
    public void handle(BoundingBox bounds) {
       
    }

    @Override
    public void handle(Node node) {
        output+=node.getTags().toString()+"\n";
    }

    @Override
    public void handle(Way way) {
        output+="asd";
        output+=way.getTags().toString()+"\n";
    }

    @Override
    public void handle(Relation relation) {
        output+=relation.getTags().toString()+"\n";
    }
    
}
