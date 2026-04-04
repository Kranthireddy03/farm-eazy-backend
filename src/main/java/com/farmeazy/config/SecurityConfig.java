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

import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.DelegatingSecurityContextRepository;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;

import lombok.RequiredArgsConstructor;

import java.util.Arrays;

import static org.springframework.security.config.Customizer.withDefaults;
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtUtil jwtUtil;
    private final RateLimitingFilter rateLimitingFilter;

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
            .headers(headers -> headers.frameOptions(frameOptions -> frameOptions.sameOrigin()))
            .exceptionHandling(exceptionHandling -> exceptionHandling
                .authenticationEntryPoint(jwtAuthenticationEntryPoint))
            .sessionManagement(sessionManagement -> sessionManagement
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .securityContext(securityContext -> securityContext
                .requireExplicitSave(false))
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/", "/favicon.ico", "/health").permitAll()
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/otp/**").permitAll()
                .requestMatchers("/api/push/vapid-key").permitAll()
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/swagger-ui/*.css", "/swagger-ui/*.js", "/swagger-ui/*.png").permitAll()
                .requestMatchers("/h2-console/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/public/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/public/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/public/support-message").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/products/media/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/uploads/**").denyAll()
                .requestMatchers(HttpMethod.GET, "/api/faq-questions").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/faq-question/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/faq-question/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/admin/faq-questions/stream").permitAll()
                .requestMatchers(HttpMethod.POST, "/support-tickets/guest").permitAll()
                .requestMatchers(HttpMethod.GET, "/support-tickets/public/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/support-tickets/public/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/support-tickets/public/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/support-tickets/public/**").permitAll()
                // Frontends may prefix the API with /api; allow both variants for guest ticket creation
                .requestMatchers(HttpMethod.POST, "/api/support-tickets/guest").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/faq/question").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/public/faq-question").permitAll()
                .requestMatchers("/api/test-email/**").permitAll()
                .anyRequest().authenticated()
            );

        http.addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterAfter(jwtAuthenticationFilter, org.springframework.security.web.context.SecurityContextHolderFilter.class);

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
        configuration.setAllowedOriginPatterns(Arrays.asList(
            "https://farm-eazy.com",
            "https://www.farm-eazy.com",
            "https://*.vercel.app",
            "https://farm-eazy-backend.onrender.com",
            "http://localhost:4200",
            "http://localhost:3000",
            "http://localhost:3001",
            "http://localhost:5173"
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH", "HEAD"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setExposedHeaders(Arrays.asList("Authorization", "Content-Type"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
