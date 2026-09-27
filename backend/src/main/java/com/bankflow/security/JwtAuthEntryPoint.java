package com.bankflow.security;

import java.io.IOException;

import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.bankflow.dto.response.ApiError;
import com.bankflow.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * Returns the same ApiError shape as every other failure. Without this the
 * filter chain would emit Spring's default HTML error page, which the frontend
 * interceptor could not parse.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException)
            throws IOException {

        ApiError error = ApiError.of(
                ErrorCode.AUTHENTICATION_REQUIRED.name(),
                "Sign in to continue.",
                request.getRequestURI());

        response.setStatus(ErrorCode.AUTHENTICATION_REQUIRED.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), error);
    }
}
