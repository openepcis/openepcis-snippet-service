package io.openepcis.snippets.repository;

import org.opensearch.client.opensearch.OpenSearchClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.jboss.logging.Logger;
import org.opensearch.client.opensearch.core.DeleteRequest;
import org.opensearch.client.opensearch.core.IndexRequest;
import org.opensearch.client.opensearch.core.IndexResponse;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.search.Hit;
import org.opensearch.client.opensearch.indices.CreateIndexRequest;
import org.opensearch.client.opensearch.indices.ExistsRequest;
import org.opensearch.client.opensearch._types.mapping.Property;
import org.opensearch.client.opensearch._types.query_dsl.BoolQuery;
import org.opensearch.client.opensearch._types.query_dsl.MatchAllQuery;
import org.opensearch.client.opensearch._types.query_dsl.MultiMatchQuery;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch._types.query_dsl.WildcardQuery;
import org.opensearch.client.opensearch._types.SortOrder;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.openepcis.snippets.model.Snippet;

/**
 * Repository for Snippet entities.
 * Handles business logic and prepares queries for OpenSearch.
 */
@ApplicationScoped
public class SnippetRepository {

    private static final Logger LOG = Logger.getLogger(SnippetRepository.class);
    private static final String INDEX_NAME = "snippets";
    private static final int DEFAULT_LIMIT = 10;

    @Inject
    OpenSearchClient client;

    @Inject
    ObjectMapper objectMapper;

    /**
     * Delete a snippet by its ID
     * 
     * @param id The ID of the snippet to delete
     * @throws IOException if there is an error deleting the snippet
     */
    public void delete(String id) throws IOException {
        // First find the document ID using the $id field
        SearchRequest searchRequest = new SearchRequest.Builder()
            .index(INDEX_NAME)
            .query(q -> q
                .term(t -> t
                    .field("$id")
                    .value(v -> v.stringValue(id))
                )
            )
            .size(1)
            .build();

        // Execute the search to get the document ID
        SearchResponse<Snippet> response = client.search(searchRequest, Snippet.class);
        if (response.hits().total().value() == 0) {
            throw new IOException("Snippet with $id '" + id + "' not found");
        }

        String documentId = response.hits().hits().get(0).id();
        
        // Create the delete request using the found document ID
        DeleteRequest request = new DeleteRequest.Builder()
            .index(INDEX_NAME)
            .id(documentId)
            .build();
        
        // Execute the delete request
        client.delete(request);
        LOG.debug("Deleted snippet with $id: " + id + " and document ID: " + documentId);
    }

    /**
     * Initialize the repository by creating the index if it doesn't exist
     * This method is called automatically when the bean is constructed
     */
    @PostConstruct
    void initializeOnStartup() {
        try {
            boolean exists = client.indices().exists(new ExistsRequest.Builder().index(INDEX_NAME).build()).value();
            if (!exists) {
                createSnippetsIndex();
                LOG.info("Created OpenSearch index: " + INDEX_NAME);
            } else {
                LOG.info("OpenSearch index already exists: " + INDEX_NAME);
            }
        } catch (IOException e) {
            LOG.error("Failed to initialize OpenSearch index", e);
        }
    }

    /**
     * Create the snippets index with appropriate mappings
     */
    private void createSnippetsIndex() throws IOException {
        // Create mapping properties for the index
        Map<String, Property> properties = Map.of(
                "$id", Property.of(p -> p.keyword(k -> k)),
                "title", Property.of(p -> p.text(t -> t.boost(2.0))),
                "description", Property.of(p -> p.text(t -> t)),
                "source", Property.of(p -> p.keyword(k -> k.index(false))),
                "createdAt", Property.of(p -> p.date(d -> d)));

        // Create the index with settings and mappings
        CreateIndexRequest request = new CreateIndexRequest.Builder()
                .index(INDEX_NAME)
                .settings(s -> s
                        .numberOfShards("1")
                        .numberOfReplicas("0"))
                .mappings(m -> m.properties(properties))
                .build();

        client.indices().create(request);
    }

    /**
     * Save a snippet to the repository
     * 
     * @param snippet    The snippet to save
     * @param sourceJson The original JSON source
     * @throws IOException if there is an error communicating with OpenSearch
     */
    public void save(Snippet snippet, String sourceJson) throws IOException {
        // Store the entire JSON in the source field
        snippet.setSource(sourceJson);

        // Create the index request
        IndexRequest<Snippet> request = new IndexRequest.Builder<Snippet>()
                .index(INDEX_NAME)
                .document(snippet)
                .build();

        // Execute the index request
        IndexResponse response = client.index(request);
        LOG.debug("Indexed snippet with ID: " + response.id());
    }

