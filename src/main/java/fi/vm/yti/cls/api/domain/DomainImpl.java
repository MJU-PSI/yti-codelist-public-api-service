package fi.vm.yti.cls.api.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.ISO8601DateFormat;
import fi.vm.yti.cls.common.model.Code;
import fi.vm.yti.cls.common.model.CodeRegistry;
import fi.vm.yti.cls.common.model.CodeScheme;
import fi.vm.yti.cls.common.model.Meta;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MultiMatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.inject.Singleton;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Set;

import static java.lang.Math.toIntExact;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;

@Singleton
@Service
public class DomainImpl implements Domain {

    private static final Logger LOG = LoggerFactory.getLogger(DomainImpl.class);
    private Client client;
    private static int MAX_SIZE = 10000;

    @Autowired
    private DomainImpl(final Client client) {
        this.client = client;
    }

    public CodeRegistry getCodeRegistry(final String codeRegistryCodeValue,
                                        final Boolean useId) {
        final boolean exists = client.admin().indices().prepareExists(DomainConstants.ELASTIC_INDEX_CODEREGISTRIES).execute().actionGet().isExists();
        if (exists) {
            final ObjectMapper mapper = new ObjectMapper();
            final SearchRequestBuilder searchRequest = client
                    .prepareSearch(DomainConstants.ELASTIC_INDEX_CODEREGISTRIES)
                    .setTypes(DomainConstants.ELASTIC_TYPE_CODEREGISTRY)
                    .addSort("codeValue.keyword", SortOrder.ASC);
            final BoolQueryBuilder builder = boolQuery();
            if (useId) {
                builder.must(QueryBuilders.matchQuery("id.keyword", codeRegistryCodeValue.toLowerCase()));
            } else {
                builder.must(QueryBuilders.matchQuery("codeValue.keyword", codeRegistryCodeValue.toLowerCase()));
            }
            searchRequest.setQuery(builder);
            final SearchResponse response = searchRequest.execute().actionGet();
            if (response.getHits().getTotalHits() > 0) {
                final SearchHit hit = response.getHits().getAt(0);
                try {
                    if (hit != null) {
                        final CodeRegistry codeRegistry = mapper.readValue(hit.getSourceAsString(), CodeRegistry.class);
                        return codeRegistry;
                    }
                } catch (IOException e) {
                    LOG.error("getCodeRegistry reading value from JSON string failed: " + hit.getSourceAsString() + ", message: " + e.getMessage());
                }
            }
        }
        return null;
    }

    public Set<CodeRegistry> getCodeRegistries(final Integer pageSize,
                                                     final Integer from,
                                                     final String codeRegistryCodeValue,
                                                     final String codeRegistryPrefLabel,
                                                     final Date after,
                                                     final Meta meta) {
        final Set<CodeRegistry> codeRegistries = new LinkedHashSet<>();
        final boolean exists = client.admin().indices().prepareExists(DomainConstants.ELASTIC_INDEX_CODEREGISTRIES).execute().actionGet().isExists();
        if (exists) {
            final ObjectMapper mapper = new ObjectMapper();
            final SearchRequestBuilder searchRequest = client
                    .prepareSearch(DomainConstants.ELASTIC_INDEX_CODEREGISTRIES)
                    .setTypes(DomainConstants.ELASTIC_TYPE_CODEREGISTRY)
                    .addSort("codeValue.keyword", SortOrder.ASC)
                    .setSize(pageSize != null ? pageSize : MAX_SIZE)
                    .setFrom(from != null ? from : 0);
            final BoolQueryBuilder builder = constructSearchQuery(codeRegistryCodeValue, codeRegistryPrefLabel, after);
            searchRequest.setQuery(builder);
            final SearchResponse response = searchRequest.execute().actionGet();
            setResultCounts(meta, response);
            Arrays.stream(response.getHits().hits()).forEach(hit -> {
                try {
                    final CodeRegistry codeRegistry = mapper.readValue(hit.getSourceAsString(), CodeRegistry.class);
                    codeRegistries.add(codeRegistry);
                } catch (IOException e) {
                    LOG.error("getCodeRegistries reading value from JSON string failed: " + hit.getSourceAsString() + ", message: " + e.getMessage());
                }
            });
        }
        return codeRegistries;
    }

