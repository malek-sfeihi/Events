package com.eventmanagment.backend;

import com.eventmanagment.backend.config.RecommendationPythonProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(RecommendationPythonProperties.class)
public class EventBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(EventBackendApplication.class, args);
	}

}
