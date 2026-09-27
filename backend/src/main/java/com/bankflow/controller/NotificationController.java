package com.bankflow.controller;

import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bankflow.dto.response.PageResponse;
import com.bankflow.entity.Notification;
import com.bankflow.service.NotificationService;
import com.bankflow.util.RequestContext;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final RequestContext requestContext;

    @GetMapping
    public ResponseEntity<PageResponse<NotificationView>> list(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {

        return ResponseEntity.ok(PageResponse.from(
                notificationService.forUser(requestContext.requireUserId(), PageRequest.of(page, size)),
                NotificationView::from));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> unreadCount() {
        return ResponseEntity.ok(Map.of(
                "unread", notificationService.unreadCount(requestContext.requireUserId())));
    }

    @PostMapping("/mark-all-read")
    public ResponseEntity<Map<String, Integer>> markAllRead() {
        return ResponseEntity.ok(Map.of(
                "updated", notificationService.markAllRead(requestContext.requireUserId())));
    }

    public record NotificationView(
            Long id, String title, String message, String channel, boolean read,
            java.time.Instant createdAt) {

        static NotificationView from(Notification n) {
            return new NotificationView(
                    n.getId(), n.getTitle(), n.getMessage(), n.getChannel().name(),
                    n.isRead(), n.getCreatedAt());
        }
    }
}
