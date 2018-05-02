package fi.vm.yti.codelist.api.resource;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import fi.vm.yti.codelist.api.api.ApiUtils;
import fi.vm.yti.codelist.api.domain.Domain;
import fi.vm.yti.codelist.api.exception.YtiCodeListException;
import fi.vm.yti.codelist.common.dto.CodeDTO;
import fi.vm.yti.codelist.common.dto.CodeRegistryDTO;
import fi.vm.yti.codelist.common.dto.CodeSchemeDTO;
import fi.vm.yti.codelist.common.dto.ErrorModel;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@Component
@Path("/v1/uris")
@Api(value = "uris")
@Produces({MediaType.APPLICATION_JSON + ";charset=UTF-8", "application/xlsx", "application/csv"})
public class UriResolverResource extends AbstractBaseResource {

    private static final Logger LOG = LoggerFactory.getLogger(UriResolverResource.class);
    private static final String SUOMI_URI_HOST = "uri.suomi.fi";
    private static final String API_PATH_CODELIST = "/codelist";

    private final ApiUtils apiUtils;
    private final Domain domain;

    @Inject
    public UriResolverResource(final ApiUtils apiUtils,
                               final Domain domain) {
        this.apiUtils = apiUtils;
        this.domain = domain;
    }

    @GET
    @Path("resolve")
    @ApiOperation(value = "Resolve URI resource.", response = String.class)
    @ApiResponse(code = 200, message = "Resolves the API url for the given codelist resource URI.")
    @Produces({MediaType.APPLICATION_JSON + ";charset=UTF-8", MediaType.TEXT_PLAIN})
    public Response resolveUri(@ApiParam(value = "Resource URI.", required = true) @QueryParam("uri") final String uri) {
        final URI resolveUri = parseUriFromString(uri);
        ensureSuomiFiUriHost(resolveUri.getHost());
        final String uriPath = resolveUri.getPath();
        final ObjectMapper objectMapper = new ObjectMapper();
        final ObjectNode json = objectMapper.createObjectNode();
        json.put("uri", uri);
        final String resourcePath = uriPath.substring(API_PATH_CODELIST.length() + 1);
        checkResourceValidity(resourcePath);
        final List<String> resourceCodeValues = Arrays.asList(resourcePath.split("/"));
        json.put("url", resolveApiResourceUrl(resourceCodeValues));
        return Response.ok().entity(json).build();
    }

