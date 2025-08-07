package io.openepcis.snippets.repository;

import io.openepcis.snippets.constants.Constants;
import io.openepcis.snippets.model.Snippet;
import io.openepcis.snippets.service.QueryBuilderService;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.stream.JsonParser;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.client.json.JsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.SortOrder;
import org.opensearch.client.opensearch._types.mapping.TypeMapping;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.core.*;
import org.opensearch.client.opensearch.core.search.Hit;
import org.opensearch.client.opensearch.indices.CreateIndexRequest;
import org.opensearch.client.opensearch.indices.ExistsRequest;
import org.opensearch.client.opensearch.indices.IndexSettings;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static io.openepcis.snippets.constants.Constants.*;

/**
 * Repository for Snippet entities.
 * Handles interactions with OpenSearch for CRUD operations on Snippet entities.
 */
@ApplicationScoped
@Slf4j
public class SnippetRepository {

    @Inject
    OpenSearchClient client;

    @Inject
    QueryBuilderService queryBuilder;

    /**
     * Initialize the repository by creating the index if it doesn't exist.
     * This method is called automatically when the bean is constructed.
     */
    @PostConstruct
    void initializeOnStartup() {
        try {
            // Check if the index exists
            boolean exists = client.indices().exists(
                    new ExistsRequest.Builder()
                            .index(Constants.INDEX_NAME)
                            .build()
            ).value();

            // Create the index if it doesn't exist
            if (!exists) {
                createSnippetsIndex();
                log.info("Created OpenSearch index: {}", Constants.INDEX_NAME);
            } else {
                log.info("OpenSearch index already exists: {}", Constants.INDEX_NAME);
            }
        } catch (IOException e) {
            log.error("Failed to initialize OpenSearch index", e);
        }
    }

    /**
     * Create the snippet index with appropriate settings and mappings from the template file.
     *
     * @throws IOException if there is an error creating the index
     */
    private void createSnippetsIndex() throws IOException {
        // Get the JsonpMapper from the client
        final JsonpMapper mapper = client._transport().jsonpMapper();

        // Read the template file from resources
        final String templateContent = new String(
                Objects.requireNonNull(
                        getClass().getClassLoader().getResourceAsStream(TEMPLATE_OPENEPCIS_SNIPPET_INDEX_TEMPLATE)
                ).readAllBytes()
        );

        // Parse the template JSON to extract both settings and mappings
        try (JsonReader jsonReader = Json.createReader(new StringReader(templateContent))) {

            final JsonObject templateObject = jsonReader.readObject().getJsonObject(TEMPLATE);

            // Extract settings JSON
            final JsonObject settingsJson = templateObject.getJsonObject(SETTINGS);

            // Extract mappings JSON
            final JsonObject mappingsJson = templateObject.getJsonObject(MAPPINGS);

            // Create a JsonParser to parse the mappings
            final JsonParser mappingsParser = mapper.jsonProvider()
                    .createParser(new StringReader(mappingsJson.toString()));

            // Create a JsonParser to parse the mappings
            final JsonParser settingParser = mapper.jsonProvider()
                    .createParser(new StringReader(settingsJson.toString()));

            // Deserialize the mappings into a TypeMapping object
            final TypeMapping mappings = TypeMapping._DESERIALIZER.deserialize(mappingsParser, mapper);

            final IndexSettings settings = IndexSettings._DESERIALIZER.deserialize(settingParser, mapper);

            // Create the index with both settings and mappings
            final CreateIndexRequest request = new CreateIndexRequest.Builder()
                    .index(Constants.INDEX_NAME)
                    .mappings(mappings)
                    .settings(settings).build();

            client.indices().create(request);
        }
    }

