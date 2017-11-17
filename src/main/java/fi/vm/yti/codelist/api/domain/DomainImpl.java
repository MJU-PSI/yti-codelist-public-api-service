package fi.vm.yti.codelist.api.domain;

import java.io.IOException;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Singleton;

import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MultiMatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.ISO8601DateFormat;

import fi.vm.yti.codelist.common.model.Code;
import fi.vm.yti.codelist.common.model.CodeRegistry;
import fi.vm.yti.codelist.common.model.CodeScheme;
import fi.vm.yti.codelist.common.model.ExternalReference;
import fi.vm.yti.codelist.common.model.Meta;
import fi.vm.yti.codelist.common.model.PropertyType;
import static fi.vm.yti.codelist.common.constants.ApiConstants.*;
import static java.lang.Math.toIntExact;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;

@Singleton
@Service
public class DomainImpl implements Domain {

    private static final Logger LOG = LoggerFactory.getLogger(DomainImpl.class);
    private static int MAX_SIZE = 10000;
    private Client client;

    @Autowired
    private DomainImpl(final Client client) {
        this.client = client;
    }

    public CodeRegistry getCodeRegistry(final String codeRegistryCodeValue) {
        final boolean exists = client.admin().indices().prepareExists(ELASTIC_INDEX_CODEREGISTRY).execute().actionGet().isExists();
        if (exists) {
            final ObjectMapper mapper = new ObjectMapper();
            final SearchRequestBuilder searchRequest = client
                .prepareSearch(ELASTIC_INDEX_CODEREGISTRY)
                .setTypes(ELASTIC_TYPE_CODEREGISTRY)
                .addSort("codeValue.keyword", SortOrder.ASC);
            final BoolQueryBuilder builder = boolQuery()
                .should(QueryBuilders.matchQuery("id", codeRegistryCodeValue.toLowerCase()))
                .should(QueryBuilders.matchQuery("codeValue", codeRegistryCodeValue.toLowerCase()))
                .minimumShouldMatch(1);
            searchRequest.setQuery(builder);
            final SearchResponse response = searchRequest.execute().actionGet();
            if (response.getHits().getTotalHits() > 0) {
                final SearchHit hit = response.getHits().getAt(0);
                try {
                    if (hit != null) {
                        return mapper.readValue(hit.getSourceAsString(), CodeRegistry.class);
                    }
                } catch (IOException e) {
                    LOG.error("getCodeRegistry reading value from JSON string failed: " + hit.getSourceAsString() + ", message: " + e.getMessage());
                }
            }
        }
        return null;
    }

    public Set<CodeRegistry> getCodeRegistries() {
        return getCodeRegistries(MAX_SIZE, 0, null, null, null, null);
    }

    public Set<CodeRegistry> getCodeRegistries(final Integer pageSize,
                                               final Integer from,
                                               final String codeRegistryCodeValue,
                                               final String codeRegistryPrefLabel,
                                               final Date after,
                                               final Meta meta) {
        final Set<CodeRegistry> codeRegistries = new LinkedHashSet<>();
        final boolean exists = client.admin().indices().prepareExists(ELASTIC_INDEX_CODEREGISTRY).execute().actionGet().isExists();
        if (exists) {
            final ObjectMapper mapper = new ObjectMapper();
            final SearchRequestBuilder searchRequest = client
                .prepareSearch(ELASTIC_INDEX_CODEREGISTRY)
                .setTypes(ELASTIC_TYPE_CODEREGISTRY)
                .addSort("codeValue.keyword", SortOrder.ASC)
                .setSize(pageSize != null ? pageSize : MAX_SIZE)
                .setFrom(from != null ? from : 0);
            final BoolQueryBuilder builder = constructSearchQuery(codeRegistryCodeValue, codeRegistryPrefLabel, after);
            searchRequest.setQuery(builder);
            final SearchResponse response = searchRequest.execute().actionGet();
            setResultCounts(meta, response);
            response.getHits().forEach(hit -> {
                try {
                    codeRegistries.add(mapper.readValue(hit.getSourceAsString(), CodeRegistry.class));
                } catch (IOException e) {
                    LOG.error("getCodeRegistries reading value from JSON string failed: " + hit.getSourceAsString() + ", message: " + e.getMessage());
                }
            });
        }
        return codeRegistries;
    }

