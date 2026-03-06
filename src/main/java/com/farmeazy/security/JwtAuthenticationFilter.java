
package com.farmeazy.security;

import com.farmeazy.service.AuthService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.core.Ordered;

import java.io.IOException;

/**
 * JWT AUTHENTICATION FILTER - REQUEST INTERCEPTOR
 * 
 * PURPOSE: Spring Security filter that intercepts HTTP requests and validates JWT tokens.
 * Executes once per request (enforced by OncePerRequestFilter parent class).
 * Populates SecurityContext with authenticated user if token valid.
 * 
 * FILTER CHAIN POSITION:
 * ┌────────────────────────────────────────────────────────────────┐
 * │ 1. CORS Filter (validate origin, headers)                      │
 * │ 2. JWT AUTHENTICATION FILTER (THIS CLASS) ← You are here       │
 * │ 3. Servlet Dispatch Filter                                     │
 * │ 4. Authorization Filter (@PreAuthorize checks)                 │
 * │ 5. Controller (Request Handler)                                │
 * └────────────────────────────────────────────────────────────────┘
 * 
 * REQUEST PROCESSING FLOW:
 * ┌──────────────────────────────────────────────────────────────┐
 * │ 1. HTTP Request arrives with Authorization header             │
 * │    GET /api/farms                                              │
 * │    Header: Authorization: Bearer eyJhbGciOiJIUzUxMiI...       │
 * └──────────────────────────────────────────────────────────────┘
 *                          ↓
 * ┌──────────────────────────────────────────────────────────────┐
 * │ 2. doFilterInternal() is called by servlet container          │
 * └──────────────────────────────────────────────────────────────┘
 *                          ↓
 * ┌──────────────────────────────────────────────────────────────┐
 * │ 3. extractJwtFromRequest() extracts token from header         │
 * │    - Reads Authorization header                               │
 * │    - Checks if starts with "Bearer "                          │
 * │    - Extracts token portion (skip "Bearer " prefix)           │
 * │    - Returns token or null if not found                       │
 * └──────────────────────────────────────────────────────────────┘
 *                          ↓
 * ┌──────────────────────────────────────────────────────────────┐
 * │ 4. Validate token (if present)                                │
 * │    - Check token not empty (StringUtils.hasText(jwt))         │
 * │    - Call jwtUtil.validateToken(jwt)                          │
 * │      - Verifies HMAC signature                                │
 * │      - Checks expiration timestamp                            │
 * │      - Returns true only if both pass                         │
 * └──────────────────────────────────────────────────────────────┘
 *                          ↓
 * ┌──────────────────────────────────────────────────────────────┐
 * │ 5. Extract username and load user (if token valid)            │
 * │    - Call jwtUtil.extractUsername(jwt)                        │
 * │      - Parses JWT claims                                      │
 * │      - Returns user email                                     │
 * │    - Call userDetailsService.loadUserByUsername(email)        │
 * │      - Queries database for user                              │
 * │      - Returns UserDetails with password hash, authorities    │
 * └──────────────────────────────────────────────────────────────┘
 *                          ↓
 * ┌──────────────────────────────────────────────────────────────┐
 * │ 6. Create authentication object                               │
 * │    - New UsernamePasswordAuthenticationToken()                │
 * │      - principal: userDetails                                 │
 * │      - credentials: null (not needed after auth)              │
 * │      - authorities: userDetails.getAuthorities()              │
 * │    - Set details from request (IP, session ID, etc.)          │
 * │      - buildDetails(request) creates WebAuthenticationDetails │
 * └──────────────────────────────────────────────────────────────┘
 *                          ↓
 * ┌──────────────────────────────────────────────────────────────┐
 * │ 7. Set authentication in SecurityContext                      │
 * │    - SecurityContextHolder.getContext().setAuthentication()   │
 * │    - Makes user available to entire request processing        │
 * │    - Controllers can access via @AuthenticationPrincipal      │
 * │    - Services can access via SecurityContext.getContext()     │
 * └──────────────────────────────────────────────────────────────┘
 *                          ↓
 * ┌──────────────────────────────────────────────────────────────┐
 * │ 8. Pass to next filter                                        │
 * │    - filterChain.doFilter(request, response)                  │
 * │    - Continues to AuthorizationFilter, then Controller        │
 * │    - If no exception, controller receives request             │
 * └──────────────────────────────────────────────────────────────┘
 *                          ↓
 * ┌──────────────────────────────────────────────────────────────┐
 * │ 9. Controller handler method executes                         │
 * │    - Has access to authenticated user via SecurityContext     │
 * │    - Can check authorization (user owns resource?)            │
 * │    - Processes request, returns response                      │
 * └──────────────────────────────────────────────────────────────┘
 *                          ↓
 * ┌──────────────────────────────────────────────────────────────┐
 * │ 10. Response returned to client                               │
 * │     - SecurityContext cleared after request (thread pool)     │
 * │     - Next request starts fresh                               │
 * └──────────────────────────────────────────────────────────────┘
 * 
 * ERROR HANDLING:
 * - Try-catch wraps entire process
 * - If JWT extraction fails: Silently continues (token may not exist for public endpoints)
 * - If token validation fails: Request proceeds without authentication
 * - If user loading fails: Request proceeds without authentication
 * - Failed logins → Spring Security authorization filter catches and returns 401
 * - This design allows public endpoints to work alongside authenticated ones
 * 
 * SECURITY PROPERTIES:
 * 1. STATELESS: No server-side session state maintained
 * 2. TOKEN-BASED: Authentication based on JWT signature verification
 * 3. PER-REQUEST: Token validated on every request (no cached login state)
 * 4. SECURE: HMAC prevents token forgery, expires prevent replay attacks
 * 
 * IMPORTANT NOTES:
 * - Extends OncePerRequestFilter to ensure execution exactly once per request
 * - Silently handles exceptions (doesn't break filter chain on JWT errors)
 * - Public endpoints (no token) still work → token optional for some endpoints
 * - Protected endpoints checked by @PreAuthorize or authorization rules
 * - This filter only validates JWT, authorization checked downstream
 * 
 * CONFIGURATION:
 * - Added to SecurityFilterChain in SecurityConfig
 * - Positioned before UsernamePasswordAuthenticationFilter
 * - Receives JwtUtil and UserDetailsService via setter injection
 * 
 * FUTURE ENHANCEMENTS:
 * - Token refresh: Check token expiration, refresh if near expiry
 * - Blacklist checking: Verify token not in logout blacklist
 * - Rate limiting: Count authentication attempts per IP
 * - Audit logging: Log token validations for security analysis
 * 
 * DEBUGGING:
 * - Enable DEBUG logging: logging.level.com.farmeazy.security=DEBUG
 * - Logs successful authentication: "[email@example.com] successfully authenticated"
 * - Logs JWT extraction: "[Authorization header value]"
 * - Logs validation failures: "Could not set user authentication in security context"
 * 
 * @author FarmEazy Development Team
 * @version 1.0.0
 * @since January 2026
 * @see JwtUtil for token validation
 * @see SecurityConfig for filter chain configuration
 */

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter implements Ordered {
        
    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
    private final JwtUtil jwtUtil;
    private final AuthService authService;
    private final RequestAttributeSecurityContextRepository securityContextRepository = new RequestAttributeSecurityContextRepository();

    public JwtAuthenticationFilter(JwtUtil jwtUtil, AuthService authService) {
        this.jwtUtil = jwtUtil;
        this.authService = authService;
    }

    /**
     * Skip JWT filter for public endpoints (auth, swagger, etc.)
     * This prevents any JWT processing on endpoints that don't need authentication.
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/api/auth/") || 
               path.startsWith("/v3/api-docs") || 
               path.startsWith("/swagger-ui") ||
               path.startsWith("/h2-console") ||
               path.equals("/") ||
               path.equals("/health") ||
               path.equals("/favicon.ico");
    }

    /**
     * Main filter method - validates JWT token in request.
     * 
     * Executes once per HTTP request (enforced by OncePerRequestFilter).
     * 
     * Process:
     * 1. Extract JWT token from Authorization header
     * 2. Validate token (signature, expiration)
     * 3. Load user details from database
     * 4. Create authentication object
     * 5. Populate SecurityContext with authenticated user
     * 6. Pass request to next filter in chain
     * 
     * If any step fails (token missing, invalid, user not found):
     * - Silently continues (user remains unauthenticated)
     * - Next authorization filter checks if endpoint requires auth
     * - If required and not authenticated → returns 401 Unauthorized
     * 
     * @param request HTTP request with Authorization header
     * @param response HTTP response (for error handling)
     * @param filterChain Remaining filters in chain
     * @throws ServletException If filter processing fails
     * @throws IOException If I/O error occurs
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                   HttpServletResponse response,
                                   FilterChain filterChain) throws ServletException, IOException {
        log.debug("JWT_FILTER: {} {}", request.getMethod(), request.getRequestURI());
        
        try {
            /**
             * Step 1: Extract JWT token from Authorization header
             * Format: Authorization: Bearer {token}
             * This method returns the {token} part or null if missing
             */
            String jwt = extractJwtFromRequest(request);

            /**
             * Step 2: Validate token exists and is valid
             * - StringUtils.hasText(jwt): Token not empty/null
             * - jwtUtil.validateToken(jwt): Signature correct, not expired
             * Only proceed if both conditions true
             */
            if (StringUtils.hasText(jwt) && jwtUtil.validateToken(jwt)) {
                /**
                 * Step 3: Extract username (email) from token claims
                 * This doesn't require database query, just JWT parsing
                 */
                String username = jwtUtil.extractUsername(jwt);

                /**
                 * Step 4: Load user from database using email
                 * UserDetailsService queries users table
                 * Returns UserDetails with:
                 * - username (email)
                 * - password (hashed, for legacy auth support)
                 * - authorities (roles, permissions)
                 */
                UserDetails userDetails = authService.loadUserByUsername(username);
                
                /**
                 * Step 5: Create authentication token
                 * UsernamePasswordAuthenticationToken:
                 * - principal: userDetails (the authenticated user)
                 * - credentials: null (not used after JWT auth)
                 * - authorities: userDetails.getAuthorities() (roles, permissions)
                 * 
                 * This represents an authenticated user in Spring Security
                 */
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());
                
                /**
                 * Step 6: Set request details
                 * WebAuthenticationDetailsSource.buildDetails(request) extracts:
                 * - Remote IP address
                 * - Session ID
                 * - Other request metadata
                 * Useful for audit logs and security checks
                 */
                authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request));

                /**
                 * Step 7: Populate SecurityContext with authenticated user
                 * SecurityContextHolder is ThreadLocal that stores auth info
                 * Available to entire request processing chain
                 * Controllers, services, authorization rules all access this
                 */
                SecurityContext context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(authentication);
                SecurityContextHolder.setContext(context);
                securityContextRepository.saveContext(context, request, response);
                log.info("JWT_AUTH_SUCCESS: user={}", username);
            }
        } catch (Exception ex) {
            /**
             * Exception handling:
             * If any error during JWT processing, just log and continue
             * This ensures filter chain doesn't break on JWT errors
             * Public endpoints (no auth required) still work
             * Protected endpoints checked later by @PreAuthorize
             * 
             * Common exceptions:
             * - JwtException: Invalid token format
             * - UsernameNotFoundException: User email not in database
             * - Exception: Any other error during processing
             */
            log.error("Could not set user authentication in security context", ex);
        }

        // Continue to next filter in chain
        filterChain.doFilter(request, response);
    }

    /**
     * Extracts JWT token from Authorization header.
     * 
     * Expected header format:
     * Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...
     * 
     * Process:
     * 1. Read Authorization header from request
     * 2. Check if header exists and starts with "Bearer " prefix
     * 3. Extract and return token portion (everything after "Bearer ")
     * 4. Return null if header missing or format incorrect
     * 
     * Why "Bearer" prefix?
     * - OAuth 2.0 standard for token-based authentication
     * - Distinguishes from "Basic" (username:password base64)
     * - Indicates bearer token (anyone with token can use it)
     * 
     * @param request HTTP request object
     * @return JWT token string (without "Bearer " prefix) or null if not found
     * 
     * @example
     * // Request: GET /api/farms HTTP/1.1
     *           Authorization: Bearer eyJhbGciOiJIUzUxMiI...
     * String token = extractJwtFromRequest(request);
     * // Returns: "eyJhbGciOiJIUzUxMiI..."
     */
    private String extractJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);  // Skip "Bearer " (7 characters)
        }
        return null;
    }
}
