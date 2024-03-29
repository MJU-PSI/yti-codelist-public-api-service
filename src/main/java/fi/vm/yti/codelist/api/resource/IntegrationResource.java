package fi.vm.yti.codelist.api.resource;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.validation.constraints.Pattern;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.Encoded;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.jackson.internal.jackson.jaxrs.cfg.ObjectWriterInjector;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import fi.vm.yti.codelist.api.api.ApiUtils;
import fi.vm.yti.codelist.api.api.ResponseWrapper;
import fi.vm.yti.codelist.api.domain.Domain;
import fi.vm.yti.codelist.api.dto.IntegrationResourceRequestDTO;
import fi.vm.yti.codelist.api.dto.ResourceDTO;
import fi.vm.yti.codelist.api.exception.YtiCodeListException;
import fi.vm.yti.codelist.common.dto.ErrorModel;
import fi.vm.yti.codelist.common.dto.Meta;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import static fi.vm.yti.codelist.common.constants.ApiConstants.*;
import static java.util.Arrays.asList;

@Component
@Path("/v1/integration")
@Produces({ MediaType.APPLICATION_JSON + ";charset=UTF-8" })
@Tag(name = "Integration")
public class IntegrationResource extends AbstractBaseResource {

    private final Domain domain;
    private final ApiUtils apiUtils;

    @Inject
    public IntegrationResource(final Domain domain,
                               final ApiUtils apiUtils) {
        this.domain = domain;
        this.apiUtils = apiUtils;
    }

    @GET
    @Path("/containers")
    @Operation(description = "API for fetching container resources")
    @ApiResponse(responseCode = "200", description = "Returns container resources with meta element that shows details and a results list.")
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    public Response getContainers(@Parameter(description = "Language code for sorting results.", in = ParameterIn.QUERY) @QueryParam("language") @DefaultValue("fi") final String language,
                                  @Parameter(description = "Pagination parameter for page size.", in = ParameterIn.QUERY) @QueryParam("pageSize") final Integer pageSize,
                                  @Parameter(description = "Pagination parameter for start index.", in = ParameterIn.QUERY) @QueryParam("from") @DefaultValue("0") final Integer from,
                                  @Parameter(description = "Status enumerations in CSL format.", in = ParameterIn.QUERY) @QueryParam("status") final String status,
                                  @Parameter(description = "After date filtering parameter, results will be codes with modified date after this ISO 8601 formatted date string.", in = ParameterIn.QUERY) @QueryParam("after") @Pattern(regexp = "[0-9TZ\\-\\:\\+]+") final String after,
                                  @Parameter(description = "Before date filtering parameter, results will be codes with modified date before this ISO 8601 formatted date string.", in = ParameterIn.QUERY) @QueryParam("before") @Pattern(regexp = "[0-9TZ\\-\\:\\+]+") final String before,
                                  @Parameter(description = "Search term used to filter results based on partial prefLabel or codeValue match.", in = ParameterIn.QUERY) @QueryParam("searchTerm") final String searchTerm,
                                  @Parameter(description = "Container URIs that are included.", in = ParameterIn.QUERY) @Encoded @QueryParam("uri") final String uri,
                                  @Parameter(description = "Container URIs that are excluded.", in = ParameterIn.QUERY) @Encoded @QueryParam("filter") final String filter,
                                  @Parameter(description = "User organizations filtering parameter, for filtering incomplete code lists", in = ParameterIn.QUERY) @QueryParam("includeIncompleteFrom") final String includeIncompleteFrom,
                                  @Parameter(description = "Control boolean for returning all incomplete containers.", in = ParameterIn.QUERY) @QueryParam("includeIncomplete") @DefaultValue("false") final boolean includeIncomplete,
                                  @Parameter(description = "Pretty format JSON output.", in = ParameterIn.QUERY) @QueryParam("pretty") final String pretty) {
        ObjectWriterInjector.set(new FilterModifier(createSimpleFilterProvider(), pretty));
        final Meta meta = new Meta(200, pageSize, from, parseDateFromString(after), parseDateFromString(before));
        final List<String> includedContainerUris = parseAndDecodeUrisFromCsl(uri);
        final List<String> excludedContainerUris = parseAndDecodeUrisFromCsl(filter);
        final List<String> statusList = parseStatusCsl(status);
        final List<String> includeIncompleteFromList = includeIncompleteFrom == null ? null : asList(includeIncompleteFrom.toLowerCase().split(","));
        final Set<ResourceDTO> containers = domain.getContainers(includedContainerUris, excludedContainerUris, language, statusList, searchTerm, includeIncompleteFromList, includeIncomplete, meta);
        if (pageSize != null && from + pageSize < meta.getTotalResults()) {
            meta.setNextPage(apiUtils.createNextPageUrl(API_VERSION, API_PATH_INTEGRATION + API_PATH_CONTAINERS, after, pageSize, from + pageSize));
        }
        final ResponseWrapper<ResourceDTO> wrapper = new ResponseWrapper<>();
        wrapper.setResults(containers);
        wrapper.setMeta(meta);
        return Response.ok(wrapper).build();
    }

