package com.bankflow.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

import com.bankflow.entity.enums.RoleName;
import com.bankflow.security.CustomUserDetailsService;
import com.bankflow.security.JwtAuthEntryPoint;
import com.bankflow.security.JwtAuthenticationFilter;
import com.bankflow.security.RestAccessDeniedHandler;

import lombok.RequiredArgsConstructor;

/**
 * First of three authorization layers. This one is coarse — URL patterns by
 * role. Method-level @PreAuthorize adds the second layer, and repository
 * queries scoped by user id add row-level checks as the third.
 *
 * Defence in depth matters here because a single missed URL pattern would
 * otherwise expose another customer's money.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String ADMIN = RoleName.ROLE_ADMIN.name().replace("ROLE_", "");
    private static final String AUDITOR = RoleName.ROLE_AUDITOR.name().replace("ROLE_", "");

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthEntryPoint authEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;
    private final CustomUserDetailsService userDetailsService;
    private final CorsConfigurationSource corsConfigurationSource;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // No cookies are used for auth, so there is no CSRF surface. The
                // token travels in the Authorization header, which a cross-site
                // form post cannot set.
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(authEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(
                                "/api/health",
                                "/api/auth/register",
                                "/api/auth/login",
                                "/api/auth/refresh")
                        .permitAll()
                        .requestMatchers(
                                "/actuator/health/**",
                                "/actuator/info",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html")
                        .permitAll()
                        // Read-only monitoring is shared by admins and auditors;
                        // everything else under /admin is admin-only.
                        .requestMatchers(HttpMethod.GET, "/api/admin/audit-logs/**", "/api/admin/transactions/**")
                        .hasAnyRole(ADMIN, AUDITOR)
                        .requestMatchers("/api/admin/**").hasRole(ADMIN)
                        .anyRequest().authenticated())
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        // Lets AuthService tell a wrong password apart from a missing account so
        // it can count failed attempts against the right user.
        provider.setHideUserNotFoundExceptions(false);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager() {
        return authenticationProvider()::authenticate;
    }

    /**
     * Strength 12 rather than the default 10 — roughly four times the work per
     * hash, which is still a few hundred milliseconds and only on the login path.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
