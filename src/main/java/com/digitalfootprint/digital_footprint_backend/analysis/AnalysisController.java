package com.digitalfootprint.digital_footprint_backend.analysis;

import com.digitalfootprint.digital_footprint_backend.analysis.dto.*;
import com.digitalfootprint.digital_footprint_backend.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
public class AnalysisController {

    private final AnalysisService analysisService;

    /**
     * POST /api/analysis
     * Run a new digital footprint analysis for the authenticated user.
     * Body: { username?, platform?, profileUrl?, content? }
     */
    @PostMapping
    public ResponseEntity<ApiResponse<AnalysisResponse>> runAnalysis(
            @AuthenticationPrincipal String phoneNumber,
            @Valid @RequestBody AnalysisRequest request
    ) {
        ApiResponse<AnalysisResponse> response = analysisService.runAnalysis(phoneNumber, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/analysis/history
     * Returns the authenticated user's analysis history (summary list).
     */
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<AnalysisHistoryItem>>> getHistory(
            @AuthenticationPrincipal String phoneNumber
    ) {
        ApiResponse<List<AnalysisHistoryItem>> response = analysisService.getHistory(phoneNumber);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/analysis/{id}
     * Returns the full analysis report for a given analysis ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AnalysisResponse>> getAnalysis(
            @AuthenticationPrincipal String phoneNumber,
            @PathVariable Long id
    ) {
        ApiResponse<AnalysisResponse> response = analysisService.getAnalysis(phoneNumber, id);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/analysis/{id}/risks
     * Returns only the detected risks for an analysis.
     */
    @GetMapping("/{id}/risks")
    public ResponseEntity<ApiResponse<List<DetectedRiskDto>>> getRisks(
            @AuthenticationPrincipal String phoneNumber,
            @PathVariable Long id
    ) {
        ApiResponse<List<DetectedRiskDto>> response = analysisService.getRisks(phoneNumber, id);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/analysis/{id}/recommendations
     * Returns only the recommendations for an analysis.
     */
    @GetMapping("/{id}/recommendations")
    public ResponseEntity<ApiResponse<List<RecommendationDto>>> getRecommendations(
            @AuthenticationPrincipal String phoneNumber,
            @PathVariable Long id
    ) {
        ApiResponse<List<RecommendationDto>> response = analysisService.getRecommendations(phoneNumber, id);
        return ResponseEntity.ok(response);
    }
}