    @GET
    @Path("redirect")
    @ApiOperation(value = "Redirect URI resource.")
    @Consumes({MediaType.APPLICATION_JSON, MediaType.TEXT_HTML})
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_HTML})
    @ApiResponses(value = {
        @ApiResponse(code = 302, message = "Does a redirect from codelist resource URI to codelist API."),
        @ApiResponse(code = 406, message = "Resource not found."),
        @ApiResponse(code = 406, message = "Cannot redirect to given URI.")
    })
    public Response redirectUri(@HeaderParam("Accept") String accept,
                                @ApiParam(value = "Resource URI.", required = true) @QueryParam("uri") final String uri) {
        final URI resolveUri = parseUriFromString(uri);
        ensureSuomiFiUriHost(resolveUri.getHost());
        final String uriPath = resolveUri.getPath();
        checkResourceValidity(uriPath);
        final String resourcePath = uriPath.substring(API_PATH_CODELIST.length() + 1);
        final List<String> resourceCodeValues = Arrays.asList(resourcePath.split("/"));
        final List<String> acceptHeaders = Arrays.asList(accept.split(","));
        if (acceptHeaders.contains(MediaType.APPLICATION_JSON)) {
            final URI redirectUrl = URI.create(resolveApiResourceUrl(resourceCodeValues));
            return Response.temporaryRedirect(redirectUrl).build();
        } else if (acceptHeaders.isEmpty() || acceptHeaders.contains(MediaType.TEXT_HTML)){
            final URI redirectUrl = URI.create(resolveWebResourceUrl(resourceCodeValues));
            return Response.temporaryRedirect(redirectUrl).build();
        } else {
            LOG.error("Unknown Accept-header: " + accept);
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), "Unknown Accept-header: " + accept));
        }
    }

    private void ensureSuomiFiUriHost(final String host) {
        if (!SUOMI_URI_HOST.equalsIgnoreCase(host)) {
            LOG.error("This URI is not resolvable as a codelist resource, wrong host.");
            throw new WebApplicationException("This URI is not resolvable as a codelist resource.");
        }
    }

    private URI parseUriFromString(final String uriString) {
        if (!uriString.isEmpty()) {
            return URI.create(uriString.replace(" ", "%20"));
        } else {
            LOG.error("URI string was not valid!");
            throw new WebApplicationException("URI string was not valid!");
        }
    }

    private String resolveApiResourceUrl(final List<String> resourceCodeValues) {
        final String url;
        if (resourceCodeValues.size() == 1) {
            final String codeRegistryCodeValue = checkNotEmpty(resourceCodeValues.get(0));
            checkCodeRegistryExists(codeRegistryCodeValue);
            url = apiUtils.createCodeRegistryUrl(codeRegistryCodeValue);
        } else if (resourceCodeValues.size() == 2) {
            final String codeRegistryCodeValue = checkNotEmpty(resourceCodeValues.get(0));
            final String codeSchemeCodeValue = checkNotEmpty(resourceCodeValues.get(1));
            checkCodeSchemeExists(codeRegistryCodeValue, codeSchemeCodeValue);
            url = apiUtils.createCodeSchemeUrl(codeRegistryCodeValue, codeSchemeCodeValue);
        } else if (resourceCodeValues.size() == 3) {
            final String codeRegistryCodeValue = checkNotEmpty(resourceCodeValues.get(0));
            final String codeSchemeCodeValue = checkNotEmpty(resourceCodeValues.get(1));
            final String codeCodeValue = checkNotEmpty(resourceCodeValues.get(2));
            checkCodeExists(codeRegistryCodeValue, codeSchemeCodeValue, codeCodeValue);
            url = apiUtils.createCodeUrl(codeRegistryCodeValue, codeSchemeCodeValue, codeCodeValue);
        } else {
            LOG.error("Codelist resource URI not resolvable!");
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), "Codelist resource URI not resolvable!"));
        }
        return url;
    }

    private String resolveWebResourceUrl(final List<String> resourceCodeValues) {
        final String url;
        if (resourceCodeValues.size() == 2) {
            final String codeRegistryCodeValue = checkNotEmpty(resourceCodeValues.get(0));
            final String codeSchemeCodeValue = checkNotEmpty(resourceCodeValues.get(1));
            checkCodeSchemeExists(codeRegistryCodeValue, codeSchemeCodeValue);
            url = apiUtils.createCodeSchemeWebUrl(codeRegistryCodeValue, codeSchemeCodeValue);
        } else if (resourceCodeValues.size() == 3) {
            final String codeRegistryCodeValue = checkNotEmpty(resourceCodeValues.get(0));
            final String codeSchemeCodeValue = checkNotEmpty(resourceCodeValues.get(1));
            final String codeCodeValue = checkNotEmpty(resourceCodeValues.get(2));
            checkCodeExists(codeRegistryCodeValue, codeSchemeCodeValue, codeCodeValue);
            url = apiUtils.createCodeWebUrl(codeRegistryCodeValue, codeSchemeCodeValue, codeCodeValue);
        } else {
            LOG.error("Codelist resource URI not resolvable!");
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), "Codelist resource URI not resolvable!"));
        }
        return url;
    }

    private void checkResourceValidity(final String uriPath) {
        final String resourcePath = uriPath.substring(API_PATH_CODELIST.length() + 1);
        final List<String> resourceCodeValues = Arrays.asList(resourcePath.split("/"));
        if (!uriPath.toLowerCase().startsWith(API_PATH_CODELIST)) {
            LOG.error("Codelist resource URI not resolvable, wrong context path!");
            throw new WebApplicationException("Codelist resource URI not resolvable, wrong context path!");
        } else if (resourceCodeValues.isEmpty()) {
            LOG.error("Codelist resource URI not resolvable, empty resource path!");
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), "Codelist resource URI not resolvable, empty resource path!"));
        }
    }

    private String checkNotEmpty(final String string) {
        if (string != null && !string.isEmpty()) {
            return string;
        } else {
            LOG.error("Resource hook not valid due to empty resource ID.");
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), "Resource hook not valid due to empty resource ID."));
        }
    }

    private void checkCodeRegistryExists(final String codeRegistryCodeValue) {
        final CodeRegistryDTO codeRegistry = domain.getCodeRegistry(codeRegistryCodeValue);
        if (codeRegistry == null) {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_FOUND.value(), "Resource not found."));
        }
    }

    private void checkCodeSchemeExists(final String codeRegistryCodeValue,
                                       final String codeSchemeCodeValue) {
        final CodeSchemeDTO codeScheme = domain.getCodeScheme(codeRegistryCodeValue, codeSchemeCodeValue);
        if (codeScheme == null) {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_FOUND.value(), "Resource not found."));
        }
    }

    private void checkCodeExists(final String codeRegistryCodeValue,
                                       final String codeSchemeCodeValue,
                                       final String codeCodeValue) {
        final CodeDTO code = domain.getCode(codeRegistryCodeValue, codeSchemeCodeValue, codeCodeValue);
        if (code == null) {
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_FOUND.value(), "Resource not found."));
        }
    }
}
