package com.amplicode.restutils;

import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

@Configuration
@AutoConfigurationPackage(basePackageClasses = RestUtilsConfiguration.class)
@EnableAutoConfiguration
@Import({RestUtilsConfiguration.class})
@PropertySource(name = "test_properties", value = "classpath:test_support/test-application.properties")
public class RestUtilsTestConfiguration {

}
