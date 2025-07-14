package io.openepcis.snippets.model;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.util.StdConverter;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Snippet {
    
    @JsonProperty("$id")
    private String id;
    
    @JsonProperty("$schema")
    private String schema;
    
    private String description;
    
    private String title;
    
    @JsonDeserialize(converter = DefinitionsConverter.class)
    private Object definitions;

    @JsonProperty("$defs")
    @JsonDeserialize(converter = DefinitionsConverter.class)
    private Object defs;

    private Instant createdAt;
    
    // Used to store the entire JSON document
    private String source;
    
    public Snippet() {
        this.createdAt = Instant.now();
    }
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getSchema() {
        return schema;
    }
    
    public void setSchema(String schema) {
        this.schema = schema;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public Object getDefinitions() {
        return definitions;
    }
    
    public void setDefinitions(Object definitions) {
        this.definitions = definitions;
    }

    public Object getDefs() {
        return defs;
    }

    public void setDefs(Object defs) {
        this.defs = defs;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
    
    public String getSource() {
        return source;
    }
    
    public void setSource(String source) {
        this.source = source;
    }
    

    
    /**
     * Creates a copy of this snippet without the source field for API responses
     * 
     * @return A new Snippet object without the source field
     */
    public Snippet withoutSource() {
        Snippet copy = new Snippet();
        copy.setId(this.id);
        copy.setSchema(this.schema);
        copy.setDescription(this.description);
        copy.setTitle(this.title);
        copy.setDefinitions(this.definitions);
        copy.setDefs(this.defs);
        copy.setCreatedAt(this.createdAt);

        return copy;
    }
    
    /**
     * Converter to handle the 'definitions' field which can be either a complex object or a string
     */
    public static class DefinitionsConverter extends StdConverter<Object, Object> {
        @Override
        public Object convert(Object value) {
            // If the value is a string, we can return it as is
            // Jackson will handle it appropriately when serializing/deserializing
            return value;
        }
    }
}
