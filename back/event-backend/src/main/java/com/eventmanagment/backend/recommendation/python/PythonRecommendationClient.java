package com.eventmanagment.backend.recommendation.python;

import com.eventmanagment.backend.config.RecommendationPythonProperties;
import com.eventmanagment.backend.recommendation.dto.RecommendationScoreResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class PythonRecommendationClient {

    private final RestClient.Builder restClientBuilder;
    private final RecommendationPythonProperties properties;

    public List<RecommendationScoreResponse> score(PythonScoreRequest request) {
        RestClient client = restClientBuilder.baseUrl(properties.baseUrl()).build();
        return client
                .post()
                .uri(properties.scorePath())
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(new ParameterizedTypeReference<List<RecommendationScoreResponse>>() {});
    }
}
