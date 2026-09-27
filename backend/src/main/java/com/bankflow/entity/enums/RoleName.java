package com.bankflow.entity.enums;

/**
 * Stored with the ROLE_ prefix because Spring Security's hasRole() expects it.
 */
public enum RoleName {
    ROLE_CUSTOMER,
    ROLE_ADMIN,
    ROLE_AUDITOR
}
