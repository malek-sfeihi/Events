package com.eventmanagment.backend.config;

import java.nio.file.Path;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String uploadsLocation = Path.of("uploads").toAbsolutePath().toUri().toString();
        if (!uploadsLocation.endsWith("/")) {
            uploadsLocation = uploadsLocation + "/";
        }
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadsLocation);
    }
}
