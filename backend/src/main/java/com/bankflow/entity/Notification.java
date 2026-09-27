package com.bankflow.entity;

import com.bankflow.entity.enums.NotificationChannel;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "notifications",
        indexes = {
            @Index(name = "idx_notifications_user_read", columnList = "user_id, read_flag"),
            @Index(name = "idx_notifications_created", columnList = "created_at")
        })
public class Notification extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "title", nullable = false, length = 120)
    private String title;

    @Column(name = "message", nullable = false, length = 500)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 16)
    private NotificationChannel channel = NotificationChannel.IN_APP;

    @Column(name = "read_flag", nullable = false)
    private boolean read = false;

    /** Deduplicates replayed Kafka messages. */
    @Column(name = "event_id", unique = true, length = 64)
    private String eventId;
}
