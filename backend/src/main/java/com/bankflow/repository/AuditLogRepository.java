package com.bankflow.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bankflow.entity.AuditLog;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /** Guards against writing the same event twice on a Kafka redelivery. */
    boolean existsByEventId(String eventId);

    Page<AuditLog> findByActorUserId(Long actorUserId, Pageable pageable);

    Page<AuditLog> findByEntityTypeAndEntityId(
            String entityType, String entityId, Pageable pageable);

    @Query("""
            SELECT a FROM AuditLog a
            WHERE (:action IS NULL OR a.action = :action)
              AND (:entityType IS NULL OR a.entityType = :entityType)
            """)
    Page<AuditLog> filter(
            @Param("action") String action,
            @Param("entityType") String entityType,
            Pageable pageable);
}
