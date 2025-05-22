package io.openepcis.snippets.config;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import org.jboss.logging.Logger;

/**
 * Configuration for Jackson JSON serialization/deserialization
 */
@ApplicationScoped
public class JacksonConfig {

    private static final Logger LOG = Logger.getLogger(JacksonConfig.class);

    /**
     * Produces a configured ObjectMapper bean
     * 
     * @return The configured ObjectMapper
     */
    @Produces
    @Singleton
    public ObjectMapper objectMapper() {
        LOG.info("Configuring ObjectMapper to exclude null values and handle Java 8 date/time types");
        
        ObjectMapper objectMapper = new ObjectMapper();
        
        objectMapper.setSerializationInclusion(Include.NON_NULL)
        .registerModule(new JavaTimeModule())
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, new SerializationFeature[]{SerializationFeature.FAIL_ON_EMPTY_BEANS});

        return objectMapper;
    }
}
