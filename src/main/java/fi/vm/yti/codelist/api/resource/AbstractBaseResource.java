package fi.vm.yti.codelist.api.resource;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.apache.poi.ss.usermodel.Workbook;
import org.glassfish.jersey.jackson.internal.jackson.jaxrs.cfg.EndpointConfigBase;
import org.glassfish.jersey.jackson.internal.jackson.jaxrs.cfg.ObjectWriterModifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.util.HtmlUtils;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.fasterxml.jackson.databind.util.ISO8601DateFormat;

import fi.vm.yti.codelist.api.configuration.UriProperties;
import fi.vm.yti.codelist.api.exception.YtiCodeListException;
import fi.vm.yti.codelist.common.dto.ErrorModel;
import fi.vm.yti.codelist.common.model.Status;
import static fi.vm.yti.codelist.api.util.EncodingUtils.urlDecodeString;
import static fi.vm.yti.codelist.common.constants.ApiConstants.*;

abstract class AbstractBaseResource {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractBaseResource.class);
    private static final String DOWNLOAD_FILENAME_CODEREGISTRIES = "coderegistries";
    private static final String DOWNLOAD_FILENAME_CODESCHEMES = "codeschemes";
    private static final String DOWNLOAD_FILENAME_CODES = "codes";
    private static final String DOWNLOAD_FILENAME_EXTERNALREFERENCES = "externalreferences";
    private static final String DOWNLOAD_FILENAME_PROPERTYTYPES = "propertytypes";
    private static final String DOWNLOAD_FILENAME_VALUETYPES = "valuetypes";
    private static final String DOWNLOAD_FILENAME_EXTENSIONS = "extensions";
    private static final String DOWNLOAD_FILENAME_MEMBERS = "members";
    private static final String HEADER_CONTENT_DISPOSITION = "content-disposition";
    private static final String DOWNLOAD_FILENAME_CROSS_REFERENCE_LIST = "crossreferencelist";

    @Autowired
    UriProperties uriProperties;

    SimpleFilterProvider createSimpleFilterProvider() {
        return createSimpleFilterProvider(FILTER_NAME_RESOURCE, null);
    }

    SimpleFilterProvider createSimpleFilterProvider(final String baseFilter,
                                                    final String expand) {
        final List<String> baseFilters = new ArrayList<>();
        baseFilters.add(baseFilter);
        return createSimpleFilterProvider(baseFilters, expand);
    }

    private SimpleFilterProvider createBaseFilterProvider() {
        final SimpleFilterProvider filterProvider = new SimpleFilterProvider();
        filterProvider.addFilter(FILTER_NAME_CODEREGISTRY, SimpleBeanPropertyFilter.filterOutAllExcept(FIELD_NAME_URI, FIELD_NAME_URL));
        filterProvider.addFilter(FILTER_NAME_CODESCHEME, SimpleBeanPropertyFilter.filterOutAllExcept(FIELD_NAME_URI, FIELD_NAME_URL));
        filterProvider.addFilter(FILTER_NAME_CODE, SimpleBeanPropertyFilter.filterOutAllExcept(FIELD_NAME_URI, FIELD_NAME_URL));
        filterProvider.addFilter(FILTER_NAME_EXTERNALREFERENCE, SimpleBeanPropertyFilter.filterOutAllExcept(FIELD_NAME_URL));
        filterProvider.addFilter(FILTER_NAME_PROPERTYTYPE, SimpleBeanPropertyFilter.filterOutAllExcept(FIELD_NAME_URI, FIELD_NAME_URL));
        filterProvider.addFilter(FILTER_NAME_INFODOMAIN, SimpleBeanPropertyFilter.filterOutAllExcept(FIELD_NAME_URL));
        filterProvider.addFilter(FILTER_NAME_ORGANIZATION, SimpleBeanPropertyFilter.filterOutAllExcept(FIELD_NAME_ID));
        filterProvider.addFilter(FILTER_NAME_EXTENSION, SimpleBeanPropertyFilter.filterOutAllExcept(FIELD_NAME_URI, FIELD_NAME_URL));
        filterProvider.addFilter(FILTER_NAME_MEMBER, SimpleBeanPropertyFilter.filterOutAllExcept(FIELD_NAME_URI, FIELD_NAME_URL));
        filterProvider.addFilter(FILTER_NAME_VALUETYPE, SimpleBeanPropertyFilter.filterOutAllExcept(FIELD_NAME_URI, FIELD_NAME_URL));
        filterProvider.addFilter(FILTER_NAME_MEMBERVALUE, SimpleBeanPropertyFilter.filterOutAllExcept(FIELD_NAME_ID));
        filterProvider.addFilter(FILTER_NAME_SEARCHHIT, SimpleBeanPropertyFilter.filterOutAllExcept(FIELD_NAME_ID));
        filterProvider.addFilter(FILTER_NAME_ANNOTATION, SimpleBeanPropertyFilter.filterOutAllExcept(FIELD_NAME_ID));
        return filterProvider;
    }

    private SimpleFilterProvider createSimpleFilterProvider(final List<String> baseFilters,
                                                            final String expand) {
        final SimpleFilterProvider filterProvider = createBaseFilterProvider();
        filterProvider.setFailOnUnknownId(false);
        for (final String baseFilter : baseFilters) {
            filterProvider.removeFilter(baseFilter.trim());
        }
        if (expand != null && !expand.isEmpty()) {
            final String[] filterOptions = expand.split(",");
            for (final String filter : filterOptions) {
                filterProvider.removeFilter(filter.trim());
            }
        }
        return filterProvider;
    }

    List<String> parseAndDecodeUrisFromCsl(final String urisCsl) {
        if (urisCsl != null) {
            final String uriDecoded = urlDecodeString(urisCsl);
            final List<String> uris = new ArrayList<>();
            if (urisCsl != null) {
                for (final String uri : uriDecoded.split(",")) {
                    uris.add(uri.toLowerCase().trim());
                }
            }
            return uris;
        }
        return null;
    }

    List<String> parseStatusCsl(final String statusCsl) {
        final List<String> statuses = new ArrayList<>();
        if (statusCsl != null) {
            for (final String s : statusCsl.split(",")) {
                try {
                    final Status status = Status.valueOf(s.toUpperCase().trim());
                    statuses.add(status.toString());
                } catch (final Exception e) {
                    throw new YtiCodeListException(new ErrorModel(HttpStatus.BAD_REQUEST.value(), "Status code is not valid: " + s));
                }
            }
        }
        return statuses;
    }

    List<String> parseStatusList(final List<String> statusList) {
        final List<String> statuses = new ArrayList<>();
        if (statusList != null) {
            statusList.forEach(s -> {
                try {
                    final Status status = Status.valueOf(s.toUpperCase().trim());
                    statuses.add(status.toString());
                } catch (final Exception e) {
                    throw new YtiCodeListException(new ErrorModel(HttpStatus.BAD_REQUEST.value(), "Status code is not valid: " + s));
                }
            });
        }
        return statuses;
    }

    List<String> parseInfoDomainsCls(final String infoDomainCsl) {
        final List<String> infoDomains = new ArrayList<>();
        if (infoDomainCsl != null) {
            for (final String s : infoDomainCsl.split(",")) {
                infoDomains.add(s.toUpperCase().trim());
            }
        }
        return infoDomains;
    }

    private String createDownloadFilename(final String format,
                                          final String filename) {
        if (FORMAT_EXCEL.equalsIgnoreCase(format) || FORMAT_EXCEL_XLSX.equalsIgnoreCase(format)) {
            return filename + "." + FORMAT_EXCEL_XLSX;
        } else if (FORMAT_EXCEL_XLS.equalsIgnoreCase(format)) {
            return filename + "." + FORMAT_EXCEL_XLS;
        } else {
            return filename + "." + FORMAT_CSV;
        }
    }

    Response streamCsvCodesOutput(final String csv) {
        return streamCsvOutput(csv, DOWNLOAD_FILENAME_CODES);
    }

    Response streamCsvCodeSchemeOutput(final String csv,
                                       final String filename) {
        return streamCsvOutput(csv, filename);
    }

    Response streamCsvCodeSchemesOutput(final String csv) {
        return streamCsvOutput(csv, DOWNLOAD_FILENAME_CODESCHEMES);
    }

    Response streamCsvCodeRegistriesOutput(final String csv) {
        return streamCsvOutput(csv, DOWNLOAD_FILENAME_CODEREGISTRIES);
    }

    Response streamCsvExternalReferencesOutput(final String csv) {
        return streamCsvOutput(csv, DOWNLOAD_FILENAME_EXTERNALREFERENCES);
    }

    Response streamCsvPropertyTypesOutput(final String csv) {
        return streamCsvOutput(csv, DOWNLOAD_FILENAME_PROPERTYTYPES);
    }

    Response streamCsvValueTypesOutput(final String csv) {
        return streamCsvOutput(csv, DOWNLOAD_FILENAME_VALUETYPES);
    }

    Response streamCsvExtensionsOutput(final String csv) {
        return streamCsvOutput(csv, DOWNLOAD_FILENAME_EXTENSIONS);
    }

    Response streamCsvMembersOutput(final String csv) {
        return streamCsvOutput(csv, DOWNLOAD_FILENAME_MEMBERS);
    }

    Response streamCsvCrossReferenceListOutput(final String csv) {
        return streamCsvOutput(csv, DOWNLOAD_FILENAME_CROSS_REFERENCE_LIST);
    }

    private Response streamCsvOutput(final String csv,
                                     final String filename) {
        final StreamingOutput stream = output -> {
            try {
                output.write(csv.getBytes(StandardCharsets.UTF_8));
            } catch (final Exception e) {
                LOG.error("CSV output generation issue.", e);
                throw new YtiCodeListException(new ErrorModel(HttpStatus.INTERNAL_SERVER_ERROR.value(), "CSV output generation failed!"));
            }
        };
        return Response.ok(stream, "text/csv").header(HEADER_CONTENT_DISPOSITION, "attachment; filename = " + createDownloadFilename(FORMAT_CSV, filename)).build();
    }

    Response streamExcelCodesOutput(final Workbook workbook) {
        return streamExcelOutput(workbook, DOWNLOAD_FILENAME_CODES);
    }

    Response streamExcelCodeSchemeOutput(final Workbook workbook,
                                         final String filename) {
        return streamExcelOutput(workbook, filename);
    }

    Response streamExcelCodeSchemesOutput(final Workbook workbook) {
        return streamExcelOutput(workbook, DOWNLOAD_FILENAME_CODESCHEMES);
    }

    Response streamExcelCodeRegistriesOutput(final Workbook workbook) {
        return streamExcelOutput(workbook, DOWNLOAD_FILENAME_CODEREGISTRIES);
    }

    Response streamExcelExternalReferencesOutput(final Workbook workbook) {
        return streamExcelOutput(workbook, DOWNLOAD_FILENAME_EXTERNALREFERENCES);
    }

    Response streamExcelPropertyTypesOutput(final Workbook workbook) {
        return streamExcelOutput(workbook, DOWNLOAD_FILENAME_PROPERTYTYPES);
    }

    Response streamExcelValueTypesOutput(final Workbook workbook) {
        return streamExcelOutput(workbook, DOWNLOAD_FILENAME_VALUETYPES);
    }

    Response streamExcelExtensionsOutput(final Workbook workbook) {
        return streamExcelOutput(workbook, DOWNLOAD_FILENAME_EXTENSIONS);
    }

    Response streamExcelCrossReferenceListOutput(final Workbook workbook) {
        return streamExcelOutput(workbook, DOWNLOAD_FILENAME_CROSS_REFERENCE_LIST);
    }

    Response streamExcelMembersOutput(final Workbook workbook) {
        return streamExcelOutput(workbook, DOWNLOAD_FILENAME_MEMBERS);
    }

    private Response streamExcelOutput(final Workbook workbook,
                                       final String filename) {
        final StreamingOutput stream = output -> {
            try {
                workbook.write(output);
            } catch (final Exception e) {
                LOG.error("Excel output generation issue.", e);
                throw new YtiCodeListException(new ErrorModel(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Excel output generation failed!"));
            }
        };
        return Response.ok(stream, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet").header(HEADER_CONTENT_DISPOSITION, "attachment; filename = " + createDownloadFilename(FORMAT_EXCEL, filename)).build();
    }

    void ensureUriHost(final String host) {
        String scheme = "https?://";
        String uriNoScheme = host.replaceFirst(scheme, "");
        if (!uriNoScheme.startsWith(uriProperties.getHost() + uriProperties.getContextPath())) {
            LOG.error("This URI is not resolvable as a codelist resource, wrong host.");
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), "This URI is not resolvable as a codelist resource."));
        }
    }

    URI parseUriFromString(final String uriString) {
        if (!uriString.isEmpty()) {
            return URI.create(uriString.replace(" ", "%20"));
        } else {
            LOG.error("URI string was not valid!");
            throw new YtiCodeListException(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), "URI string was not valid!"));
        }
    }

    static class FilterModifier extends ObjectWriterModifier {

        private final FilterProvider provider;
        private final boolean pretty;

        FilterModifier(final FilterProvider provider,
                       final String pretty) {
            this.provider = provider;
            this.pretty = pretty != null;
        }

        @Override
        public ObjectWriter modify(final EndpointConfigBase<?> endpoint,
                                   final MultivaluedMap<String, Object> responseHeaders,
                                   final Object valueToWrite,
                                   final ObjectWriter writer,
                                   final JsonGenerator jsonGenerator) {
            if (pretty) {
                jsonGenerator.useDefaultPrettyPrinter();
            }
            return writer.with(provider);
        }
    }

    public static Date parseDateFromString(final String dateString) {
        if (dateString != null) {
            final ISO8601DateFormat dateFormat = new ISO8601DateFormat();
            try {
                return dateFormat.parse(dateString);
            } catch (ParseException e) {
                LOG.error("Parsing date from string failed: " + dateString);
                throw new YtiCodeListException(new ErrorModel(HttpStatus.BAD_REQUEST.value(), "Date input not valid: " + HtmlUtils.htmlEscape(dateString)));
            }
        }
        return null;
    }
}
