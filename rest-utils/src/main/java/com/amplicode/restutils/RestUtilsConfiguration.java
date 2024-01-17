package com.amplicode.restutils;

import com.amplicode.restutils.patch.ObjectPatcher;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.Validator;

@Configuration
public class RestUtilsConfiguration {
    @Bean
    public ObjectPatcher objectPatcher(ObjectMapper objectMapper, Validator validator) {
        return new ObjectPatcher(objectMapper, validator);
    }
}
