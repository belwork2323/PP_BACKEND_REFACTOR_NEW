package in.co.bel.acempp.service;

import in.co.bel.acempp.dto.auth.resetpswd.*;
import in.co.bel.acempp.dto.general.PaginationDto;
import in.co.bel.acempp.enums.ResetRequestStatus;
import in.co.bel.acempp.repository.PasswordResetRequestRepository;
import in.co.bel.acempp.entity.*;
import in.co.bel.acempp.enums.ErrorCode;
import in.co.bel.acempp.exception.AcemException;
import in.co.bel.acempp.repository.*;
import in.co.bel.acempp.util.DateTimeConversionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {

    private final PasswordResetRequestRepository resetRequestRepo;
    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;

    // ─────────────────────────── SUBMIT ───────────────────────────
    public ResetPasswordResponse submitResetRequest(ResetPasswordRequest request) {
        // 1. Check user exists
        User user = userRepo.findByUserId(request.getUserId())
                .orElseThrow(() -> new AcemException(
                        ErrorCode.USER_NOT_FOUND,
                        "User not found with userId: " + request.getUserId()));

        // 2. No duplicate PENDING request
        if (resetRequestRepo.existsByUserIdAndStatus(user.getUserId(), ResetRequestStatus.PENDING)) {
            throw new AcemException(
                    ErrorCode.DUPLICATE_ISSUANCE,
                    "A password reset request is already pending for this user");
        }

        // 3. Generate requestId (e.g. PRR-00001)
        String requestId = String.format("PRR-%05d", resetRequestRepo.count() + 1);

        // 4. Save
        PasswordResetRequest entity = PasswordResetRequest.builder()
                .requestId(requestId)
                .userId(user.getUserId())
                .reason(request.getReason())
                .status(ResetRequestStatus.PENDING)
                .requestedAt(LocalDateTime.now())
                .build();

        resetRequestRepo.save(entity);
        log.info("Password reset request {} created for user {}", requestId, user.getUserId());

        // 5. Response
        return ResetPasswordResponse.builder()
                .request(PasswordRequestDto.builder()
                        .requestId(requestId)
                        .userId(user.getUserId())
                        .reason(request.getReason())
                        .status(ResetRequestStatus.PENDING.name())
                        .requestedAt(entity.getRequestedAt())
                        .build())
                .build();
    }

    // ─────────────────────────── LIST (with filters) ───────────────────────────
    public PasswordResetListResponse getResetRequests(PasswordResetListRequest request) {
        LocalDateTime start = parseNullableDate(request.getStartDate());
        LocalDateTime end   = parseNullableDate(request.getEndDate());

        String statusParam = (request.getStatus() == null || request.getStatus().isBlank())
                ? "ALL"
                : request.getStatus().toUpperCase();

        if (!List.of("ALL", "PENDING", "APPROVED", "REJECTED").contains(statusParam)) {
            throw new AcemException(ErrorCode.INVALID_REQUEST,
                    "Invalid status. Allowed: ALL, PENDING, APPROVED, REJECTED");
        }

        Pageable pageable = PageRequest.of(
                Math.max(0, request.getPage() - 1),
                request.getPageSize());

        Page<PasswordResetRequest> page = resetRequestRepo.findRequestsWithFilters(
                request.getSearch(),
                statusParam,
                start,
                end,
                pageable);

        // Summary counts
        ResetSummaryDto summary = ResetSummaryDto.builder()
                .all(resetRequestRepo.count())
                .pending(resetRequestRepo.countByStatus(ResetRequestStatus.PENDING))
                .approved(resetRequestRepo.countByStatus(ResetRequestStatus.APPROVED))
                .rejected(resetRequestRepo.countByStatus(ResetRequestStatus.REJECTED))
                .build();

        List<PasswordResetRequestDto> dtos = page.getContent().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        return PasswordResetListResponse.builder()
                .summary(summary)
                .requests(dtos)
                .pagination(PaginationDto.builder()
                        .page(request.getPage())
                        .pageSize(request.getPageSize())
                        .totalRecords(page.getTotalElements())
                        .totalPages(page.getTotalPages())
                        .build())
                .build();
    }

    // ─────────────────────────── CHANGE STATUS ───────────────────────────
    public ChangeResetStatusResponse changeStatus(ChangeResetStatusRequest request,
                                                  String adminUserId,
                                                  String adminUsername) {
        PasswordResetRequest resetReq = resetRequestRepo
                .findByRequestId(request.getRequestId())
                .orElseThrow(() -> new AcemException(ErrorCode.RESOURCE_NOT_FOUND,
                        "Password reset request not found: " + request.getRequestId()));

        if (resetReq.getStatus() != ResetRequestStatus.PENDING) {
            throw new AcemException(ErrorCode.INVALID_ACTION,
                    "Request is already " + resetReq.getStatus().name());
        }

        String action = request.getAction().toUpperCase();
        if (!action.equals("APPROVE") && !action.equals("REJECT")) {
            throw new AcemException(ErrorCode.INVALID_REQUEST,
                    "Action must be APPROVE or REJECT");
        }

        ResetRequestStatus newStatus = action.equals("APPROVE")
                ? ResetRequestStatus.APPROVED
                : ResetRequestStatus.REJECTED;

        resetReq.setStatus(newStatus);
        resetReq.setRemarks(request.getRemarks());
        resetReq.setReviewedByUserId(adminUserId);
        resetReq.setReviewedByName(adminUsername);
        resetReq.setResolvedAt(LocalDateTime.now());
        resetRequestRepo.save(resetReq);

        String temporaryPassword = null;

        // If APPROVED → reset to standard temp password and force change
        if (newStatus == ResetRequestStatus.APPROVED) {
            User user = userRepo.findByUserId(resetReq.getUserId())
                    .orElseThrow(() -> new AcemException(ErrorCode.USER_NOT_FOUND,
                            "User not found"));
            temporaryPassword = in.co.bel.acempp.util.PasswordPolicy.generateDefaultPassword(user.getUserId());
            user.setPasswordHistory(
                    in.co.bel.acempp.util.PasswordPolicy.pushHistory(
                            user.getPasswordHistory(), user.getPassword()));
            user.setPassword(passwordEncoder.encode(temporaryPassword));
            user.setMustChangePassword(true);
            user.setTemporaryPassword(temporaryPassword);
            user.setUpdatedAt(LocalDateTime.now());
            user.setUpdatedBy(adminUserId);
            userRepo.save(user);
            log.info("Password reset approved for user {} by admin {}",
                    user.getUserId(), adminUsername);
        }

        log.info("Reset request {} {} by admin {}",
                request.getRequestId(), action, adminUsername);

        return ChangeResetStatusResponse.builder()
                .requestId(resetReq.getRequestId())
                .status(newStatus.name())
                .reviewedBy(ReviewedByDto.builder()
                        .userId(adminUserId)
                        .name(adminUsername)
                        .build())
                .reviewedAt(resetReq.getResolvedAt())
                .remarks(request.getRemarks())
                .temporaryPassword(temporaryPassword)
                .build();
    }

    // ─────────────────────────── HELPERS ───────────────────────────
    private PasswordResetRequestDto mapToDto(PasswordResetRequest r) {
        boolean isReviewed = r.getStatus() != ResetRequestStatus.PENDING;
        String temporaryPassword = null;

        // Show temp password only while approved and user still must change it
        if (r.getStatus() == ResetRequestStatus.APPROVED) {
            temporaryPassword = userRepo.findByUserId(r.getUserId())
                    .filter(u -> Boolean.TRUE.equals(u.getMustChangePassword()))
                    .map(User::getTemporaryPassword)
                    .filter(pw -> pw != null && !pw.isBlank())
                    .orElse(null);
        }

        return PasswordResetRequestDto.builder()
                .requestId(r.getRequestId())
                .userId(r.getUserId())
                .reason(r.getReason())
                .status(r.getStatus().name())
                .requestedAt(r.getRequestedAt())
                .reviewedBy(isReviewed && r.getReviewedByUserId() != null
                        ? ReviewedByDto.builder()
                        .userId(r.getReviewedByUserId())
                        .name(r.getReviewedByName())
                        .build()
                        : null)
                .reviewedAt(isReviewed ? r.getResolvedAt() : null)
                .remarks(isReviewed ? r.getRemarks() : null)
                .temporaryPassword(temporaryPassword)
                .build();
    }

    private LocalDateTime parseNullableDate(String date) {
        if (date == null || date.isBlank()) return null;
        return DateTimeConversionUtil.parseLocalDate(date).atStartOfDay();
    }
}
