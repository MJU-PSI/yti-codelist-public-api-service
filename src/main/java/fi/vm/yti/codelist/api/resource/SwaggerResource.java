package fi.vm.yti.codelist.api.resource;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fi.vm.yti.codelist.api.AppInitializer;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import static fi.vm.yti.codelist.common.constants.ApiConstants.API_PATH_SWAGGER_JSON;
import static fi.vm.yti.codelist.common.constants.ApiConstants.METHOD_GET;

@Component
@Path("/swagger.json")
@Api(value = "swagger.json", description = "Operation that outputs environment specific dynamic swagger.json.")
@Produces("text/plain")
public class SwaggerResource extends AbstractBaseResource {

    private static final Logger LOG = LoggerFactory.getLogger(SwaggerResource.class);

    @GET
    @ApiOperation(value = "Get Swagger JSON", response = String.class)
    @ApiResponse(code = 200, message = "Returns the swagger.json description for this service.")
    @SuppressFBWarnings("DMI_HARDCODED_ABSOLUTE_FILENAME")
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    public String getSwaggerJson() throws IOException {
        logApiRequest(LOG, METHOD_GET, "", API_PATH_SWAGGER_JSON);
        final File file = new File(AppInitializer.LOCAL_SWAGGER_DATA_DIR + "swagger.json");
        final String swaggerJson = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
        return swaggerJson;
    }
}