    @POST
    @Path("/containers")
    @Operation(description = "API for fetching container resources")
    @ApiResponse(responseCode = "200", description = "Returns container resources with meta element that shows details and a results list.")
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    public Response getContainersPost(@Parameter(description = "Integration resource request parameters as JSON payload.") @RequestBody final String integrationRequestData) {
        final IntegrationResourceRequestDTO request = parseIntegrationRequestDto(integrationRequestData);
        ObjectWriterInjector.set(new FilterModifier(createSimpleFilterProvider(), request.getPretty()));
        final List<String> statusList = parseStatusList(request.getStatus());
        final List<String> excludedContainerUris = convertListToLowerCase(request.getFilter());
        final List<String> includedContainerUris = convertListToLowerCase(request.getUri());
        final List<String> includeIncompleteFromList = request.getIncludeIncompleteFrom();
        final Integer pageSize = request.getPageSize();
        final Integer from = request.getPageFrom();
        final String after = request.getAfter();
        final String before = request.getBefore();
        final String searchTerm = request.getSearchTerm();
        final String language = request.getLanguage();
        final boolean includeIncomplete = request.getIncludeIncomplete();
        final Meta meta = new Meta(200, pageSize, from, parseDateFromString(after), parseDateFromString(before));
        final Set<ResourceDTO> containers = domain.getContainers(includedContainerUris, excludedContainerUris, language, statusList, searchTerm, includeIncompleteFromList, includeIncomplete, meta);
        if (pageSize != null && from + pageSize < meta.getTotalResults()) {
            meta.setNextPage(apiUtils.createNextPageUrl(API_VERSION, API_PATH_INTEGRATION + API_PATH_CONTAINERS, after, pageSize, from + pageSize));
        }
        final ResponseWrapper<ResourceDTO> wrapper = new ResponseWrapper<>();
        wrapper.setResults(containers);
        wrapper.setMeta(meta);
        return Response.ok(wrapper).build();
    }