    public CodeScheme getCodeScheme(final String codeRegistryCodeValue,
                                    final String codeSchemeCodeValue) {
        final boolean exists = client.admin().indices().prepareExists(ELASTIC_INDEX_CODESCHEME).execute().actionGet().isExists();
        if (exists) {
            final ObjectMapper mapper = new ObjectMapper();
            final SearchRequestBuilder searchRequest = client
                .prepareSearch(ELASTIC_INDEX_CODESCHEME)
                .setTypes(ELASTIC_TYPE_CODESCHEME)
                .addSort("codeValue.keyword", SortOrder.ASC);
            final BoolQueryBuilder builder = boolQuery()
                .should(QueryBuilders.matchQuery("id", codeSchemeCodeValue.toLowerCase()))
                .should(QueryBuilders.matchQuery("codeValue", codeSchemeCodeValue.toLowerCase()))
                .minimumShouldMatch(1);
            builder.must(QueryBuilders.matchQuery("codeRegistry.codeValue", codeRegistryCodeValue.toLowerCase()));
            searchRequest.setQuery(builder);
            final SearchResponse response = searchRequest.execute().actionGet();
            if (response.getHits().getTotalHits() > 0) {
                final SearchHit hit = response.getHits().getAt(0);
                try {
                    if (hit != null) {
                        return mapper.readValue(hit.getSourceAsString(), CodeScheme.class);
                    }
                } catch (IOException e) {
                    LOG.error("getCodeScheme reading value from JSON string failed: " + hit.getSourceAsString() + ", message: " + e.getMessage());
                }
            }
        }
        return null;
    }

    public Set<CodeScheme> getCodeSchemes() {
        return getCodeSchemes(MAX_SIZE, 0, null, null, null, null, null, null, null, null);
    }

    public Set<CodeScheme> getCodeSchemes(final Integer pageSize,
                                          final Integer from,
                                          final String codeRegistryCodeValue,
                                          final String codeRegistryPrefLabel,
                                          final String codeSchemeCodeValue,
                                          final String codeSchemePrefLabel,
                                          final List<String> statuses,
                                          final List<String> dataClassifications,
                                          final Date after,
                                          final Meta meta) {
        final Set<CodeScheme> codeSchemes = new LinkedHashSet<>();
        final boolean exists = client.admin().indices().prepareExists(ELASTIC_INDEX_CODESCHEME).execute().actionGet().isExists();
        if (exists) {
            final ObjectMapper mapper = new ObjectMapper();
            final SearchRequestBuilder searchRequest = client
                .prepareSearch(ELASTIC_INDEX_CODESCHEME)
                .setTypes(ELASTIC_TYPE_CODESCHEME)
                .addSort("codeValue.keyword", SortOrder.ASC)
                .setSize(pageSize != null ? pageSize : MAX_SIZE)
                .setFrom(from != null ? from : 0);
            final BoolQueryBuilder builder = constructSearchQuery(codeSchemeCodeValue, codeSchemePrefLabel, after);
            if (codeRegistryCodeValue != null) {
                builder.must(QueryBuilders.matchQuery("codeRegistry.codeValue", codeRegistryCodeValue.toLowerCase()));
            }
            if (codeRegistryPrefLabel != null) {
                builder.must(QueryBuilders.nestedQuery("codeRegistry.prefLabels", QueryBuilders.multiMatchQuery(codeRegistryPrefLabel.toLowerCase() + "*", "prefLabels.*").type(MultiMatchQueryBuilder.Type.PHRASE_PREFIX), ScoreMode.None));
            }
            if (dataClassifications != null && !dataClassifications.isEmpty()) {
                builder.must(QueryBuilders.nestedQuery("dataClassifications", QueryBuilders.termsQuery("dataClassifications.codeValue.keyword", dataClassifications), ScoreMode.None));
            }
            if (statuses != null && !statuses.isEmpty()) {
                builder.must(QueryBuilders.termsQuery("status.keyword", statuses));
            }
            searchRequest.setQuery(builder);
            final SearchResponse response = searchRequest.execute().actionGet();
            setResultCounts(meta, response);
            response.getHits().forEach(hit -> {
                try {
                    codeSchemes.add(mapper.readValue(hit.getSourceAsString(), CodeScheme.class));
                } catch (IOException e) {
                    LOG.error("getCodeSchemes reading value from JSON string failed: " + hit.getSourceAsString() + ", message: " + e.getMessage());
                }
            });
        }
        return codeSchemes;
    }