    public CodeScheme getCodeScheme(final String codeRegistryCodeValue,
                                    final String codeSchemeCodeValue,
                                    final Boolean useId) {
        final boolean exists = client.admin().indices().prepareExists(DomainConstants.ELASTIC_INDEX_CODESCHEMES).execute().actionGet().isExists();
        if (exists) {
            final ObjectMapper mapper = new ObjectMapper();
            final SearchRequestBuilder searchRequest = client
                    .prepareSearch(DomainConstants.ELASTIC_INDEX_CODESCHEMES)
                    .setTypes(DomainConstants.ELASTIC_TYPE_CODESCHEME)
                    .addSort("codeValue.keyword", SortOrder.ASC);
            final BoolQueryBuilder builder = boolQuery();
            if (useId) {
                builder.must(QueryBuilders.matchQuery("id.keyword", codeSchemeCodeValue.toLowerCase()));
            } else {
                builder.must(QueryBuilders.matchQuery("codeValue.keyword", codeSchemeCodeValue.toLowerCase()));
            }
            builder.must(QueryBuilders.matchQuery("codeRegistry.codeValue.keyword", codeRegistryCodeValue.toLowerCase()));
            searchRequest.setQuery(builder);
            final SearchResponse response = searchRequest.execute().actionGet();
            if (response.getHits().getTotalHits() > 0) {
                final SearchHit hit = response.getHits().getAt(0);
                try {
                    if (hit != null) {
                        final CodeScheme codeScheme = mapper.readValue(hit.getSourceAsString(), CodeScheme.class);
                        return codeScheme;
                    }
                } catch (IOException e) {
                    LOG.error("getCodeScheme reading value from JSON string failed: " + hit.getSourceAsString() + ", message: " + e.getMessage());
                }
            }
        }
        return null;
    }

    public Set<CodeScheme> getCodeSchemes(final Integer pageSize,
                                          final Integer from,
                                          final String codeRegistryCodeValue,
                                          final String codeSchemeCodeValue,
                                          final String codeSchemePrefLabel,
                                          final String codeSchemeType,
                                          final Date after,
                                          final Meta meta) {
        final Set<CodeScheme> codeSchemes = new LinkedHashSet<>();
        final boolean exists = client.admin().indices().prepareExists(DomainConstants.ELASTIC_INDEX_CODESCHEMES).execute().actionGet().isExists();
        if (exists) {
            final ObjectMapper mapper = new ObjectMapper();
            final SearchRequestBuilder searchRequest = client
                    .prepareSearch(DomainConstants.ELASTIC_INDEX_CODESCHEMES)
                    .setTypes(DomainConstants.ELASTIC_TYPE_CODESCHEME)
                    .addSort("codeValue.keyword", SortOrder.ASC)
                    .setSize(pageSize != null ? pageSize : MAX_SIZE)
                    .setFrom(from != null ? from : 0);
            final BoolQueryBuilder builder = constructSearchQuery(codeSchemeCodeValue, codeSchemePrefLabel, after);
            builder.must(QueryBuilders.matchQuery("codeRegistry.codeValue.keyword", codeRegistryCodeValue.toLowerCase()));
            searchRequest.setQuery(builder);
            final SearchResponse response = searchRequest.execute().actionGet();
            setResultCounts(meta, response);
            Arrays.stream(response.getHits().hits()).forEach(hit -> {
                try {
                    final CodeScheme codeScheme = mapper.readValue(hit.getSourceAsString(), CodeScheme.class);
                    codeSchemes.add(codeScheme);
                } catch (IOException e) {
                    LOG.error("getCodeSchemes reading value from JSON string failed: " + hit.getSourceAsString() + ", message: " + e.getMessage());
                }
            });
        }
        return codeSchemes;
    }