    @GET
    @Path("/resources")
    @Operation(description = "API for fetching resources for a container")
    @ApiResponse(responseCode = "200", description = "Returns resources for a specific container with meta element that shows details and a results list.")
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    public Response getResources(@Parameter(description = "Language code for sorting results.", in = ParameterIn.QUERY) @DefaultValue("fi") final String language,
                                 @Parameter(description = "Pagination parameter for page size.", in = ParameterIn.QUERY) @QueryParam("pageSize") final Integer pageSize,
                                 @Parameter(description = "Pagination parameter for start index.", in = ParameterIn.QUERY) @QueryParam("from") @DefaultValue("0") final Integer from,
                                 @Parameter(description = "Status enumerations in CSL format.", in = ParameterIn.QUERY) @QueryParam("status") final String status,
                                 @Parameter(description = "After date filtering parameter, results will be codes with modified date after this ISO 8601 formatted date string.", in = ParameterIn.QUERY) @QueryParam("after") final String after,
                                 @Parameter(description = "Before date filtering parameter, results will be codes with modified date before this ISO 8601 formatted date string.", in = ParameterIn.QUERY) @QueryParam("before") final String before,
                                 @Parameter(description = "Container URIs.", in = ParameterIn.QUERY) @QueryParam("container") final String container,
                                 @Parameter(description = "Type for filtering resources.", in = ParameterIn.QUERY) @QueryParam("type") final String type,
                                 @Parameter(description = "Resource URIs that are included.", in = ParameterIn.QUERY) @Encoded @QueryParam("uri") final String uri,
                                 @Parameter(description = "Resource URIs that are filtered.", in = ParameterIn.QUERY) @Encoded @QueryParam("filter") final String filter,
                                 @Parameter(description = "Search term used to filter results based on partial prefLabel or codeValue match.", in = ParameterIn.QUERY) @QueryParam("searchTerm") final String searchTerm,
                                 @Parameter(description = "User organizations filtering parameter, for filtering incomplete code lists", in = ParameterIn.QUERY) @QueryParam("includeIncompleteFrom") final String includeIncompleteFrom,
                                 @Parameter(description = "Control boolean for returning resources from incomplete code lists.", in = ParameterIn.QUERY) @QueryParam("includeIncomplete") @DefaultValue("false") final boolean includeIncomplete,
                                 @Parameter(description = "Pretty format JSON output.", in = ParameterIn.QUERY) @QueryParam("pretty") final String pretty) {
        ObjectWriterInjector.set(new FilterModifier(createSimpleFilterProvider(), pretty));
        final List<String> containerUris = container == null ? null : asList(container.toLowerCase().split(","));
        final List<String> includedResourceUris = parseAndDecodeUrisFromCsl(uri);
        final List<String> excludedResourceUris = parseAndDecodeUrisFromCsl(filter);
        final List<String> includeIncompleteFromList = includeIncompleteFrom == null ? null : asList(includeIncompleteFrom.toLowerCase().split(","));
        final List<String> statusList = parseStatusCsl(status);
        final Meta meta = new Meta(200, pageSize, from, parseDateFromString(after), parseDateFromString(before));
        final Set<ResourceDTO> resources = domain.getResources(containerUris, includedResourceUris, excludedResourceUris, language, statusList, searchTerm, type, includeIncompleteFromList, includeIncomplete, meta);
        if (pageSize != null && from + pageSize < meta.getTotalResults()) {
            if (container != null) {
                meta.setNextPage(apiUtils.createNextPageUrl(API_VERSION, API_PATH_INTEGRATION + API_PATH_RESOURCES, after, pageSize, from + pageSize) + "&container=" + container);
            }
            meta.setNextPage(apiUtils.createNextPageUrl(API_VERSION, API_PATH_INTEGRATION + API_PATH_RESOURCES, after, pageSize, from + pageSize) + "&container=" + container);
        }
        final ResponseWrapper<ResourceDTO> wrapper = new ResponseWrapper<>();
        wrapper.setResults(resources);
        wrapper.setMeta(meta);
        return Response.ok(wrapper).build();
    }

    @POST
    @Path("/resources")
    @Operation(description = "API for fetching resources for a container")
    @ApiResponse(responseCode = "200", description = "Returns resources for a specific container with meta element that shows details and a results list.")
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    public Response getResourcesPost(@Parameter(description = "Integration resource request parameters as JSON payload.") @RequestBody final String integrationRequestData) {
        final IntegrationResourceRequestDTO request = parseIntegrationRequestDto(integrationRequestData);
        ObjectWriterInjector.set(new FilterModifier(createSimpleFilterProvider(), request.getPretty()));
        final List<String> containerUris = convertListToLowerCase(request.getContainer());
        final List<String> excludedResourceUris = convertListToLowerCase(request.getFilter());
        final List<String> includedResourceUris = convertListToLowerCase(request.getUri());
        final List<String> includeIncompleteFromList = request.getIncludeIncompleteFrom();
        final boolean includeIncomplete = request.getIncludeIncomplete();
        final List<String> statusList = parseStatusList(request.getStatus());
        final Integer pageSize = request.getPageSize();
        final Integer from = request.getPageFrom();
        final String after = request.getAfter();
        final String before = request.getBefore();
        final String language = request.getLanguage();
        final String type = request.getType();
        final String searchTerm = request.getSearchTerm();
        final Meta meta = new Meta(200, pageSize, from, parseDateFromString(after), parseDateFromString(before));
        final Set<ResourceDTO> resources = domain.getResources(containerUris, includedResourceUris, excludedResourceUris, language, statusList, searchTerm, type, includeIncompleteFromList, includeIncomplete, meta);
        final ResponseWrapper<ResourceDTO> wrapper = new ResponseWrapper<>();
        wrapper.setResults(resources);
        wrapper.setMeta(meta);
        return Response.ok(wrapper).build();
    }

    private List<String> convertListToLowerCase(final List<String> list) {
        if (list != null && !list.isEmpty()) {
            return list.stream().map(String::toLowerCase).collect(Collectors.toList());
        }
        return null;
    }

    private IntegrationResourceRequestDTO parseIntegrationRequestDto(final String integrationRequestData) {
        try {
            final ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(integrationRequestData, new TypeReference<IntegrationResourceRequestDTO>() {
            });
        } catch (IOException e) {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), "Malformed resources in request body!"));
        }
    }
}