    public Code getCode(final String codeRegistryCodeValue,
                        final String codeSchemeCodeValue,
                        final String codeCodeValue) {
        final boolean exists = client.admin().indices().prepareExists(ELASTIC_INDEX_CODE).execute().actionGet().isExists();
        if (exists) {
            final ObjectMapper mapper = new ObjectMapper();
            final SearchRequestBuilder searchRequest = client
                .prepareSearch(ELASTIC_INDEX_CODE)
                .setTypes(ELASTIC_TYPE_CODE);
            final BoolQueryBuilder builder = boolQuery()
                .should(QueryBuilders.matchQuery("id", codeCodeValue.toLowerCase()))
                .should(QueryBuilders.matchQuery("codeValue", codeCodeValue.toLowerCase()))
                .minimumShouldMatch(1);
            builder.must(QueryBuilders.matchQuery("codeScheme.codeValue", codeSchemeCodeValue.toLowerCase()));
            builder.must(QueryBuilders.matchQuery("codeScheme.codeRegistry.codeValue", codeRegistryCodeValue.toLowerCase()));
            searchRequest.setQuery(builder);

            final SearchResponse response = searchRequest.execute().actionGet();
            LOG.info("getCode found: " + response.getHits().getTotalHits() + " hits.");
            if (response.getHits().getTotalHits() > 0) {
                final SearchHit hit = response.getHits().getAt(0);
                try {
                    if (hit != null) {
                        return mapper.readValue(hit.getSourceAsString(), Code.class);
                    }
                } catch (IOException e) {
                    LOG.error("getCode reading value from JSON string failed: " + hit.getSourceAsString() + ", message: " + e.getMessage());
                }
            }
            return null;
        } else {
            return null;
        }
    }

    public Set<Code> getCodes(final Integer pageSize,
                              final Integer from,
                              final String codeRegistryCodeValue,
                              final String codeSchemeCodeValue,
                              final String codeCodeValue,
                              final String prefLabel,
                              final List<String> statuses,
                              final Date after,
                              final Meta meta) {
        final boolean exists = client.admin().indices().prepareExists(ELASTIC_INDEX_CODE).execute().actionGet().isExists();
        if (exists) {
            final ObjectMapper mapper = new ObjectMapper();
            final Set<Code> codes = new LinkedHashSet<>();
            final SearchRequestBuilder searchRequest = client
                .prepareSearch(ELASTIC_INDEX_CODE)
                .setTypes(ELASTIC_TYPE_CODE)
                .addSort("codeValue.keyword", SortOrder.ASC)
                .setSize(pageSize != null ? pageSize : MAX_SIZE)
                .setFrom(from != null ? from : 0);

            final BoolQueryBuilder builder = constructSearchQuery(codeCodeValue, prefLabel, after);
            builder.must(QueryBuilders.matchQuery("codeScheme.codeValue", codeSchemeCodeValue.toLowerCase()));
            builder.must(QueryBuilders.matchQuery("codeScheme.codeRegistry.codeValue", codeRegistryCodeValue.toLowerCase()));
            searchRequest.setQuery(builder);
            if (!statuses.isEmpty()) {
                builder.must(QueryBuilders.termsQuery("status.keyword", statuses));
            }
            final SearchResponse response = searchRequest.execute().actionGet();
            setResultCounts(meta, response);
            response.getHits().forEach(hit -> {
                try {
                    codes.add(mapper.readValue(hit.getSourceAsString(), Code.class));
                } catch (IOException e) {
                    LOG.error("getCodes reading value from JSON string failed: " + hit.getSourceAsString() + ", message: " + e.getMessage());
                }
            });
            return codes;
        }
        return null;
    }

