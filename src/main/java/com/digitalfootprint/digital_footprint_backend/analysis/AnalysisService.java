package com.digitalfootprint.digital_footprint_backend.analysis;

import com.digitalfootprint.digital_footprint_backend.analysis.dto.*;
import com.digitalfootprint.digital_footprint_backend.auth.User;
import com.digitalfootprint.digital_footprint_backend.auth.UserRepository;
import com.digitalfootprint.digital_footprint_backend.common.ApiResponse;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalysisService {

    private final AnalysisRepository analysisRepository;
    private final UserRepository userRepository;
    private final AiServiceClient aiServiceClient;

    /**
     * Creates and runs a full analysis for the authenticated user.
     * Flow: Validate → Save request → Call Python AI → Parse result → Persist → Return.
     */
    @Transactional
    public ApiResponse<AnalysisResponse> runAnalysis(String phoneNumber, AnalysisRequest request) {
        // Validate that at least some content is provided
        boolean hasContent = isNotBlank(request.getUsername())
                || isNotBlank(request.getProfileUrl())
                || isNotBlank(request.getContent());
        if (!hasContent) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Please provide at least a username, profile URL, or content to analyze."
            );
        }

        User user = resolveUser(phoneNumber);

        // Clean input
        String cleanUsername = trimOrNull(request.getUsername());
        String cleanPlatform = trimOrNull(request.getPlatform());
        String cleanUrl = trimOrNull(request.getProfileUrl());
        String cleanContent = trimOrNull(request.getContent());

        // Build AI request
        AnalysisRequest aiRequest = new AnalysisRequest();
        aiRequest.setUsername(cleanUsername);
        aiRequest.setPlatform(cleanPlatform);
        aiRequest.setProfileUrl(cleanUrl);
        aiRequest.setContent(cleanContent);

        // Call Python AI service
        log.info("Sending analysis to AI service for user id={}", user.getId());
        JsonNode aiResult = aiServiceClient.analyze(aiRequest);

        // Parse AI response
        int overallScore = aiResult.path("overallScore").asInt(0);
        String riskLevel = aiResult.path("riskLevel").asText("LOW");
        int totalRisks = aiResult.path("totalRisksDetected").asInt(0);

        // Parse risk explanations
        List<String> explanationList = new ArrayList<>();
        JsonNode explanationNode = aiResult.path("riskExplanation");
        if (explanationNode.isArray()) {
            for (JsonNode node : explanationNode) {
                explanationList.add(node.asText());
            }
        }
        String explanationJson = String.join("||", explanationList);

        // Build and save Analysis entity
        Analysis analysis = Analysis.builder()
                .user(user)
                .username(cleanUsername)
                .platform(cleanPlatform)
                .profileUrl(cleanUrl)
                .content(cleanContent)
                .overallScore(overallScore)
                .riskLevel(riskLevel)
                .totalRisksDetected(totalRisks)
                .riskExplanation(explanationJson)
                .build();

        // Parse and attach detected risks
        List<DetectedRisk> risks = new ArrayList<>();
        JsonNode risksNode = aiResult.path("detectedRisks");
        if (risksNode.isArray()) {
            for (JsonNode riskNode : risksNode) {
                DetectedRisk risk = DetectedRisk.builder()
                        .analysis(analysis)
                        .category(riskNode.path("category").asText("Unknown"))
                        .indicator(riskNode.path("indicator").asText("Unknown"))
                        .riskLevel(riskNode.path("riskLevel").asText("LOW"))
                        .explanation(riskNode.path("explanation").asText(""))
                        .impact(riskNode.path("impact").asText(""))
                        .build();
                risks.add(risk);
            }
        }
        analysis.getDetectedRisks().addAll(risks);

        // Parse and attach recommendations
        List<Recommendation> recs = new ArrayList<>();
        JsonNode recsNode = aiResult.path("recommendations");
        if (recsNode.isArray()) {
            for (JsonNode recNode : recsNode) {
                Recommendation rec = Recommendation.builder()
                        .analysis(analysis)
                        .priority(recNode.path("priority").asText("LOW"))
                        .text(recNode.path("text").asText(""))
                        .build();
                recs.add(rec);
            }
        }
        analysis.getRecommendations().addAll(recs);

        // Save everything in one transaction
        Analysis saved = analysisRepository.save(analysis);
        log.info("Analysis id={} saved with score={} level={}", saved.getId(), overallScore, riskLevel);

        return ApiResponse.success("Analysis completed successfully.", toFullResponse(saved));
    }

    /**
     * Get a single analysis by ID for the authenticated user.
     */
    @Transactional(readOnly = true)
    public ApiResponse<AnalysisResponse> getAnalysis(String phoneNumber, Long analysisId) {
        User user = resolveUser(phoneNumber);
        Analysis analysis = analysisRepository.findByIdAndUserId(analysisId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Analysis not found."
                ));
        return ApiResponse.success("Analysis retrieved.", toFullResponse(analysis));
    }

    /**
     * Get analysis history for the authenticated user.
     */
    @Transactional(readOnly = true)
    public ApiResponse<List<AnalysisHistoryItem>> getHistory(String phoneNumber) {
        User user = resolveUser(phoneNumber);
        List<Analysis> analyses = analysisRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        List<AnalysisHistoryItem> items = analyses.stream().map(this::toHistoryItem).toList();
        return ApiResponse.success("Analysis history retrieved.", items);
    }

    /**
     * Get only the detected risks for an analysis.
     */
    @Transactional(readOnly = true)
    public ApiResponse<List<DetectedRiskDto>> getRisks(String phoneNumber, Long analysisId) {
        User user = resolveUser(phoneNumber);
        Analysis analysis = analysisRepository.findByIdAndUserId(analysisId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Analysis not found."
                ));
        List<DetectedRiskDto> dtos = analysis.getDetectedRisks().stream()
                .map(this::toRiskDto)
                .toList();
        return ApiResponse.success("Risks retrieved.", dtos);
    }

    /**
     * Get only the recommendations for an analysis.
     */
    @Transactional(readOnly = true)
    public ApiResponse<List<RecommendationDto>> getRecommendations(String phoneNumber, Long analysisId) {
        User user = resolveUser(phoneNumber);
        Analysis analysis = analysisRepository.findByIdAndUserId(analysisId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Analysis not found."
                ));
        List<RecommendationDto> dtos = analysis.getRecommendations().stream()
                .map(this::toRecDto)
                .toList();
        return ApiResponse.success("Recommendations retrieved.", dtos);
    }

    // ─── Helpers ───────────────────────────────────────────

    private User resolveUser(String phoneNumber) {
        return userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "User not found."
                ));
    }

    private AnalysisResponse toFullResponse(Analysis a) {
        List<String> explanations = new ArrayList<>();
        if (a.getRiskExplanation() != null && !a.getRiskExplanation().isBlank()) {
            for (String part : a.getRiskExplanation().split("\\|\\|")) {
                if (!part.isBlank()) explanations.add(part.trim());
            }
        }

        return AnalysisResponse.builder()
                .id(a.getId())
                .username(a.getUsername())
                .platform(a.getPlatform())
                .profileUrl(a.getProfileUrl())
                .overallScore(a.getOverallScore())
                .riskLevel(a.getRiskLevel())
                .totalRisksDetected(a.getTotalRisksDetected())
                .riskExplanation(explanations)
                .detectedRisks(a.getDetectedRisks().stream().map(this::toRiskDto).toList())
                .recommendations(a.getRecommendations().stream().map(this::toRecDto).toList())
                .createdAt(a.getCreatedAt())
                .build();
    }

    private AnalysisHistoryItem toHistoryItem(Analysis a) {
        return AnalysisHistoryItem.builder()
                .id(a.getId())
                .platform(a.getPlatform())
                .username(a.getUsername())
                .overallScore(a.getOverallScore())
                .riskLevel(a.getRiskLevel())
                .totalRisksDetected(a.getTotalRisksDetected())
                .createdAt(a.getCreatedAt())
                .build();
    }

    private DetectedRiskDto toRiskDto(DetectedRisk r) {
        return DetectedRiskDto.builder()
                .id(r.getId())
                .category(r.getCategory())
                .indicator(r.getIndicator())
                .riskLevel(r.getRiskLevel())
                .explanation(r.getExplanation())
                .impact(r.getImpact())
                .build();
    }

    private RecommendationDto toRecDto(Recommendation r) {
        return RecommendationDto.builder()
                .id(r.getId())
                .priority(r.getPriority())
                .text(r.getText())
                .build();
    }

    private boolean isNotBlank(String s) {
        return s != null && !s.isBlank();
    }

    private String trimOrNull(String s) {
        if (s == null || s.isBlank()) return null;
        return s.trim();
    }
}
