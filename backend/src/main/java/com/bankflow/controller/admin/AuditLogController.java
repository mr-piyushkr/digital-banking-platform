package com.bankflow.controller.admin;

import java.time.Instant;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bankflow.dto.response.PageResponse;
import com.bankflow.entity.AuditLog;
import com.bankflow.repository.AuditLogRepository;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

/**
 * Read-only by design — there is no endpoint to create, edit or delete an audit
 * entry. Rows arrive only from the audit event handler.
 */
@RestController
@RequestMapping("/api/admin/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogRepository auditLogRepository;

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<PageResponse<AuditLogView>> list(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entityType,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "25") @Min(1) @Max(100) int size) {

        var result = auditLogRepository.filter(
                blankToNull(action),
                blankToNull(entityType),
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));

        return ResponseEntity.ok(PageResponse.from(result, AuditLogView::from));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public record AuditLogView(
            Long id,
            Long actorUserId,
            String action,
            String entityType,
            String entityId,
            String details,
            String ipAddress,
            Instant createdAt) {

        static AuditLogView from(AuditLog log) {
            return new AuditLogView(
                    log.getId(),
                    log.getActorUserId(),
                    log.getAction(),
                    log.getEntityType(),
                    log.getEntityId(),
                    log.getDetails(),
                    log.getIpAddress(),
                    log.getCreatedAt());
        }
    }
}