    public PropertyType getPropertyType(final String propertyTypeId) {
        final boolean exists = client.admin().indices().prepareExists(ELASTIC_INDEX_PROPERTYTYPE).execute().actionGet().isExists();
        if (exists) {
            final ObjectMapper mapper = new ObjectMapper();
            final SearchRequestBuilder searchRequest = client
                .prepareSearch(ELASTIC_INDEX_PROPERTYTYPE)
                .setTypes(ELASTIC_TYPE_PROPERTYTYPE);
            final BoolQueryBuilder builder = boolQuery()
                .should(QueryBuilders.matchQuery("id", propertyTypeId.toLowerCase()))
                .minimumShouldMatch(1);
            searchRequest.setQuery(builder);
            final SearchResponse response = searchRequest.execute().actionGet();
            if (response.getHits().getTotalHits() > 0) {
                final SearchHit hit = response.getHits().getAt(0);
                try {
                    if (hit != null) {
                        return mapper.readValue(hit.getSourceAsString(), PropertyType.class);
                    }
                } catch (IOException e) {
                    LOG.error("getPropertyType reading value from JSON string failed: " + hit.getSourceAsString() + ", message: " + e.getMessage());
                }
            }
        }
        return null;
    }

    public Set<PropertyType> getPropertyTypes() {
        return getPropertyTypes(MAX_SIZE, 0, null, null,null, null);
    }

    public Set<PropertyType> getPropertyTypes(final Integer pageSize,
                                              final Integer from,
                                              final String propertyTypePrefLabel,
                                              final String context,
                                              final Date after,
                                              final Meta meta) {
        final Set<PropertyType> propertyTypes = new LinkedHashSet<>();
        final boolean exists = client.admin().indices().prepareExists(ELASTIC_INDEX_PROPERTYTYPE).execute().actionGet().isExists();
        if (exists) {
            final ObjectMapper mapper = new ObjectMapper();
            final SearchRequestBuilder searchRequest = client
                .prepareSearch(ELASTIC_INDEX_PROPERTYTYPE)
                .setTypes(ELASTIC_TYPE_PROPERTYTYPE)
                .setSize(pageSize != null ? pageSize : MAX_SIZE)
                .setFrom(from != null ? from : 0);
            final BoolQueryBuilder builder = constructSearchQuery(null, propertyTypePrefLabel, after);
            if (context != null) {
                builder.must(QueryBuilders.prefixQuery("context", context.toLowerCase()));
            }
            searchRequest.setQuery(builder);
            final SearchResponse response = searchRequest.execute().actionGet();
            setResultCounts(meta, response);
            response.getHits().forEach(hit -> {
                try {
                    final PropertyType propertyType = mapper.readValue(hit.getSourceAsString(), PropertyType.class);
                    propertyTypes.add(propertyType);
                } catch (IOException e) {
                    LOG.error("getPropertyTypes reading value from JSON string failed: " + hit.getSourceAsString() + ", message: " + e.getMessage());
                }
            });
        }
        return propertyTypes;
    }

