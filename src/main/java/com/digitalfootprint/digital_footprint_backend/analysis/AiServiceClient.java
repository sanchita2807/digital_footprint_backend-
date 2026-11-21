package com.digitalfootprint.digital_footprint_backend.analysis;

import com.digitalfootprint.digital_footprint_backend.analysis.dto.AnalysisRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
@Slf4j
public class AiServiceClient {

    @Value("${app.ai-service.url:http://localhost:8000}")
    private String aiServiceUrl;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public AiServiceClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Health check — calls the Python service's /health endpoint.
     * Returns true if the service responds with HTTP 200.
     */
    public boolean checkHealth() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(aiServiceUrl + "/health"))
                    .header("Accept", "application/json")
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            log.warn("AI service health check failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Sends analysis input to the Python AI service and returns the parsed result.
     */
    public JsonNode analyze(AnalysisRequest request) {
        try {
            // Build the payload for Python service
            var payload = objectMapper.createObjectNode();
            if (request.getUsername() != null) payload.put("username", request.getUsername().trim());
            if (request.getPlatform() != null) payload.put("platform", request.getPlatform().trim());
            if (request.getProfileUrl() != null) payload.put("profileUrl", request.getProfileUrl().trim());
            if (request.getContent() != null) payload.put("content", request.getContent().trim());

            String body = objectMapper.writeValueAsString(payload);
            log.debug("Sending to AI service: {}", body);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(aiServiceUrl + "/analyze"))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .timeout(Duration.ofSeconds(30))
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            log.debug("AI service response status: {}", response.statusCode());
            log.debug("AI service response body: {}", response.body());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.error("AI service returned error status: {}, body: {}", response.statusCode(), response.body());
                throw new ResponseStatusException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "AI analysis service returned an error. Please try again."
                );
            }

            return objectMapper.readTree(response.body());

        } catch (ResponseStatusException rse) {
            throw rse;
        } catch (Exception e) {
            log.error("Failed to connect to AI service at {}: {}", aiServiceUrl, e.getMessage());
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "AI analysis service is currently unavailable. Please ensure the AI service is running and try again."
            );
        }
    }
}
