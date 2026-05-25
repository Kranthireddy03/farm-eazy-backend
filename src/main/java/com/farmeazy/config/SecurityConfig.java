package com.farmeazy.config;
import org.springframework.context.annotation.Lazy;

import com.farmeazy.service.AuthService;
import com.farmeazy.security.JwtAuthenticationFilter;
import com.farmeazy.security.JwtAuthenticationEntryPoint;
import com.farmeazy.security.JwtUtil;
import com.farmeazy.middleware.RateLimitingFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.DelegatingSecurityContextRepository;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;

import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.security.config.Customizer.withDefaults;
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtUtil jwtUtil;
    private final RateLimitingFilter rateLimitingFilter;

    @Value("${cors.allowed-origins:}")
    private String allowedOrigins;

    @Value("${spring.h2.console.enabled:false}")
    private boolean h2ConsoleEnabled;

    @Value("${springdoc.api-docs.enabled:false}")
    private boolean apiDocsEnabled;

    @Value("${springdoc.swagger-ui.enabled:false}")
    private boolean swaggerEnabled;

    @Value("${security.headers.csp:default-src 'self'; connect-src 'self' https://farm-eazy-backend.onrender.com https://www.googleapis.com; img-src 'self' data: https:; script-src 'self' 'unsafe-inline' https://accounts.google.com; style-src 'self' 'unsafe-inline'; frame-ancestors 'none';}")
    private String contentSecurityPolicy;

    public SecurityConfig(JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
                          JwtUtil jwtUtil,
                          RateLimitingFilter rateLimitingFilter) {
        this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
        this.jwtUtil = jwtUtil;
        this.rateLimitingFilter = rateLimitingFilter;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(@Lazy AuthService authService) {
        return new JwtAuthenticationFilter(jwtUtil, authService);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        http.cors(withDefaults())
            .csrf(csrf -> csrf.disable())
            .headers(headers -> headers
                .frameOptions(frameOptions -> frameOptions.sameOrigin())
                .contentTypeOptions(withDefaults())
                .xssProtection(withDefaults())
                .contentSecurityPolicy(csp -> csp.policyDirectives(contentSecurityPolicy))
                .httpStrictTransportSecurity(hsts -> hsts
                    .includeSubDomains(true)
                    .preload(true)
                    .maxAgeInSeconds(31536000))
                .cacheControl(withDefaults())
                .referrerPolicy(referrer -> referrer.policy(org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER)))
            .exceptionHandling(exceptionHandling -> exceptionHandling
                .authenticationEntryPoint(jwtAuthenticationEntryPoint))
            .sessionManagement(sessionManagement -> sessionManagement
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .securityContext(securityContext -> securityContext
                .requireExplicitSave(false))
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/", "/favicon.ico", "/health").permitAll()
                .requestMatchers("/api/auth/login").permitAll()
                .requestMatchers("/api/auth/register").permitAll()
                .requestMatchers("/api/auth/register/availability").permitAll()
                .requestMatchers("/api/auth/forgot-password").permitAll()
                .requestMatchers("/api/auth/reset-password").permitAll()
                .requestMatchers("/api/auth/r/**").permitAll()
                .requestMatchers("/api/auth/request-otp").permitAll()
                .requestMatchers("/api/auth/verify-otp").permitAll()
                .requestMatchers("/api/auth/login/request-otp").permitAll()
                .requestMatchers("/api/auth/login/preview-user").permitAll()
                .requestMatchers("/api/auth/login/verify-otp").permitAll()
                .requestMatchers("/api/auth/login/change-password-otp").permitAll()
                .requestMatchers("/api/auth/google").permitAll()
                .requestMatchers("/api/auth/google/register").permitAll()
                .requestMatchers("/api/auth/google/complete-profile").authenticated()
                .requestMatchers("/api/auth/google/defer-profile").authenticated()
                .requestMatchers("/api/auth/suggest-username").permitAll()
                .requestMatchers("/api/auth/refresh").permitAll()
                .requestMatchers("/api/auth/logout").permitAll()
                .requestMatchers("/api/otp/**").permitAll()
                .requestMatchers("/api/push/vapid-key").permitAll()
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/swagger-ui/*.css", "/swagger-ui/*.js", "/swagger-ui/*.png").access((authentication, context) -> new AuthorizationDecision(apiDocsEnabled && swaggerEnabled))
                .requestMatchers("/h2-console/**").access((authentication, context) -> new AuthorizationDecision(h2ConsoleEnabled))
                .requestMatchers(HttpMethod.GET, "/api/public/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/public/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/public/support-message").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/admin/faq-questions/stream").permitAll()
                .requestMatchers("/api/admin/**").hasAnyRole("ADMIN", "SUPERADMIN")
                .requestMatchers(HttpMethod.POST, "/api/payment/webhook").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/razorpay/webhook/bank-verification").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/products/media/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/uploads/**").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/faq-questions").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/faq-question/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/faq-question/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/faq/question").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/public/faq-question").permitAll()
                .requestMatchers("/api/test-email/**").permitAll()
                .anyRequest().authenticated()
            );

        http.addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * CORS configuration bean.
     * 
     * Allows cross-origin requests from frontend applications:
     * - localhost:4200 (Angular development server)
     * - localhost:3000 (React/Node development server)
     * 
     * Headers Allowed:
     * - All headers (*) including Authorization, Content-Type, etc.
     * 
     * Methods Allowed:
     * - GET, POST, PUT, DELETE, OPTIONS
     * 
     * Credentials:
     * - Cookies and Authorization headers allowed (allowCredentials: true)
     * 
     * Pre-flight Cache:
     * - Browser caches CORS policy for 3600 seconds (1 hour)
     * - Reduces OPTIONS requests for repeated API calls
     * 
     * @return Configured CORS source
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        List<String> exactOrigins = Arrays.stream(String.valueOf(allowedOrigins == null ? "" : allowedOrigins).split(","))
            .map(String::trim)
            .filter(origin -> !origin.isBlank())
            .collect(Collectors.toList());
        configuration.setAllowedOrigins(exactOrigins);
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH", "HEAD"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "Accept", "Origin", "X-Requested-With", "X-Gateway-Client", "X-Gateway-Timestamp", "X-Gateway-Signature", "X-Request-Nonce", "X-User-Location"));
        configuration.setExposedHeaders(Arrays.asList("Authorization", "Content-Type"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // CORS for API routes and root-level support/FAQ endpoints used by the support portal.
        source.registerCorsConfiguration("/api/**", configuration);
        source.registerCorsConfiguration("/support-tickets/**", configuration);
        source.registerCorsConfiguration("/faq/**", configuration);
        source.registerCorsConfiguration("/faq-question/**", configuration);
        source.registerCorsConfiguration("/faq-questions/**", configuration);
        source.registerCorsConfiguration("/notifications/**", configuration);
        return source;
    }
}
