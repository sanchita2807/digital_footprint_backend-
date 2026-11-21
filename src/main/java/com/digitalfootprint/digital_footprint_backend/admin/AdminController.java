package com.digitalfootprint.digital_footprint_backend.admin;

import com.digitalfootprint.digital_footprint_backend.admin.dto.*;
import com.digitalfootprint.digital_footprint_backend.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * All admin REST endpoints. Every endpoint except /login requires ROLE_ADMIN.
 * The JWT filter extracts the admin's email as the authentication principal.
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    // ─── Authentication ─────────────────────────────────────────

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AdminAuthResponse>> login(@Valid @RequestBody AdminLoginRequest request) {
        return ResponseEntity.ok(adminService.login(request));
    }

    @PostMapping("/logout")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> logout(@AuthenticationPrincipal String adminEmail) {
        adminService.logout(adminEmail);
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully."));
    }

    // ─── Dashboard ─────────────────────────────────────────────

    @GetMapping({"/dashboard", "/statistics"})
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AdminDashboardStats>> getDashboardStats() {
        return ResponseEntity.ok(adminService.getDashboardStats());
    }

    @GetMapping("/recent-registrations")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<AdminUserDto>>> getRecentRegistrations(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(adminService.getRecentRegistrations(limit));
    }

    @GetMapping("/recent-activities")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<AuditLogDto>>> getRecentActivities(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(adminService.getRecentActivities(limit));
    }

    // ─── User Management ───────────────────────────────────────

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<AdminUserDto>>> getUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(adminService.getUsers(search, status, page, size));
    }

    @GetMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AdminUserDetailDto>> getUserDetail(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.getUserDetail(id));
    }

    @PatchMapping("/users/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AdminUserDto>> updateUserStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserStatusRequest request,
            @AuthenticationPrincipal String adminEmail) {
        return ResponseEntity.ok(adminService.updateUserStatus(id, request.getIsActive(), adminEmail));
    }

    @PostMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AdminUserDto>> createUser(
            @Valid @RequestBody AdminCreateUserRequest request,
            @AuthenticationPrincipal String adminEmail) {
        return ResponseEntity.ok(adminService.createUser(request, adminEmail));
    }

    @PutMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AdminUserDto>> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody AdminUpdateUserRequest request,
            @AuthenticationPrincipal String adminEmail) {
        return ResponseEntity.ok(adminService.updateUser(id, request, adminEmail));
    }

    @DeleteMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> deleteUser(
            @PathVariable Long id,
            @AuthenticationPrincipal String adminEmail) {
        return ResponseEntity.ok(adminService.deleteUser(id, adminEmail));
    }

    // ─── Analysis Management ──────────────────────────────────

    @GetMapping("/analyses")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<AdminAnalysisDto>>> getAnalyses(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String riskLevel,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(adminService.getAnalyses(search, riskLevel, page, size));
    }

    @GetMapping("/analyses/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AdminAnalysisDetailDto>> getAnalysisDetail(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.getAnalysisDetail(id));
    }

    @DeleteMapping("/analyses/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> deleteAnalysis(
            @PathVariable Long id,
            @AuthenticationPrincipal String adminEmail) {
        return ResponseEntity.ok(adminService.deleteAnalysis(id, adminEmail));
    }

    // ─── Risk Monitoring ───────────────────────────────────────

    @GetMapping({"/risk-monitoring", "/risk-overview"})
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AdminRiskMonitoringStats>> getRiskMonitoring() {
        return ResponseEntity.ok(adminService.getRiskMonitoringStats());
    }

    // ─── AI Service Monitoring ────────────────────────────────

    @GetMapping({"/ai-service/status", "/ai-status"})
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AdminAiServiceStatus>> getAiServiceStatus() {
        return ResponseEntity.ok(adminService.getAiServiceStatus());
    }

    // ─── Recommendation Management ─────────────────────────────

    @GetMapping("/recommendations")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<AdminRecommendationDto>>> getRecommendations(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String priority,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(adminService.getRecommendations(search, priority, page, size));
    }

    @PostMapping("/recommendations")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AdminRecommendationDto>> createRecommendation(
            @Valid @RequestBody AdminSaveRecommendationRequest request,
            @AuthenticationPrincipal String adminEmail) {
        return ResponseEntity.ok(adminService.createRecommendation(request, adminEmail));
    }

    @PutMapping("/recommendations/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AdminRecommendationDto>> updateRecommendation(
            @PathVariable Long id,
            @Valid @RequestBody AdminSaveRecommendationRequest request,
            @AuthenticationPrincipal String adminEmail) {
        return ResponseEntity.ok(adminService.updateRecommendation(id, request, adminEmail));
    }

    @DeleteMapping("/recommendations/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> deleteRecommendation(
            @PathVariable Long id,
            @AuthenticationPrincipal String adminEmail) {
        return ResponseEntity.ok(adminService.deleteRecommendation(id, adminEmail));
    }

    // ─── Reports ───────────────────────────────────────────────

    @GetMapping("/reports/analysis")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<AdminAnalysisDto>>> getAnalysisReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(adminService.getReportData(startDate, endDate, page, size));
    }

    // ─── Audit Logs ────────────────────────────────────────────

    @GetMapping("/audit-logs")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<AuditLogDto>>> getAuditLogs(
            @RequestParam(required = false) String actionType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(adminService.getAuditLogs(actionType, page, size));
    }

    // ─── Admin Profile ─────────────────────────────────────────

    @GetMapping("/profile")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AdminProfileDto>> getProfile(@AuthenticationPrincipal String adminEmail) {
        return ResponseEntity.ok(adminService.getProfile(adminEmail));
    }

    @PutMapping("/profile")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AdminProfileDto>> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            @AuthenticationPrincipal String adminEmail) {
        return ResponseEntity.ok(adminService.updateProfile(request, adminEmail));
    }

    @PutMapping("/profile/password")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal String adminEmail) {
        return ResponseEntity.ok(adminService.changePassword(request, adminEmail));
    }
}
