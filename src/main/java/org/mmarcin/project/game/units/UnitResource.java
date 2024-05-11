package org.mmarcin.project.game.units;

import java.util.Collection;
import java.util.Map;

import javax.print.attribute.standard.Media;

import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import org.jboss.resteasy.reactive.server.multipart.FormValue;
import org.jboss.resteasy.reactive.server.multipart.MultipartFormDataInput;
import org.mmarcin.project.game.units.dto.WarriorData;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/units")
public class UnitResource {

    private static final Logger LOG = Logger.getLogger(UnitResource.class);
    @Inject
    UnitService unitService;

    @ServerExceptionMapper
    public RestResponse<String> mapException(RuntimeException e) {
        LOG.debug(("dupa"));
        e.printStackTrace();
        return RestResponse.status(Response.Status.NOT_FOUND, e.getMessage() + e.getLocalizedMessage());
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public String addUnit(WarriorData warriorData) {

        return unitService.createUnit(warriorData.name,
                Double.parseDouble(warriorData.lat),
                Double.parseDouble(warriorData.lon));

    }

    @GET
    @Path("/at/{lat}/{lon}")
    @Produces(MediaType.TEXT_PLAIN)

    public String getUnits(Double lat, Double lon) {
        return unitService.getUnits(lat, lon);
    }

    @GET
    @Path("/check/{lat}/{lon}")
    @Produces(MediaType.APPLICATION_JSON)
    public String checkUnitCountAround(Double lat, Double lon) {
        unitService.generateUnits(lat, lon);
        return unitService.getUnits(lat, lon).toString();
    }

    @DELETE
    @Produces(MediaType.TEXT_PLAIN)
    public String deleteUnits() {
        return unitService.deleteAll();
    }
}
