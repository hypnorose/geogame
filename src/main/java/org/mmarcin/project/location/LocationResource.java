package org.mmarcin.project.location;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/location")
public class LocationResource {
    @Inject
    LocationService locationService;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/terrain/{lat}/{lon}")
    public String location(Double lat, Double lon) {
        return locationService.location(lat, lon);
    }
}
