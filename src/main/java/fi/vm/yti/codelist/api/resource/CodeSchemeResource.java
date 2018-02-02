package fi.vm.yti.codelist.api.resource;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.apache.poi.ss.usermodel.Workbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.jaxrs.cfg.ObjectWriterInjector;

import fi.vm.yti.codelist.api.api.ResponseWrapper;
import fi.vm.yti.codelist.api.domain.Domain;
import fi.vm.yti.codelist.common.model.CodeRegistry;
import fi.vm.yti.codelist.common.model.CodeScheme;
import fi.vm.yti.codelist.common.model.Meta;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import static fi.vm.yti.codelist.common.constants.ApiConstants.API_PATH_CODESCHEMES;
import static fi.vm.yti.codelist.common.constants.ApiConstants.API_PATH_VERSION_V1;
import static fi.vm.yti.codelist.common.constants.ApiConstants.FILTER_NAME_CODESCHEME;
import static fi.vm.yti.codelist.common.constants.ApiConstants.FORMAT_CSV;
import static fi.vm.yti.codelist.common.constants.ApiConstants.FORMAT_EXCEL;
import static fi.vm.yti.codelist.common.constants.ApiConstants.FORMAT_EXCEL_XLS;
import static fi.vm.yti.codelist.common.constants.ApiConstants.FORMAT_EXCEL_XLSX;
import static fi.vm.yti.codelist.common.constants.ApiConstants.FORMAT_JSON;
import static fi.vm.yti.codelist.common.constants.ApiConstants.METHOD_GET;

/**
 * REST resources for CodeSchemes.
 */
@Component
@Path("/v1/codeschemes")
@Api(value = "codeschemes")
@Produces({MediaType.APPLICATION_JSON + ";charset=UTF-8", "application/xlsx", "application/csv"})
public class CodeSchemeResource extends AbstractBaseResource {

    private static final Logger LOG = LoggerFactory.getLogger(CodeSchemeResource.class);
    private final Domain domain;

    @Inject
    public CodeSchemeResource(final Domain domain) {
        this.domain = domain;
    }

