package io.openepcis.snippets.service;

import io.openepcis.snippets.constants.Constants;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.opensearch.client.opensearch._types.query_dsl.*;

import java.util.Set;

/**
 * Service for building OpenSearch queries.
 * This service provides methods to build various types of queries used in the application.
 */
@ApplicationScoped
public class QueryBuilderService {

    @Inject
    SynonymService synonymService;

    /**
     * Build a search query based on the search text.
     *
     * @param searchText The text to search for
     * @return A Query object with the configured query
     */
    public Query buildSearchQuery(String searchText) {
        if (searchText != null && !searchText.trim().isEmpty()) {

            // Create a bool query to combine multiple search conditions
            BoolQuery.Builder boolQuery = new BoolQuery.Builder();

            // Add a multi-match query for the original search text
            addMultiMatchQuery(searchText, boolQuery);
            
            // Add wildcard queries for the original search text
            addWildcardQueries(searchText, boolQuery);
            
            // Add synonym queries
            addSynonymQueries(searchText, boolQuery);

            // Set minimum should match to ensure at least one condition is met
            boolQuery.minimumShouldMatch(Constants.MINIMUM_SHOULD_MATCH);

            return Query.of(q -> q.bool(boolQuery.build()));
        } else {
            // If no search text, return all documents
            return Query.of(q -> q.matchAll(MatchAllQuery.of(m -> m)));
        }
    }

    /**
     * Build a query to find a snippet by its ID.
     *
     * @param id The ID to search for
     * @return A Query object with the configured query
     */
    public Query buildIdQuery(String id) {
        return Query.of(q -> q.term(t -> t
                .field(Constants.ID_KEYWORD)
                .value(v -> v.stringValue(id))
        ));
    }

    /**
     * Add a multi-match query to the bool query.
     *
     * @param text The text to search for
     * @param boolQuery The bool query to add to
     */
    private void addMultiMatchQuery(String text, BoolQuery.Builder boolQuery) {
        MultiMatchQuery multiMatch = MultiMatchQuery.of(m -> m
                .query(text)
                .fields(Constants.TITLE, Constants.DESCRIPTION)
                .fuzzyTranspositions(true)
                .fuzziness(Constants.FUZZINESS_LEVEL)
                .prefixLength(Constants.PREFIX_LENGTH));
        
        boolQuery.should(Query.of(q -> q.multiMatch(multiMatch)));
    }

    /**
     * Add wildcard queries to the bool query.
     *
     * @param text The text to search for
     * @param boolQuery The bool query to add to
     */
    private void addWildcardQueries(String text, BoolQuery.Builder boolQuery) {
        String wildcardPattern = "*" + text.toLowerCase() + "*";
        
        // Add a wildcard query for the title
        WildcardQuery titleWildcard = WildcardQuery.of(w -> w
                .field(Constants.TITLE)
                .wildcard(wildcardPattern));
        boolQuery.should(Query.of(q -> q.wildcard(titleWildcard)));
        
        // Add a wildcard query for description
        WildcardQuery descWildcard = WildcardQuery.of(w -> w
                .field(Constants.DESCRIPTION)
                .wildcard(wildcardPattern));
        boolQuery.should(Query.of(q -> q.wildcard(descWildcard)));
    }

    /**
     * Add synonym queries to the bool query.
     *
     * @param searchText The original search text
     * @param boolQuery The bool query to add to
     */
    private void addSynonymQueries(String searchText, BoolQuery.Builder boolQuery) {
        // Get synonyms for the search text
        Set<String> synonyms = synonymService.getSynonyms(searchText);
        
        // Add queries for each synonym
        for (String synonym : synonyms) {
            // Skip the original search text as it's already been added
            if (!synonym.equals(searchText.toLowerCase().trim())) {
                addMultiMatchQuery(synonym, boolQuery);
                addWildcardQueries(synonym, boolQuery);
            }
        }
    }
}