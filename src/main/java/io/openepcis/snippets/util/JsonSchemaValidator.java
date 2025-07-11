package io.openepcis.snippets.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Utility class for validating JSON against a schema
 */
@ApplicationScoped
@Slf4j
public class JsonSchemaValidator {

    private JsonSchema snippetSchema;

    @Inject
    ObjectMapper objectMapper;

    /**
     * Initialize the validator by loading the snippet schema
     */
    public void init() {
        try (InputStream schemaStream = getClass().getClassLoader()
                .getResourceAsStream("schema/snippet-schema.json")) {

            if (schemaStream == null) {
                log.error("Failed to load snippet schema");
                return;
            }

            JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
            snippetSchema = factory.getSchema(schemaStream);
            log.info("Snippet schema loaded successfully");
        } catch (IOException e) {
            log.error("Error loading snippet schema", e);
        }
    }

    /**
     * Validate a JSON string against the snippet schema
     *
     * @param json The JSON string to validate
     * @return A set of validation error messages, empty if validation succeeds
     */
    public Set<ValidationMessage> validateSnippet(String json) {
        try {
            if (snippetSchema == null) {
                init();
            }

            JsonNode jsonNode = objectMapper.readTree(json);
            return snippetSchema.validate(jsonNode);
        } catch (Exception e) {
            log.error("Error validating JSON", e);
            throw new RuntimeException("Error validating JSON: " + e.getMessage(), e);
        }
    }

    /**
     * Validate a JSON string against the snippet schema and return a formatted error message
     *
     * @param json The JSON string to validate
     * @return null if validation succeeds, otherwise a string containing all validation errors
     */
    public String validateSnippetWithErrorMessage(String json) {
        Set<ValidationMessage> errors = validateSnippet(json);

        if (errors.isEmpty()) {
            return null;
        }

        return errors.stream()
                .map(ValidationMessage::getMessage)
                .collect(Collectors.joining(", "));
    }
}