    public ExternalReference getExternalReference(final String externalReferenceId) {
        final boolean exists = client.admin().indices().prepareExists(ELASTIC_INDEX_EXTERNALREFERENCE).execute().actionGet().isExists();
        if (exists) {
            final ObjectMapper mapper = new ObjectMapper();
            final SearchRequestBuilder searchRequest = client
                .prepareSearch(ELASTIC_INDEX_EXTERNALREFERENCE)
                .setTypes(ELASTIC_TYPE_EXTERNALREFERENCE);
            final BoolQueryBuilder builder = boolQuery()
                .should(QueryBuilders.matchQuery("id", externalReferenceId.toLowerCase()))
                .minimumShouldMatch(1);
            searchRequest.setQuery(builder);
            final SearchResponse response = searchRequest.execute().actionGet();
            if (response.getHits().getTotalHits() > 0) {
                final SearchHit hit = response.getHits().getAt(0);
                try {
                    if (hit != null) {
                        final ExternalReference externalReference = mapper.readValue(hit.getSourceAsString(), ExternalReference.class);
                        return externalReference;
                    }
                } catch (IOException e) {
                    LOG.error("getExternalReference reading value from JSON string failed: " + hit.getSourceAsString() + ", message: " + e.getMessage());
                }
            }
        }
        return null;
    }

    public Set<ExternalReference> getExternalReferences() {
        return getExternalReferences(MAX_SIZE, 0, null, null, null);
    }

    public Set<ExternalReference> getExternalReferences(final Integer pageSize,
                                              final Integer from,
                                              final String codeRegistryPrefLabel,
                                              final Date after,
                                              final Meta meta) {
        final Set<ExternalReference> externalReferences = new LinkedHashSet<>();
        final boolean exists = client.admin().indices().prepareExists(ELASTIC_INDEX_EXTERNALREFERENCE).execute().actionGet().isExists();
        if (exists) {
            final ObjectMapper mapper = new ObjectMapper();
            final SearchRequestBuilder searchRequest = client
                .prepareSearch(ELASTIC_INDEX_EXTERNALREFERENCE)
                .setTypes(ELASTIC_TYPE_EXTERNALREFERENCE)
                .setSize(pageSize != null ? pageSize : MAX_SIZE)
                .setFrom(from != null ? from : 0);
            final BoolQueryBuilder builder = constructSearchQuery(null, codeRegistryPrefLabel, after);
            searchRequest.setQuery(builder);
            final SearchResponse response = searchRequest.execute().actionGet();
            setResultCounts(meta, response);
            response.getHits().forEach(hit -> {
                try {
                    final ExternalReference externalReference = mapper.readValue(hit.getSourceAsString(), ExternalReference.class);
                    externalReferences.add(externalReference);
                } catch (IOException e) {
                    LOG.error("getExternalReferences reading value from JSON string failed: " + hit.getSourceAsString() + ", message: " + e.getMessage());
                }
            });
        }
        return externalReferences;
    }
    private BoolQueryBuilder constructSearchQuery(final String codeValue,
                                                  final String prefLabel,
                                                  final Date after) {
        final BoolQueryBuilder builder = boolQuery();
        if (codeValue != null) {
            builder.must(QueryBuilders.prefixQuery("codeValue", codeValue.toLowerCase()));
        }
        if (prefLabel != null) {
            builder.must(QueryBuilders.nestedQuery("prefLabels", QueryBuilders.multiMatchQuery(prefLabel.toLowerCase() + "*", "prefLabels.*").type(MultiMatchQueryBuilder.Type.PHRASE_PREFIX), ScoreMode.None));
        }
        if (after != null) {
            final ISO8601DateFormat dateFormat = new ISO8601DateFormat();
            final String afterString = dateFormat.format(after);
            builder.must(QueryBuilders.rangeQuery("modified").gt(afterString));
        }
        return builder;
    }

    private void setResultCounts(final Meta meta,
                                 final SearchResponse response) {
        final Integer totalResults = toIntExact(response.getHits().getTotalHits());
        final Integer resultCount = toIntExact(response.getHits().internalHits().length);
        if (meta != null) {
            meta.setTotalResults(totalResults);
            meta.setResultCount(resultCount);
        }
        LOG.info("Search found: " + totalResults + " total hits.");
    }
}