    /**
     * Save a snippet to the repository.
     *
     * @param snippet    The snippet to save
     * @param sourceJson The original JSON source
     * @return The ID of the saved snippet
     * @throws IOException if there is an error communicating with OpenSearch
     */
    public String save(Snippet snippet, String sourceJson) throws IOException {
        try {
            // Store the entire JSON in the source field
            snippet.setSource(sourceJson);

            // Create the index request
            IndexRequest<Snippet> request = new IndexRequest.Builder<Snippet>()
                    .index(Constants.INDEX_NAME)
                    .document(snippet)
                    .build();

            // Execute the index request
            IndexResponse response = client.index(request);
            log.debug("Indexed snippet with ID: {}", response.id());

            return response.id();
        } catch (IOException e) {
            log.error("Error saving snippet: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Delete a snippet by its ID.
     *
     * @param id The ID of the snippet to delete
     * @throws IOException if there is an error deleting the snippet
     */
    public void delete(String id) throws IOException {
        try {
            // First, find the document ID using the $id field
            Query query = queryBuilder.buildIdQuery(id);

            SearchRequest searchRequest = new SearchRequest.Builder()
                    .index(Constants.INDEX_NAME)
                    .query(query)
                    .size(1)
                    .build();

            // Execute the search to get the document ID
            SearchResponse<Snippet> response = client.search(searchRequest, Snippet.class);
            if (response.hits().total().value() == 0) {
                throw new IOException("Snippet with $id '" + id + "' not found");
            }

            String documentId = response.hits().hits().getFirst().id();

            // Create the delete request using the found document ID
            DeleteRequest request = new DeleteRequest.Builder()
                    .index(Constants.INDEX_NAME)
                    .id(documentId)
                    .build();

            // Execute the delete request
            client.delete(request);
            log.debug("Deleted snippet with $id: {} and document ID: {}", id, documentId);
        } catch (IOException e) {
            log.error("Error deleting snippet with $id: {}", id, e);
            throw e;
        }
    }

    /**
     * Search for snippets.
     *
     * @param searchText The text to search for (optional)
     * @param limit      The maximum number of results to return
     * @return A list of matching snippets
     * @throws IOException if there is an error communicating with OpenSearch
     */
    public List<Snippet> search(String searchText, int limit) throws IOException {
        try {
            // Build the search query
            Query query = queryBuilder.buildSearchQuery(searchText);

            // Create the search request
            SearchRequest request = new SearchRequest.Builder()
                    .index(Constants.INDEX_NAME)
                    .query(query)
                    .sort(s -> s.field(f -> f.field(Constants.CREATED_AT).order(SortOrder.Desc)))
                    .size(limit > 0 ? limit : Constants.DEFAULT_LIMIT)
                    .build();

            // Execute the search request
            SearchResponse<Snippet> response = client.search(request, Snippet.class);
            return convertSearchHits(response.hits().hits(), false);
        } catch (IOException e) {
            log.error("Error searching snippets: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Check if a snippet with the given $id exists.
     *
     * @param id The $id to check
     * @return true if a snippet with the given $id exists, false otherwise
     * @throws IOException if there is an error communicating with OpenSearch
     */
    public boolean existsById(String id) throws IOException {
        if (id == null || id.trim().isEmpty()) {
            return false;
        }

        try {
            // Create a query to find the exact $id
            Query query = queryBuilder.buildIdQuery(id);

            // Create the search request
            SearchRequest request = new SearchRequest.Builder()
                    .index(Constants.INDEX_NAME)
                    .query(query)
                    .size(0) // We only need to know if it exists, not the actual document
                    .build();

            // Execute the search request
            SearchResponse<Snippet> response = client.search(request, Snippet.class);
            return response.hits().total().value() > 0;
        } catch (IOException e) {
            log.error("Error checking if snippet exists by $id: {}", id, e);
            throw e;
        }
    }

    /**
     * Convert search hits to Snippet objects.
     *
     * @param hits          The search hits to convert
     * @param includeSource Whether to include the source field in the returned snippets
     * @return A list of Snippet objects
     */
    private List<Snippet> convertSearchHits(List<Hit<Snippet>> hits, boolean includeSource) {
        if (hits == null || hits.isEmpty()) {
            return Collections.emptyList();
        }

        List<Snippet> snippets = new ArrayList<>(hits.size());
        for (Hit<Snippet> hit : hits) {
            try {
                Snippet snippet = hit.source();
                if (snippet != null) {
                    // If we don't want to include the source, create a copy without it
                    if (!includeSource) {
                        snippet = snippet.withoutSource();
                    }
                    snippets.add(snippet);
                }
            } catch (Exception e) {
                log.error("Error converting search hit to Snippet: {}", e.getMessage(), e);
            }
        }

        return snippets;
    }
}
