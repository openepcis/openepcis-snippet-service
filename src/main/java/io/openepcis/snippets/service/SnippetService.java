package io.openepcis.snippets.service;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.openepcis.snippets.model.Snippet;
import io.openepcis.snippets.repository.SnippetRepository;
import io.openepcis.snippets.util.JsonSchemaValidator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.util.List;

/**
 * Service layer for Snippet operations
 * Handles business logic between the resource and repository layers
 */
@ApplicationScoped
public class SnippetService {

    private static final Logger LOG = Logger.getLogger(SnippetService.class);

    @Inject
    SnippetRepository snippetRepository;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    JsonSchemaValidator jsonSchemaValidator;

    /**
     * Create a new snippet
     *
     * @param requestBody The JSON request body containing the snippet data
     * @return The created snippet
     * @throws IOException If there is an error processing the request
     * @throws IllegalArgumentException If the request is invalid or a duplicate $id is found
     */
    public Snippet createSnippet(String requestBody) throws IOException, IllegalArgumentException {
        // Validate request body
        if (requestBody == null || requestBody.trim().isEmpty()) {
            throw new IllegalArgumentException("Request body cannot be empty");
        }

        // Parse JSON
        ObjectNode jsonNode;
        try {
            jsonNode = objectMapper.readValue(requestBody, ObjectNode.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JSON format: " + e.getMessage());
        }

        // Validate against schema
        String validationError = jsonSchemaValidator.validateSnippetWithErrorMessage(requestBody);
        if (validationError != null) {
            throw new IllegalArgumentException("Schema validation failed: " + validationError);
        }

        // Check for duplicate $id
        if (jsonNode.has("$id")) {
            String id = jsonNode.get("$id").asText();
            boolean exists = snippetRepository.existsById(id);
            if (exists) {
                throw new IllegalArgumentException("A snippet with $id '" + id + "' already exists");
            }
        }

        // Convert to Snippet object
        Snippet snippet;
        try {
            snippet = objectMapper.convertValue(jsonNode, Snippet.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid snippet format: " + e.getMessage());
        }

        // Save the snippet
        snippetRepository.save(snippet, requestBody);
        
        LOG.info("Created new snippet with $id: " + (jsonNode.has("$id") ? jsonNode.get("$id").asText() : "<no id>"));
        
        return snippet;
    }

    /**
     * Search for snippets based on the provided search text
     *
     * @param searchText The text to search for
     * @param limit The maximum number of results to return
     * @return A list of matching snippets
     * @throws IOException If there is an error searching for snippets
     */
    public List<Snippet> searchSnippets(String searchText, int limit) throws IOException {
        return snippetRepository.search(searchText, limit);
    }
}