    public Code getCode(final String codeRegistryCodeValue,
                        final String codeSchemeCodeValue,
                        final String codeCodeValue,
                        final Boolean useId) {
        final boolean exists = client.admin().indices().prepareExists(DomainConstants.ELASTIC_INDEX_CODES).execute().actionGet().isExists();
        if (exists) {
            final ObjectMapper mapper = new ObjectMapper();
            final SearchRequestBuilder searchRequest = client
                    .prepareSearch(DomainConstants.ELASTIC_INDEX_CODES)
                    .setTypes(DomainConstants.ELASTIC_TYPE_CODE);

            final BoolQueryBuilder builder = boolQuery();
            if (useId) {
                builder.must(QueryBuilders.matchQuery("id.keyword", codeCodeValue.toLowerCase()));
            } else {
                builder.must(QueryBuilders.matchQuery("codeValue.keyword", codeCodeValue.toLowerCase()));
            }
            builder.must(QueryBuilders.matchQuery("codeScheme.codeValue.keyword", codeSchemeCodeValue.toLowerCase()));
            builder.must(QueryBuilders.matchQuery("codeScheme.codeRegistry.codeValue.keyword", codeRegistryCodeValue.toLowerCase()));
            searchRequest.setQuery(builder);

            final SearchResponse response = searchRequest.execute().actionGet();
            LOG.info("getCode found: " + response.getHits().getTotalHits() + " hits.");
            if (response.getHits().getTotalHits() > 0) {
                final SearchHit hit = response.getHits().getAt(0);
                try {
                    if (hit != null) {
                        final Code code = mapper.readValue(hit.getSourceAsString(), Code.class);
                        return code;
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
                              final Date after,
                              final Meta meta) {
        final boolean exists = client.admin().indices().prepareExists(DomainConstants.ELASTIC_INDEX_CODES).execute().actionGet().isExists();
        if (exists) {
            final ObjectMapper mapper = new ObjectMapper();
            final Set<Code> codes = new LinkedHashSet<>();
            final SearchRequestBuilder searchRequest = client
                    .prepareSearch(DomainConstants.ELASTIC_INDEX_CODES)
                    .setTypes(DomainConstants.ELASTIC_TYPE_CODE)
                    .addSort("codeValue.keyword", SortOrder.ASC)
                    .setSize(pageSize != null ? pageSize : MAX_SIZE)
                    .setFrom(from != null ? from : 0);

            final BoolQueryBuilder builder = constructSearchQuery(codeCodeValue, prefLabel, after);
            builder.must(QueryBuilders.matchQuery("codeScheme.codeValue.keyword", codeSchemeCodeValue.toLowerCase()));
            builder.must(QueryBuilders.matchQuery("codeScheme.codeRegistry.codeValue.keyword", codeRegistryCodeValue.toLowerCase()));
            searchRequest.setQuery(builder);

            final SearchResponse response = searchRequest.execute().actionGet();
            setResultCounts(meta, response);
            Arrays.stream(response.getHits().hits()).forEach(hit -> {
                try {
                    final Code code = mapper.readValue(hit.getSourceAsString(), Code.class);
                    codes.add(code);
                } catch (IOException e) {
                    LOG.error("getCodes reading value from JSON string failed: " + hit.getSourceAsString() + ", message: " + e.getMessage());
                }
            });
            return codes;
        }
        return null;
    }

    private BoolQueryBuilder constructSearchQuery(final String codeValue,
                                                  final String prefLabel,
                                                  final Date after) {
        final BoolQueryBuilder builder = boolQuery();
        if (codeValue != null) {
            builder.must(QueryBuilders.prefixQuery("codeValue.keyword", codeValue.toLowerCase()));
        }
        if (prefLabel != null) {
            builder.must(QueryBuilders.nestedQuery("prefLabels", QueryBuilders.multiMatchQuery(prefLabel.toLowerCase() + "*", "prefLabels.fi", "prefLabels.se", "prefLabels.en").type(MultiMatchQueryBuilder.Type.PHRASE_PREFIX), ScoreMode.None));
        }
        if (after != null) {
            final ISO8601DateFormat dateFormat = new ISO8601DateFormat();
            final String afterString = dateFormat.format(after);
            builder.must(boolQuery()
                    .should(QueryBuilders.rangeQuery("modified").gt(afterString))
                    .minimumShouldMatch(1));
        }
        return builder;
    }

    private void addDateFiltersToRequest(final SearchRequestBuilder searchRequest,
                                         final Date after) {
        if (after != null) {
            final ISO8601DateFormat dateFormat = new ISO8601DateFormat();
            final String afterString = dateFormat.format(after);
            final QueryBuilder qb = boolQuery()
                    .should(QueryBuilders.rangeQuery("modified").gt(afterString))
                    .minimumShouldMatch(1);
            searchRequest.setQuery(qb);
        }
    }

    private void setResultCounts(final Meta meta,
                                 final SearchResponse response) {
        final Integer totalResults = toIntExact(response.getHits().getTotalHits());
        meta.setTotalResults(totalResults);
        final Integer resultCount = toIntExact(response.getHits().hits().length);
        meta.setResultCount(resultCount);
        LOG.info("Search found: " + totalResults + " total hits.");
    }

}
