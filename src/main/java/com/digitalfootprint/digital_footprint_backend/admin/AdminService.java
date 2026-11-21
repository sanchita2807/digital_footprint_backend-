package com.digitalfootprint.digital_footprint_backend.admin;

import com.digitalfootprint.digital_footprint_backend.admin.dto.*;
import com.digitalfootprint.digital_footprint_backend.analysis.*;
import com.digitalfootprint.digital_footprint_backend.analysis.dto.DetectedRiskDto;
import com.digitalfootprint.digital_footprint_backend.analysis.dto.RecommendationDto;
import com.digitalfootprint.digital_footprint_backend.auth.User;
import com.digitalfootprint.digital_footprint_backend.auth.UserRepository;
import com.digitalfootprint.digital_footprint_backend.common.ApiResponse;
import com.digitalfootprint.digital_footprint_backend.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Central service for all admin-panel operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    private final UserRepository userRepository;
    private final AnalysisRepository analysisRepository;
    private final DetectedRiskRepository detectedRiskRepository;
    private final RecommendationRepository recommendationRepository;
    private final AuditLogRepository auditLogRepository;
    private final AiServiceClient aiServiceClient;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    // ══════════════════════════════════════════════════════════
    // Authentication
    // ══════════════════════════════════════════════════════════

    @Transactional
    public ApiResponse<AdminAuthResponse> login(AdminLoginRequest request) {
        User admin = userRepository.findByEmail(request.getEmail().trim().toLowerCase())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Invalid email or password."));

        if (admin.getIsAdmin() == null || admin.getIsAdmin() != 1) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password.");
        }

        if (!admin.isActive()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Your admin account has been deactivated. Please contact the system administrator.");
        }

        if (admin.getPassword() == null
                || !passwordEncoder.matches(request.getPassword(), admin.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password.");
        }

        String token = jwtTokenProvider.generateToken(admin.getEmail(), admin.getId(), admin.getIsAdmin());

        recordAudit(admin.getEmail(), "LOGIN", "Admin logged in", null, null);

        AdminAuthResponse response = AdminAuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .adminId(admin.getId())
                .email(admin.getEmail())
                .fullName(admin.getFullName())
                .role(admin.getIsAdmin() != null && admin.getIsAdmin() == 1 ? "Admin" : "User")
                .build();

        return ApiResponse.success("Admin login successful.", response);
    }

    @Transactional
    public void logout(String adminEmail) {
        recordAudit(adminEmail, "LOGOUT", "Admin logged out", null, null);
    }

    // ══════════════════════════════════════════════════════════
    // Dashboard Statistics
    // ══════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public ApiResponse<AdminDashboardStats> getDashboardStats() {
        long totalUsers = userRepository.countByIsAdmin(0);
        long totalAnalyses = analysisRepository.count();
        long verifiedUsers = userRepository.countByIsVerified(true);
        long activeUsers = userRepository.countByIsActive(true);

        long lowRisk = analysisRepository.countByRiskLevel("LOW");
        long mediumRisk = analysisRepository.countByRiskLevel("MEDIUM");
        long highRisk = analysisRepository.countByRiskLevel("HIGH");

        // Trends for the last 14 days
        LocalDateTime trendStart = LocalDateTime.now().minusDays(13)
                .withHour(0).withMinute(0).withSecond(0).withNano(0);
        List<TrendPoint> registrationTrend = buildTrend(
                userRepository.countDailyRegistrations(trendStart), trendStart, 14);
        List<TrendPoint> analysisTrend = buildTrend(
                analysisRepository.countDailyAnalyses(trendStart), trendStart, 14);

        // Risk category distribution
        List<RiskCategoryCount> categoryDist = buildCategoryCounts(
                detectedRiskRepository.countByCategoryNative());

        // Recent analyses (latest 10)
        Page<Analysis> recentPage = analysisRepository
                .findAllWithUsers(PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt")));
        List<RecentAnalysisDto> recentAnalyses = recentPage.getContent().stream()
                .map(this::toRecentAnalysisDto)
                .toList();

        AdminDashboardStats stats = AdminDashboardStats.builder()
                .totalUsers(totalUsers)
                .totalAnalyses(totalAnalyses)
                .completedAnalyses(totalAnalyses)
                .pendingOrFailedAnalyses(0)
                .lowRiskCount(lowRisk)
                .mediumRiskCount(mediumRisk)
                .highRiskCount(highRisk)
                .verifiedUsers(verifiedUsers)
                .activeUsers(activeUsers)
                .userRegistrationTrend(registrationTrend)
                .analysisActivityTrend(analysisTrend)
                .riskCategoryDistribution(categoryDist)
                .recentAnalyses(recentAnalyses)
                .build();

        return ApiResponse.success("Dashboard statistics retrieved.", stats);
    }

    @Transactional(readOnly = true)
    public ApiResponse<List<AdminUserDto>> getRecentRegistrations(int limit) {
        Page<User> userPage = userRepository.findAll(
                PageRequest.of(0, Math.max(1, limit), Sort.by(Sort.Direction.DESC, "createdAt")));
        List<AdminUserDto> users = userPage.getContent().stream()
                .map(this::toAdminUserDto)
                .toList();
        return ApiResponse.success("Recent registrations retrieved.", users);
    }

    @Transactional(readOnly = true)
    public ApiResponse<List<AuditLogDto>> getRecentActivities(int limit) {
        Page<AuditLog> logPage = auditLogRepository.findAll(
                PageRequest.of(0, Math.max(1, limit), Sort.by(Sort.Direction.DESC, "createdAt")));
        List<AuditLogDto> logs = logPage.getContent().stream()
                .map(this::toAuditLogDto)
                .toList();
        return ApiResponse.success("Recent activities retrieved.", logs);
    }

    // ══════════════════════════════════════════════════════════
    // User Management
    // ══════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public ApiResponse<PageResponse<AdminUserDto>> getUsers(String search, String status, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<User> userPage;

        Boolean activeFilter = null;
        if (status != null && !status.isBlank() && !status.equalsIgnoreCase("all")) {
            if (status.equalsIgnoreCase("ACTIVE") || status.equalsIgnoreCase("true") || status.equals("1")) {
                activeFilter = true;
            } else if (status.equalsIgnoreCase("INACTIVE") || status.equalsIgnoreCase("false") || status.equals("0")) {
                activeFilter = false;
            }
        }

        if (search != null && !search.isBlank()) {
            if (activeFilter != null) {
                userPage = userRepository.searchUsersByStatus(search.trim(), activeFilter, pageRequest);
            } else {
                userPage = userRepository.searchUsers(search.trim(), pageRequest);
            }
        } else {
            if (activeFilter != null) {
                userPage = userRepository.findAllRegularUsersByStatus(activeFilter, pageRequest);
            } else {
                userPage = userRepository.findAllRegularUsers(pageRequest);
            }
        }

        List<AdminUserDto> users = userPage.getContent().stream()
                .map(this::toAdminUserDto)
                .toList();

        PageResponse<AdminUserDto> response = PageResponse.<AdminUserDto>builder()
                .content(users)
                .currentPage(userPage.getNumber())
                .totalPages(userPage.getTotalPages())
                .totalElements(userPage.getTotalElements())
                .pageSize(userPage.getSize())
                .build();

        return ApiResponse.success("Users retrieved.", response);
    }

    @Transactional(readOnly = true)
    public ApiResponse<AdminUserDetailDto> getUserDetail(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found."));

        List<Analysis> userAnalyses = analysisRepository.findByUserIdForAdmin(userId);

        long highRiskCount = userAnalyses.stream().filter(a -> "HIGH".equals(a.getRiskLevel())).count();
        long mediumRiskCount = userAnalyses.stream().filter(a -> "MEDIUM".equals(a.getRiskLevel())).count();
        long lowRiskCount = userAnalyses.stream().filter(a -> "LOW".equals(a.getRiskLevel())).count();

        List<RecentAnalysisDto> analysisHistory = userAnalyses.stream()
                .limit(20)
                .map(this::toRecentAnalysisDto)
                .toList();

        AdminUserDetailDto detail = AdminUserDetailDto.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .username(user.getUsername())
                .occupation(user.getOccupation())
                .city(user.getCity())
                .isVerified(user.isVerified())
                .isActive(user.isActive())
                .role(user.getIsAdmin() != null && user.getIsAdmin() == 1 ? "Admin" : "User")
                .analysisCount(userAnalyses.size())
                .highRiskCount(highRiskCount)
                .mediumRiskCount(mediumRiskCount)
                .lowRiskCount(lowRiskCount)
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .analysisHistory(analysisHistory)
                .build();

        return ApiResponse.success("User details retrieved.", detail);
    }

    @Transactional
    public ApiResponse<AdminUserDto> updateUserStatus(Long userId, boolean isActive, String adminEmail) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found."));

        if (user.getIsAdmin() != null && user.getIsAdmin() == 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot modify admin accounts.");
        }

        user.setActive(isActive);
        userRepository.save(user);

        recordAudit(adminEmail, "USER_STATUS_CHANGED",
                "User " + user.getEmail() + " status set to " + (isActive ? "active" : "inactive"),
                "USER", userId);

        return ApiResponse.success("User status updated.", toAdminUserDto(user));
    }

    @Transactional
    public ApiResponse<AdminUserDto> createUser(AdminCreateUserRequest request, String adminEmail) {
        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phone number already exists.");
        }
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            if (userRepository.findByEmail(request.getEmail().trim()).isPresent()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email already exists.");
            }
        }

        User user = User.builder()
                .fullName(request.getFullName().trim())
                .phoneNumber(request.getPhoneNumber().trim())
                .email(request.getEmail() != null && !request.getEmail().isBlank() ? request.getEmail().trim() : null)
                .username(request.getUsername())
                .occupation(request.getOccupation())
                .city(request.getCity())
                .isAdmin(request.getIsAdmin() != null ? request.getIsAdmin() : 0)
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .isVerified(true)
                .password(request.getPassword() != null && !request.getPassword().isBlank()
                        ? passwordEncoder.encode(request.getPassword())
                        : null)
                .build();

        user = userRepository.save(user);

        recordAudit(adminEmail, "USER_CREATED",
                "Created user " + user.getPhoneNumber() + " (" + user.getFullName() + ")",
                "USER", user.getId());

        return ApiResponse.success("User created successfully.", toAdminUserDto(user));
    }

    @Transactional
    public ApiResponse<AdminUserDto> updateUser(Long userId, AdminUpdateUserRequest request, String adminEmail) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found."));

        user.setFullName(request.getFullName().trim());
        user.setPhoneNumber(request.getPhoneNumber().trim());
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail().trim().isEmpty() ? null : request.getEmail().trim());
        }
        if (request.getUsername() != null) user.setUsername(request.getUsername());
        if (request.getOccupation() != null) user.setOccupation(request.getOccupation());
        if (request.getCity() != null) user.setCity(request.getCity());
        if (request.getIsAdmin() != null) user.setIsAdmin(request.getIsAdmin());
        if (request.getIsActive() != null) user.setActive(request.getIsActive());
        if (request.getIsVerified() != null) user.setVerified(request.getIsVerified());

        user = userRepository.save(user);

        recordAudit(adminEmail, "USER_UPDATED",
                "Updated user " + user.getPhoneNumber() + " (" + user.getFullName() + ")",
                "USER", user.getId());

        return ApiResponse.success("User updated successfully.", toAdminUserDto(user));
    }

    @Transactional
    public ApiResponse<String> deleteUser(Long userId, String adminEmail) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found."));

        // Delete user's analyses first
        List<Analysis> analyses = analysisRepository.findByUserIdForAdmin(userId);
        if (analyses != null && !analyses.isEmpty()) {
            analysisRepository.deleteAll(analyses);
        }

        userRepository.delete(user);

        recordAudit(adminEmail, "USER_DELETED",
                "Deleted user ID " + userId + " (" + user.getFullName() + ")",
                "USER", userId);

        return ApiResponse.success("User and associated analyses deleted successfully.");
    }

    // ══════════════════════════════════════════════════════════
    // Analysis Management
    // ══════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public ApiResponse<PageResponse<AdminAnalysisDto>> getAnalyses(String search, String riskLevel, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id"));
        Page<Analysis> analysisPage;

        boolean hasSearch = search != null && !search.isBlank();
        boolean hasRisk = riskLevel != null && !riskLevel.isBlank();

        if (hasSearch || hasRisk) {
            analysisPage = analysisRepository.searchAnalyses(
                    hasSearch ? search.trim() : null,
                    hasRisk ? riskLevel.toUpperCase() : null,
                    pageRequest);
        } else {
            analysisPage = analysisRepository.findAllWithUsers(pageRequest);
        }

        List<AdminAnalysisDto> analyses = analysisPage.getContent().stream()
                .map(this::toAdminAnalysisDto)
                .toList();

        PageResponse<AdminAnalysisDto> response = PageResponse.<AdminAnalysisDto>builder()
                .content(analyses)
                .currentPage(analysisPage.getNumber())
                .totalPages(analysisPage.getTotalPages())
                .totalElements(analysisPage.getTotalElements())
                .pageSize(analysisPage.getSize())
                .build();

        return ApiResponse.success("Analyses retrieved.", response);
    }

    @Transactional(readOnly = true)
    public ApiResponse<AdminAnalysisDetailDto> getAnalysisDetail(Long analysisId) {
        Analysis analysis = analysisRepository.findById(analysisId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Analysis not found."));

        List<String> explanations = new ArrayList<>();
        if (analysis.getRiskExplanation() != null && !analysis.getRiskExplanation().isBlank()) {
            for (String part : analysis.getRiskExplanation().split("\\|\\|")) {
                if (!part.isBlank()) explanations.add(part.trim());
            }
        }

        List<DetectedRiskDto> risks = analysis.getDetectedRisks().stream()
                .map(r -> DetectedRiskDto.builder()
                        .id(r.getId())
                        .category(r.getCategory())
                        .indicator(r.getIndicator())
                        .riskLevel(r.getRiskLevel())
                        .explanation(r.getExplanation())
                        .impact(r.getImpact())
                        .build())
                .toList();

        List<RecommendationDto> recs = analysis.getRecommendations().stream()
                .map(r -> RecommendationDto.builder()
                        .id(r.getId())
                        .priority(r.getPriority())
                        .text(r.getText())
                        .build())
                .toList();

        AdminAnalysisDetailDto detail = AdminAnalysisDetailDto.builder()
                .id(analysis.getId())
                .userId(analysis.getUser().getId())
                .userName(analysis.getUser().getFullName())
                .userEmail(analysis.getUser().getEmail())
                .username(analysis.getUsername())
                .platform(analysis.getPlatform())
                .profileUrl(analysis.getProfileUrl())
                .content(analysis.getContent())
                .overallScore(analysis.getOverallScore())
                .riskLevel(analysis.getRiskLevel())
                .totalRisksDetected(analysis.getTotalRisksDetected())
                .riskExplanation(explanations)
                .detectedRisks(risks)
                .recommendations(recs)
                .createdAt(analysis.getCreatedAt())
                .build();

        return ApiResponse.success("Analysis details retrieved.", detail);
    }

    @Transactional
    public ApiResponse<String> deleteAnalysis(Long analysisId, String adminEmail) {
        Analysis analysis = analysisRepository.findById(analysisId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Analysis not found."));

        analysisRepository.delete(analysis);

        recordAudit(adminEmail, "ANALYSIS_DELETED",
                "Deleted analysis ID " + analysisId + " for user ID " + analysis.getUser().getId(),
                "ANALYSIS", analysisId);

        return ApiResponse.success("Analysis deleted successfully.");
    }

    // ══════════════════════════════════════════════════════════
    // Risk Monitoring
    // ══════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public ApiResponse<AdminRiskMonitoringStats> getRiskMonitoringStats() {
        long total = detectedRiskRepository.count();
        long high = detectedRiskRepository.countByRiskLevel("HIGH");
        long medium = detectedRiskRepository.countByRiskLevel("MEDIUM");
        long low = detectedRiskRepository.countByRiskLevel("LOW");

        List<RiskCategoryCount> categoryDist = buildCategoryCounts(
                detectedRiskRepository.countByCategoryNative());
        List<RiskCategoryCount> highRiskByCat = buildCategoryCounts(
                detectedRiskRepository.countHighRiskByCategoryNative());

        LocalDateTime trendStart = LocalDateTime.now().minusDays(13)
                .withHour(0).withMinute(0).withSecond(0).withNano(0);
        List<Object[]> dailyRisks = detectedRiskRepository.countDailyRisks(trendStart);
        List<TrendPoint> riskTrend = buildTrend(dailyRisks, trendStart, 14);

        List<RiskCategoryCount> mostFrequent = categoryDist.stream().limit(6).toList();

        AdminRiskMonitoringStats stats = AdminRiskMonitoringStats.builder()
                .totalRiskIndicators(total)
                .highRiskIndicators(high)
                .mediumRiskIndicators(medium)
                .lowRiskIndicators(low)
                .riskCategoryDistribution(categoryDist)
                .highRiskByCategory(highRiskByCat)
                .riskTrend(riskTrend)
                .mostFrequentCategories(mostFrequent)
                .build();

        return ApiResponse.success("Risk monitoring statistics retrieved.", stats);
    }

    // ══════════════════════════════════════════════════════════
    // AI Service Monitoring
    // ══════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public ApiResponse<AdminAiServiceStatus> getAiServiceStatus() {
        boolean online = aiServiceClient.checkHealth();
        long totalAnalyses = analysisRepository.count();
        long highRisk = analysisRepository.countByRiskLevel("HIGH");
        long mediumRisk = analysisRepository.countByRiskLevel("MEDIUM");
        long lowRisk = analysisRepository.countByRiskLevel("LOW");

        LocalDateTime last24h = LocalDateTime.now().minusHours(24);
        long recentCount = analysisRepository.countByCreatedAtBetween(last24h, LocalDateTime.now());

        AdminAiServiceStatus status = AdminAiServiceStatus.builder()
                .serviceOnline(online)
                .serviceUrl("http://localhost:8000")
                .totalAnalysesProcessed(totalAnalyses)
                .successfulAnalyses(totalAnalyses)
                .highRiskAnalyses(highRisk)
                .mediumRiskAnalyses(mediumRisk)
                .lowRiskAnalyses(lowRisk)
                .recentAnalysesCount(recentCount)
                .build();

        return ApiResponse.success("AI service status retrieved.", status);
    }

    // ══════════════════════════════════════════════════════════
    // Recommendation Management
    // ══════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public ApiResponse<PageResponse<AdminRecommendationDto>> getRecommendations(
            String search, String priority, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size);
        String priorityParam = (priority != null && !priority.isBlank()) ? priority.toUpperCase() : null;
        String searchParam = (search != null && !search.isBlank()) ? search.trim() : null;

        Page<Recommendation> recPage = recommendationRepository.findForAdmin(priorityParam, searchParam, pageRequest);

        List<AdminRecommendationDto> recs = recPage.getContent().stream()
                .map(r -> AdminRecommendationDto.builder()
                        .id(r.getId())
                        .analysisId(r.getAnalysis().getId())
                        .userName(r.getAnalysis().getUser().getFullName())
                        .riskCategory(extractCategory(r.getAnalysis().getDetectedRisks()))
                        .priority(r.getPriority())
                        .text(r.getText())
                        .createdAt(r.getAnalysis().getCreatedAt())
                        .build())
                .toList();

        PageResponse<AdminRecommendationDto> response = PageResponse.<AdminRecommendationDto>builder()
                .content(recs)
                .currentPage(recPage.getNumber())
                .totalPages(recPage.getTotalPages())
                .totalElements(recPage.getTotalElements())
                .pageSize(recPage.getSize())
                .build();

        return ApiResponse.success("Recommendations retrieved.", response);
    }

    @Transactional
    public ApiResponse<AdminRecommendationDto> createRecommendation(AdminSaveRecommendationRequest request, String adminEmail) {
        Analysis analysis;
        if (request.getAnalysisId() != null) {
            analysis = analysisRepository.findById(request.getAnalysisId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Analysis not found."));
        } else {
            // Pick latest analysis if not specified
            List<Analysis> all = analysisRepository.findAll();
            if (all.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No analysis available to link recommendation.");
            }
            analysis = all.get(all.size() - 1);
        }

        Recommendation rec = Recommendation.builder()
                .analysis(analysis)
                .priority(request.getPriority().toUpperCase())
                .text(request.getText().trim())
                .build();

        rec = recommendationRepository.save(rec);

        recordAudit(adminEmail, "RECOMMENDATION_CREATED",
                "Created recommendation ID " + rec.getId() + " (" + rec.getPriority() + ")",
                "RECOMMENDATION", rec.getId());

        AdminRecommendationDto dto = AdminRecommendationDto.builder()
                .id(rec.getId())
                .analysisId(analysis.getId())
                .userName(analysis.getUser() != null ? analysis.getUser().getFullName() : "System")
                .riskCategory(extractCategory(analysis.getDetectedRisks()))
                .priority(rec.getPriority())
                .text(rec.getText())
                .createdAt(analysis.getCreatedAt())
                .build();

        return ApiResponse.success("Recommendation created successfully.", dto);
    }

    @Transactional
    public ApiResponse<AdminRecommendationDto> updateRecommendation(Long id, AdminSaveRecommendationRequest request, String adminEmail) {
        Recommendation rec = recommendationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recommendation not found."));

        rec.setPriority(request.getPriority().toUpperCase());
        rec.setText(request.getText().trim());

        rec = recommendationRepository.save(rec);

        recordAudit(adminEmail, "RECOMMENDATION_UPDATED",
                "Updated recommendation ID " + rec.getId() + " (" + rec.getPriority() + ")",
                "RECOMMENDATION", rec.getId());

        Analysis analysis = rec.getAnalysis();
        AdminRecommendationDto dto = AdminRecommendationDto.builder()
                .id(rec.getId())
                .analysisId(analysis != null ? analysis.getId() : null)
                .userName(analysis != null && analysis.getUser() != null ? analysis.getUser().getFullName() : "System")
                .riskCategory(analysis != null ? extractCategory(analysis.getDetectedRisks()) : "GENERAL")
                .priority(rec.getPriority())
                .text(rec.getText())
                .createdAt(analysis != null ? analysis.getCreatedAt() : null)
                .build();

        return ApiResponse.success("Recommendation updated successfully.", dto);
    }

    @Transactional
    public ApiResponse<String> deleteRecommendation(Long id, String adminEmail) {
        Recommendation rec = recommendationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recommendation not found."));

        recommendationRepository.delete(rec);

        recordAudit(adminEmail, "RECOMMENDATION_DELETED",
                "Deleted recommendation ID " + id,
                "RECOMMENDATION", id);

        return ApiResponse.success("Recommendation deleted successfully.");
    }

    // ══════════════════════════════════════════════════════════
    // Reports
    // ══════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public ApiResponse<PageResponse<AdminAnalysisDto>> getReportData(
            LocalDate startDate, LocalDate endDate, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        if (startDate != null && endDate != null) {
            LocalDateTime start = startDate.atStartOfDay();
            LocalDateTime end = endDate.plusDays(1).atStartOfDay();
            List<Analysis> filtered = analysisRepository.findAll().stream()
                    .filter(a -> a.getCreatedAt() != null
                            && !a.getCreatedAt().isBefore(start)
                            && a.getCreatedAt().isBefore(end))
                    .sorted((a1, a2) -> a2.getCreatedAt().compareTo(a1.getCreatedAt()))
                    .toList();
            int startIdx = page * size;
            int endIdx = Math.min(startIdx + size, filtered.size());
            List<AdminAnalysisDto> content = filtered.subList(startIdx, endIdx).stream()
                    .map(this::toAdminAnalysisDto)
                    .toList();
            PageResponse<AdminAnalysisDto> response = PageResponse.<AdminAnalysisDto>builder()
                    .content(content)
                    .currentPage(page)
                    .totalPages((int) Math.ceil((double) filtered.size() / size))
                    .totalElements(filtered.size())
                    .pageSize(size)
                    .build();
            return ApiResponse.success("Report data retrieved.", response);
        }

        Page<Analysis> analysisPage = analysisRepository.findAllWithUsers(pageRequest);
        List<AdminAnalysisDto> analyses = analysisPage.getContent().stream()
                .map(this::toAdminAnalysisDto)
                .toList();

        PageResponse<AdminAnalysisDto> response = PageResponse.<AdminAnalysisDto>builder()
                .content(analyses)
                .currentPage(analysisPage.getNumber())
                .totalPages(analysisPage.getTotalPages())
                .totalElements(analysisPage.getTotalElements())
                .pageSize(analysisPage.getSize())
                .build();

        return ApiResponse.success("Report data retrieved.", response);
    }

    // ══════════════════════════════════════════════════════════
    // Audit Logs
    // ══════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public ApiResponse<PageResponse<AuditLogDto>> getAuditLogs(String actionType, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        String actionParam = (actionType != null && !actionType.isBlank()) ? actionType.toUpperCase() : null;

        Page<AuditLog> logPage = auditLogRepository.findAllFiltered(actionParam, pageRequest);

        List<AuditLogDto> logs = logPage.getContent().stream()
                .map(this::toAuditLogDto)
                .toList();

        PageResponse<AuditLogDto> response = PageResponse.<AuditLogDto>builder()
                .content(logs)
                .currentPage(logPage.getNumber())
                .totalPages(logPage.getTotalPages())
                .totalElements(logPage.getTotalElements())
                .pageSize(logPage.getSize())
                .build();

        return ApiResponse.success("Audit logs retrieved.", response);
    }

    // ══════════════════════════════════════════════════════════
    // Admin Profile
    // ══════════════════════════════════════════════════════════

    private User findAdminUser(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Admin identifier not provided.");
        }
        return userRepository.findByEmail(identifier)
                .or(() -> userRepository.findByPhoneNumber(identifier))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Admin not found."));
    }

    @Transactional(readOnly = true)
    public ApiResponse<AdminProfileDto> getProfile(String adminEmail) {
        User admin = findAdminUser(adminEmail);

        AdminProfileDto profile = AdminProfileDto.builder()
                .id(admin.getId())
                .fullName(admin.getFullName())
                .email(admin.getEmail())
                .role(admin.getIsAdmin() != null && admin.getIsAdmin() == 1 ? "Admin" : "User")
                .createdAt(admin.getCreatedAt())
                .build();

        return ApiResponse.success("Profile retrieved.", profile);
    }

    @Transactional
    public ApiResponse<AdminProfileDto> updateProfile(UpdateProfileRequest request, String adminEmail) {
        User admin = findAdminUser(adminEmail);

        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            admin.setFullName(request.getFullName().trim());
        }

        userRepository.save(admin);
        recordAudit(adminEmail, "PROFILE_UPDATED", "Admin profile updated", "ADMIN", admin.getId());

        return getProfile(adminEmail);
    }

    @Transactional
    public ApiResponse<String> changePassword(ChangePasswordRequest request, String adminEmail) {
        User admin = findAdminUser(adminEmail);

        if (admin.getPassword() == null
                || !passwordEncoder.matches(request.getCurrentPassword(), admin.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is incorrect.");
        }

        admin.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(admin);

        recordAudit(adminEmail, "PASSWORD_CHANGED", "Admin password changed", "ADMIN", admin.getId());

        return ApiResponse.success("Password changed successfully.");
    }

    // ══════════════════════════════════════════════════════════
    // Audit Recording
    // ══════════════════════════════════════════════════════════

    @Transactional
    public void recordAudit(String adminEmail, String actionType, String description,
            String targetType, Long targetId) {
        try {
            AuditLog auditEntry = AuditLog.builder()
                    .adminEmail(adminEmail != null ? adminEmail : "system")
                    .actionType(actionType)
                    .description(description)
                    .targetType(targetType)
                    .targetId(targetId)
                    .build();
            auditLogRepository.save(auditEntry);
        } catch (Exception e) {
            log.warn("Failed to record audit log: {}", e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════
    // Helpers / Mappers
    // ══════════════════════════════════════════════════════════

    private AdminUserDto toAdminUserDto(User u) {
        return AdminUserDto.builder()
                .id(u.getId())
                .fullName(u.getFullName())
                .email(u.getEmail())
                .phoneNumber(u.getPhoneNumber())
                .username(u.getUsername())
                .occupation(u.getOccupation())
                .city(u.getCity())
                .isAdmin(u.getIsAdmin() != null ? u.getIsAdmin() : 0)
                .role(u.getIsAdmin() != null && u.getIsAdmin() == 1 ? "Admin" : "User")
                .isActive(u.isActive())
                .isVerified(u.isVerified())
                .createdAt(u.getCreatedAt())
                .updatedAt(u.getUpdatedAt())
                .build();
    }

    private AdminAnalysisDto toAdminAnalysisDto(Analysis a) {
        return AdminAnalysisDto.builder()
                .id(a.getId())
                .userId(a.getUser() != null ? a.getUser().getId() : null)
                .userName(a.getUser() != null ? a.getUser().getFullName() : "Unknown")
                .userEmail(a.getUser() != null ? a.getUser().getEmail() : "N/A")
                .username(a.getUsername())
                .platform(a.getPlatform())
                .profileUrl(a.getProfileUrl())
                .riskLevel(a.getRiskLevel())
                .overallScore(a.getOverallScore())
                .totalRisksDetected(a.getTotalRisksDetected())
                .createdAt(a.getCreatedAt())
                .build();
    }

    private RecentAnalysisDto toRecentAnalysisDto(Analysis a) {
        return RecentAnalysisDto.builder()
                .id(a.getId())
                .userName(a.getUser() != null ? a.getUser().getFullName() : "Unknown")
                .userEmail(a.getUser() != null ? a.getUser().getEmail() : "N/A")
                .platform(a.getPlatform())
                .username(a.getUsername())
                .riskLevel(a.getRiskLevel())
                .overallScore(a.getOverallScore())
                .totalRisksDetected(a.getTotalRisksDetected())
                .createdAt(a.getCreatedAt())
                .build();
    }

    private AuditLogDto toAuditLogDto(AuditLog l) {
        return AuditLogDto.builder()
                .id(l.getId())
                .adminEmail(l.getAdminEmail())
                .actionType(l.getActionType())
                .description(l.getDescription())
                .targetType(l.getTargetType())
                .targetId(l.getTargetId())
                .createdAt(l.getCreatedAt())
                .build();
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) return phone;
        return "****" + phone.substring(phone.length() - 4);
    }

    private String extractCategory(List<DetectedRisk> risks) {
        if (risks == null || risks.isEmpty()) return "General";
        return risks.get(0).getCategory();
    }

    private List<TrendPoint> buildTrend(List<Object[]> rows, LocalDateTime start, int days) {
        List<TrendPoint> trend = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            LocalDateTime day = start.plusDays(i);
            String label = day.toLocalDate().toString();
            long count = 0;
            for (Object[] row : rows) {
                if (row[0] != null && row[0].toString().equals(label)) {
                    count = row[1] != null ? ((Number) row[1]).longValue() : 0;
                    break;
                }
            }
            trend.add(TrendPoint.builder().date(label).count(count).build());
        }
        return trend;
    }

    private List<RiskCategoryCount> buildCategoryCounts(List<Object[]> rows) {
        List<RiskCategoryCount> counts = new ArrayList<>();
        if (rows == null) return counts;
        for (Object[] row : rows) {
            if (row[0] != null) {
                counts.add(RiskCategoryCount.builder()
                        .category(row[0].toString())
                        .count(row[1] != null ? ((Number) row[1]).longValue() : 0)
                        .build());
            }
        }
        return counts;
    }
}
