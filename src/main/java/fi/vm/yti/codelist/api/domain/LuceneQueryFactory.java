package fi.vm.yti.codelist.api.domain;

import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.inject.Singleton;
import javax.ws.rs.BadRequestException;

import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.standard.StandardQueryParser;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

@Singleton
@Service
public class LuceneQueryFactory {

    private static final String REGEX_PLAIN_QUERY_PATTERN_STRING = "^(?:(?!(?:\\s++|^)(?:AND|OR|TO)(?:\\s|$))(?:\\w++|\\s++|(?<=\\w)-++))+$";
    private static final String REGEX_ASTERISK_QUERY_PATTERN_STRING = "^(?:(?!(?:\\s++|^)(?:AND|OR|TO)(?:\\s|$))(?:\\w++|\\s++|(?<=[\\w*])-++|(?<!\\*)\\*(?=[\\w-])|(?<=[\\w-])\\*(?!\\*)))+$";
    private static final Logger LOG = LoggerFactory.getLogger(LuceneQueryFactory.class);

    private final Pattern plainQueryPattern = Pattern.compile(REGEX_PLAIN_QUERY_PATTERN_STRING, Pattern.UNICODE_CHARACTER_CLASS);
    private final Pattern plainSplitter = Pattern.compile("\\s+", Pattern.UNICODE_CHARACTER_CLASS);
    private final Pattern givenQueryPattern;

    @Autowired
    public LuceneQueryFactory() {
        givenQueryPattern = Pattern.compile(REGEX_ASTERISK_QUERY_PATTERN_STRING, Pattern.UNICODE_CHARACTER_CLASS);
    }

    QueryStringQueryBuilder buildPrefixSuffixQuery(final String searchTerm) {
        if (searchTerm != null) {
            final String trimmed = searchTerm.trim().toLowerCase();
            if (!searchTerm.isEmpty()) {
                String parsedQuery = null;
                if (plainQueryPattern.matcher(trimmed).matches()) {
                    final String[] splitQuery = plainSplitter.split(trimmed);
                    if (splitQuery.length == 1) {
                        parsedQuery = trimmed + " OR " + trimmed + "* OR *" + trimmed;
                    } else if (splitQuery.length > 1) {
                        parsedQuery = Arrays.stream(splitQuery).map(q -> "(" + q + " OR " + q + "* OR *" + q + ")").collect(Collectors.joining(" AND "));
                    }
                } else if (givenQueryPattern.matcher(trimmed).matches()) {
                    parsedQuery = trimmed;
                }
                if (parsedQuery != null) {
                    final StandardQueryParser parser = new StandardQueryParser();
                    try {
                        parser.setAllowLeadingWildcard(true);
                        return QueryBuilders.queryStringQuery(parser.parse(parsedQuery, "").toString());
                    } catch (final QueryNodeException e) {
                        LOG.error("ElasticSearch prefix / suffix query failed.", e);
                    }
                }
            }
        }
        LOG.debug("ElasticSearch prefix / suffix query string disqualified: '" + HtmlUtils.htmlEscape(searchTerm) + "'");
        throw new BadRequestException("Invalid query");
    }
}