    /**
     * Search for snippets
     * 
     * @param searchText The text to search for (optional)
     * @param limit      The maximum number of results to return
     * @return A list of matching snippets
     * @throws IOException if there is an error communicating with OpenSearch
     */
    public List<Snippet> search(String searchText, int limit) throws IOException {
        // Build the search query
        Query query = buildSearchQuery(searchText);

        // Create the search request
        SearchRequest request = new SearchRequest.Builder()
                .index(INDEX_NAME)
                .query(query)
                .sort(s -> s.field(f -> f.field("createdAt").order(SortOrder.Desc)))
                .size(limit > 0 ? limit : DEFAULT_LIMIT)
                .build();

        try {
            // Execute the search request
            SearchResponse<Snippet> response = client.search(request, Snippet.class);
            return convertSearchHits(response.hits().hits(), false);
        } catch (IOException e) {
            LOG.error("Error searching snippets", e);
            throw e;
        }
    }

    /**
     * Check if a snippet with the given $id exists
     * 
     * @param id The $id to check
     * @return true if a snippet with the given $id exists, false otherwise
     * @throws IOException if there is an error communicating with OpenSearch
     */
    public boolean existsById(String id) throws IOException {
        if (id == null || id.trim().isEmpty()) {
            return false;
        }

        // Create a term query to find the exact $id
        Query query = Query.of(q -> q.term(t -> t.field("$id.keyword").value(v -> v.stringValue(id))));

        // Create the search request
        SearchRequest request = new SearchRequest.Builder()
                .index(INDEX_NAME)
                .query(query)
                .size(0) // We only need to know if it exists, not the actual document
                .build();

        try {
            // Execute the search request
            SearchResponse<Snippet> response = client.search(request, Snippet.class);
            return response.hits().total().value() > 0;
        } catch (IOException e) {
            LOG.error("Error checking if snippet exists by $id: " + id, e);
            throw e;
        }
    }

    /**
     * Build a search query based on the search text
     * 
     * @param searchText The text to search for
     * @return A Query object with the configured query
     */
    private Query buildSearchQuery(String searchText) {
        if (searchText != null && !searchText.trim().isEmpty()) {
            // Create a bool query to combine multiple search conditions
            BoolQuery.Builder boolQuery = new BoolQuery.Builder();

            // Add a multi-match query for full-text search in title and description
            // Title has a higher boost value (2.0) as specified in the mapping
            MultiMatchQuery multiMatch = MultiMatchQuery.of(m -> m
                    .query(searchText)
                    .fields("title", "description")
                    .fuzzyTranspositions(true)
                    .prefixLength(2));
            boolQuery.should(Query.of(q -> q.multiMatch(multiMatch)));

            // Add wildcard queries to match partial terms
            WildcardQuery titleWildcard = WildcardQuery.of(w -> w
                    .field("title")
                    .wildcard("*" + searchText.toLowerCase() + "*"));
            boolQuery.should(Query.of(q -> q.wildcard(titleWildcard)));

            WildcardQuery descWildcard = WildcardQuery.of(w -> w
                    .field("description")
                    .wildcard("*" + searchText.toLowerCase() + "*"));
            boolQuery.should(Query.of(q -> q.wildcard(descWildcard)));

            // Set minimum should match to 1 to ensure at least one condition is met
            boolQuery.minimumShouldMatch("1");

            return Query.of(q -> q.bool(boolQuery.build()));
        } else {
            // If no search text, return all documents
            return Query.of(q -> q.matchAll(MatchAllQuery.of(m -> m)));
        }
    }

    /**
     * Convert search hits to Snippet objects
     * 
     * @param hits          The search hits to convert
     * @param includeSource Whether to include the source field in the returned
     *                      snippets
     * @return A list of Snippet objects
     */
    private List<Snippet> convertSearchHits(List<Hit<Snippet>> hits, boolean includeSource) {
        if (hits.isEmpty()) {
            return Collections.emptyList();
        }

        List<Snippet> snippets = new ArrayList<>(hits.size());
        for (Hit<Snippet> hit : hits) {
            try {
                Snippet snippet = hit.source();

                // If we don't want to include the source, create a copy without it
                if (!includeSource && snippet != null) {
                    snippet = snippet.withoutSource();
                }

                if (snippet != null) {
                    snippets.add(snippet);
                }
            } catch (Exception e) {
                LOG.error("Error converting search hit to Snippet", e);
            }
        }

        return snippets;
    }
}
