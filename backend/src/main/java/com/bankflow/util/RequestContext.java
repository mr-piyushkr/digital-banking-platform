package com.bankflow.util;

import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.bankflow.exception.BusinessException;
import com.bankflow.exception.ErrorCode;
import com.bankflow.security.CustomUserDetails;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Reads the authenticated principal and request metadata. Services depend on
 * this rather than on SecurityContextHolder directly, which keeps them testable
 * with a plain stub.
 */
@Component
public class RequestContext {

    public Optional<CustomUserDetails> currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        return authentication.getPrincipal() instanceof CustomUserDetails details
                ? Optional.of(details)
                : Optional.empty();
    }

    public Long requireUserId() {
        return currentUser()
                .map(CustomUserDetails::getUserId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.AUTHENTICATION_REQUIRED, "No authenticated user."));
    }

    public Optional<Long> currentUserId() {
        return currentUser().map(CustomUserDetails::getUserId);
    }

    /** Honours X-Forwarded-For so the audit log records the real client behind a proxy. */
    public String clientIp() {
        return request()
                .map(req -> {
                    String forwarded = req.getHeader("X-Forwarded-For");
                    if (forwarded != null && !forwarded.isBlank()) {
                        return forwarded.split(",")[0].trim();
                    }
                    return req.getRemoteAddr();
                })
                .orElse(null);
    }

    public String userAgent() {
        return request().map(req -> req.getHeader("User-Agent")).orElse(null);
    }

    private Optional<HttpServletRequest> request() {
        return RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs
                ? Optional.of(attrs.getRequest())
                : Optional.empty();
    }
}
