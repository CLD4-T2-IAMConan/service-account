package com.company.account.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/**
 * Jackson configuration for HTTP JSON serialization/deserialization
 * This is separate from Redis ObjectMapper configuration in CacheConfig
 */
@Configuration
public class JacksonConfig {

    /**
     * Primary ObjectMapper bean for HTTP endpoints
     * Does NOT use polymorphic type handling (no @class property required)
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
        ObjectMapper mapper = builder.build();

        // Register Java 8 date/time module
        mapper.registerModule(new JavaTimeModule());

        // Write dates as ISO-8601 strings instead of timestamps
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // Do NOT activate default typing for HTTP JSON
        // (Redis uses a separate ObjectMapper with default typing)

        return mapper;
    }
}
