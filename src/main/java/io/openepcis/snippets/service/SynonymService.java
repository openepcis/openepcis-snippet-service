package io.openepcis.snippets.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.openepcis.snippets.constants.Constants;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Service for handling synonym operations.
 * This service loads synonyms from a JSON file and provides methods to retrieve synonyms for a given term.
 * The synonyms are loaded once statically to avoid memory leaks and improve performance.
 */
@ApplicationScoped
@Slf4j
public class SynonymService {

    // Static field to ensure it is created only once
    private static final Map<String, List<String>> synonymMap = new HashMap<>();

    // Static initializer block to load synonyms once when the class is loaded
    static {
        loadSynonyms();
    }

    // No need for injected ObjectMapper as we use a temporary one in the static loadSynonyms method

    /**
     * Load synonyms from the external JSON file.
     * This method is called once when the class is loaded.
     */
    private static void loadSynonyms() {
        // Create a temporary ObjectMapper for static initialization
        ObjectMapper tempObjectMapper = new ObjectMapper();

        try (InputStream inputStream = SynonymService.class.getClassLoader().getResourceAsStream(Constants.SYNONYM_FILE_PATH)) {
            if (inputStream == null) {
                log.warn("Synonym file not found: {}. Using empty synonym map.", Constants.SYNONYM_FILE_PATH);
                return;
            }

            // Parse the JSON file into a map structure using TypeReference for the flattened structure
            TypeReference<Map<String, List<String>>> typeRef = new TypeReference<>() {};
            Map<String, List<String>> flatSynonyms = tempObjectMapper.readValue(inputStream, typeRef);

            // Add all entries to the synonym map
            synonymMap.putAll(flatSynonyms);

            log.info("Loaded {} synonym entries from {}", synonymMap.size(), Constants.SYNONYM_FILE_PATH);
        } catch (IOException e) {
            log.error("Failed to load synonyms from {}", Constants.SYNONYM_FILE_PATH, e);
        }
    }

    /**
     * Get synonyms for a given search text, only handling exact matches.
     *
     * @param searchText The text to find synonyms for
     * @return A set of synonyms including the original search text
     */
    public Set<String> getSynonyms(String searchText) {
        if (searchText == null || searchText.trim().isEmpty() || searchText.length() < 2) {
            return Collections.emptySet();
        }

        Set<String> result = new HashSet<>();
        String lowerSearchText = searchText.toLowerCase().trim();

        // Add the original search text
        result.add(lowerSearchText);

        // Check for exact matches in the synonym map
        addExactMatchSynonyms(lowerSearchText, result);

        return result;
    }

    /**
     * Add synonyms for exact matches in the synonym map.
     *
     * @param term The term to find exact synonyms for
     * @param result The set to add the synonyms to
     */
    private void addExactMatchSynonyms(String term, Set<String> result) {
        // Check if the term is a key in the synonym map
        List<String> exactSynonyms = synonymMap.get(term);
        if (exactSynonyms != null) {
            result.addAll(exactSynonyms);
            // Also add the key itself
            result.add(term);
        }

        // Check if the term is a value in the synonym map
        for (Map.Entry<String, List<String>> entry : synonymMap.entrySet()) {
            if (entry.getValue().contains(term)) {
                result.add(entry.getKey());
                result.addAll(entry.getValue());
            }
        }
    }

}