    @GET
    @ApiOperation(value = "Return list of available CodeSchemes.", response = CodeRegistry.class, responseContainer = "List")
    @ApiResponse(code = 200, message = "Returns all Registers in JSON format.")
    @Produces({MediaType.APPLICATION_JSON + ";charset=UTF-8", MediaType.TEXT_PLAIN})
    public Response getCodeSchemes(@ApiParam(value = "CodeRegistry CodeValue.") @QueryParam("codeRegistryCodeValue") final String codeRegistryCodeValue,
                                   @ApiParam(value = "CodeRegistry Name.") @QueryParam("codeRegistryName") final String codeRegistryPrefLabel,
                                   @ApiParam(value = "Pagination parameter for page size.") @QueryParam("pageSize") final Integer pageSize,
                                   @ApiParam(value = "Pagination parameter for start index.") @QueryParam("from") @DefaultValue("0") final Integer from,
                                   @ApiParam(value = "Service classifications in CSL format.") @QueryParam("dataClassification") final String dataClassification,
                                   @ApiParam(value = "Organization id for content filtering.") @QueryParam("organizationId") final String organizationId,
                                   @ApiParam(value = "CodeScheme codeValue as string value.") @QueryParam("codeValue") final String codeSchemeCodeValue,
                                   @ApiParam(value = "CodeScheme PrefLabel as string value.") @QueryParam("prefLabel") final String codeSchemePrefLabel,
                                   @ApiParam(value = "Status enumerations in CSL format.") @QueryParam("status") final String status,
                                   @ApiParam(value = "Format for content.") @QueryParam("format") @DefaultValue(FORMAT_JSON) final String format,
                                   @ApiParam(value = "After date filtering parameter, results will be codes with modified date after this ISO 8601 formatted date string.") @QueryParam("after") final String after,
                                   @ApiParam(value = "Filter string (csl) for expanding specific child resources.") @QueryParam("expand") final String expand) {
        logApiRequest(LOG, METHOD_GET, API_PATH_VERSION_V1, API_PATH_CODESCHEMES + "/");
        final List<String> dataClassificationList = parseDataClassifications(dataClassification);
        final List<String> statusList = parseStatus(status);
        if (FORMAT_CSV.startsWith(format.toLowerCase())) {
            final Set<CodeScheme> codeSchemes = domain.getCodeSchemes(pageSize, from, organizationId, codeRegistryCodeValue, codeRegistryPrefLabel, codeSchemeCodeValue, codeSchemePrefLabel, statusList, dataClassificationList, Meta.parseAfterFromString(after), null);
            final String csv = constructCodeSchemesCsv(codeSchemes);
            final StreamingOutput stream = output -> {
                try {
                    output.write(csv.getBytes(StandardCharsets.UTF_8));
                } catch (final Exception e) {
                    throw new WebApplicationException(e);
                }
            };
            return Response.ok(stream).header(HEADER_CONTENT_DISPOSITION, "attachment; filename = " + createDownloadFilename(format, DOWNLOAD_FILENAME_CODESCHEMES)).build();
        } else if (FORMAT_EXCEL.equalsIgnoreCase(format) || FORMAT_EXCEL_XLS.equalsIgnoreCase(format) || FORMAT_EXCEL_XLSX.equalsIgnoreCase(format)) {
            final Set<CodeScheme> codeSchemes = domain.getCodeSchemes(pageSize, from, organizationId, codeRegistryCodeValue, codeRegistryPrefLabel, codeSchemeCodeValue, codeSchemePrefLabel, statusList, dataClassificationList, Meta.parseAfterFromString(after), null);
            final Workbook workbook = constructCodeSchemesExcel(format, codeSchemes);
            final StreamingOutput stream = output -> {
                try {
                    workbook.write(output);
                } catch (final Exception e) {
                    throw new WebApplicationException(e);
                }
            };
            return Response.ok(stream).header(HEADER_CONTENT_DISPOSITION, "attachment; filename = " + createDownloadFilename(format, DOWNLOAD_FILENAME_CODESCHEMES)).build();
        } else {
            final Meta meta = new Meta(200, null, null, after);
            ObjectWriterInjector.set(new AbstractBaseResource.FilterModifier(createSimpleFilterProvider(FILTER_NAME_CODESCHEME, expand)));
            final Set<CodeScheme> codeSchemes = domain.getCodeSchemes(pageSize, from, organizationId, codeRegistryCodeValue, codeRegistryPrefLabel, codeSchemeCodeValue, codeSchemePrefLabel, statusList, dataClassificationList, meta.getAfter(), meta);
            meta.setResultCount(codeSchemes.size());
            final ResponseWrapper<CodeScheme> wrapper = new ResponseWrapper<>();
            wrapper.setResults(codeSchemes);
            wrapper.setMeta(meta);
            return Response.ok(wrapper).build();
        }
    }

    @GET
    @Path("{codeSchemeId}")
    @ApiOperation(value = "Return one specific CodeScheme.", response = CodeScheme.class)
    @ApiResponse(code = 200, message = "Returns one specific CodeScheme in JSON format.")
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    public Response getCodeScheme(@ApiParam(value = "CodeScheme CodeValue.", required = true) @PathParam("codeSchemeId") final String codeSchemeId,
                                  @ApiParam(value = "Filter string (csl) for expanding specific child resources.") @QueryParam("expand") final String expand) {
        logApiRequest(LOG, METHOD_GET, API_PATH_VERSION_V1, API_PATH_CODESCHEMES + "/" + codeSchemeId + "/");
        ObjectWriterInjector.set(new AbstractBaseResource.FilterModifier(createSimpleFilterProvider(FILTER_NAME_CODESCHEME, expand)));
        final CodeScheme codeScheme = domain.getCodeScheme(codeSchemeId);
        if (codeScheme != null) {
            return Response.ok(codeScheme).build();
        } else {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }
}
