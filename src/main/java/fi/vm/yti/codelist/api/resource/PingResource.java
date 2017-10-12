package fi.vm.yti.codelist.api.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import static fi.vm.yti.codelist.common.constants.ApiConstants.API_PATH_PING;
import static fi.vm.yti.codelist.common.constants.ApiConstants.METHOD_GET;

/**
 * REST resources for service responsiveness testing.
 */
@Component
@Path("/ping")
@Api(value = "ping")
@Produces("text/plain")
public class PingResource extends AbstractBaseResource {

    private static final Logger LOG = LoggerFactory.getLogger(PingResource.class);

    @GET
    @ApiOperation(value = "Return pong upon successful API request.", response = String.class)
    @ApiResponse(code = 200, message = "Returns the String 'pong'.")
    @Produces("text/plain")
    public Response ping() {
        logApiRequest(LOG, METHOD_GET, "", API_PATH_PING);
        return Response.ok("pong").build();
    }
}
