package com.farmeazy.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * JWT UTILITY - TOKEN GENERATION & VALIDATION
 * 
 * PURPOSE: Centralized JWT token management for FarmEazy authentication.
 * Generates secure tokens with HMAC-SHA512 signature and validates token authenticity.
 * 
 * JWT TOKEN STRUCTURE:
 * ┌─────────────────────────────────────────────────────────────────────────┐
 * │ eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.                                   │
 * │ eyJyb2xlcyI6WyJVU0VSIl0sInN1YiI6InRlc3RAZXhhbXBsZS5jb20iLCJpYXQiOjE2     │
 * │ MzAyOTY3OTIsImV4cCI6MTYzMDM4MzE5Mn0.                                     │
 * │ HmP5r_8S9Yd_9L6qZ_5V_8D9rQ_1Y_4K_9M_2B_5P_7G                            │
 * └─────────────────────────────────────────────────────────────────────────┘
 *  └─Header──┘ └──────────────────Payload──────────────────┘ └Signature──┘
 * 
 * HEADER:
 * {
 *   "alg": "HS512",    // Algorithm: HMAC SHA-512
 *   "typ": "JWT"       // Type: JSON Web Token
 * }
 * 
 * PAYLOAD (CLAIMS):
 * {
 *   "roles": ["USER"],                    // User authorities/roles
 *   "sub": "test@example.com",            // Subject (email, user identifier)
 *   "iat": 1630296792,                    // Issued at (Unix timestamp)
 *   "exp": 1630383192                     // Expiration (Unix timestamp, +24h)
 * }
 * 
 * SIGNATURE:
 * - Generated using HMAC-SHA512 algorithm
 * - Secret key: Configured in application.properties (jwt.secret)
 * - Prevents tampering: If payload modified, signature won't match
 * - Only server knows secret, so clients can't forge tokens
 * 
 * CONFIGURATION PROPERTIES:
 * jwt.secret=your-secret-key-here-minimum-32-bytes-recommended
 * jwt.expiration=86400000                // 24 hours in milliseconds (24*60*60*1000)
 * 
 * KEY FEATURES:
 * 1. TOKEN GENERATION:
 *    - Called during user registration/login
 *    - Includes user's email as subject (username)
 *    - Includes user's roles/authorities in claims
 *    - Sets issue time and expiration time
 *    - Signs with HMAC-SHA512 algorithm
 * 
 * 2. TOKEN VALIDATION:
 *    - Verifies signature using secret key
 *    - Checks expiration time
 *    - Returns true only if both checks pass
 *    - Prevents expired token usage
 *    - Prevents forged/tampered tokens
 * 
 * 3. CLAIMS EXTRACTION:
 *    - Extract username (email) from token
 *    - Extract expiration date
 *    - Extract custom claims (roles, user ID, etc.)
 *    - Enables user identification without database query
 * 
 * SECURITY PROPERTIES:
 * - Stateless: No server-side session storage needed
 * - Self-contained: All needed info in token
 * - Signed: Cryptographic verification prevents forgery
 * - Expiring: Limited validity window (24 hours)
 * - Non-reversible: Cannot extract original payload without signature verification
 * 
 * TOKEN LIFECYCLE:
 * ┌──────────────────────────────────────────────────────────┐
 * │ 1. GENERATION                                             │
 * │    User registers/logs in → JwtUtil.generateToken()      │
 * │    → Token returned to client                             │
 * └──────────────────────────────────────────────────────────┘
 *                      ↓
 * ┌──────────────────────────────────────────────────────────┐
 * │ 2. STORAGE                                                 │
 * │    Client stores in localStorage or sessionStorage        │
 * │    Included in Authorization header: Bearer {token}       │
 * └──────────────────────────────────────────────────────────┘
 *                      ↓
 * ┌──────────────────────────────────────────────────────────┐
 * │ 3. TRANSMISSION                                            │
 * │    Client sends: GET /api/farms                           │
 * │    Header: Authorization: Bearer {token}                  │
 * └──────────────────────────────────────────────────────────┘
 *                      ↓
 * ┌──────────────────────────────────────────────────────────┐
 * │ 4. VALIDATION                                              │
 * │    JwtAuthenticationFilter extracts token                 │
 * │    JwtUtil.validateToken() verifies signature & time      │
 * │    If valid → JwtUtil.extractUsername() gets email        │
 * │    UserDetailsService.loadUserByUsername(email)           │
 * │    SecurityContext.setAuthentication()                    │
 * └──────────────────────────────────────────────────────────┘
 *                      ↓
 * ┌──────────────────────────────────────────────────────────┐
 * │ 5. AUTHORIZATION                                           │
 * │    Controller uses SecurityContextHolder to verify user    │
 * │    User isolation checks in service layer                 │
 * │    Response returned if authorized                        │
 * └──────────────────────────────────────────────────────────┘
 *                      ↓
 * ┌──────────────────────────────────────────────────────────┐
 * │ 6. EXPIRATION                                              │
 * │    After 24 hours, token expired                          │
 * │    validateToken() returns false                          │
 * │    Request returns 401 Unauthorized                       │
 * │    Client redirects to login                              │
 * └──────────────────────────────────────────────────────────┘
 * 
 * IMPLEMENTATION NOTES:
 * - SecretKey generated from jwt.secret using HMAC algorithm
 * - Minimum 32 bytes recommended (256 bits) for security
 * - Token is Base64URL-encoded for safe transmission
 * - Claims customizable: Add more data by modifying claims Map
 * 
 * POTENTIAL ENHANCEMENTS:
 * - Refresh tokens: Issue short-lived access token + long-lived refresh token
 * - Token revocation: Maintain blacklist of logged-out tokens
 * - Role-based claims: Add ADMIN, USER, EDITOR roles to token
 * - Device tracking: Include device ID in claims for security
 * 
 * TROUBLESHOOTING:
 * - "Invalid token" error: Check secret key matches between generation and validation
 * - "Token expired" error: Check system time synchronization, expiration settings
 * - "Could not extract username" error: Ensure claims set correctly during generation
 * 
 * @author FarmEazy Development Team
 * @version 1.0.0
 * @since January 2026
 * @see JwtAuthenticationFilter for filter that uses this utility
 * @see AuthService for token generation during auth
 */
@Component
public class JwtUtil {

    /**
     * JWT secret key from application properties.
     * Used for HMAC-SHA512 signing and verification.
     * Must be at least 32 bytes for HS512 algorithm.
     * Keep this secret! Expose in code only via properties file.
     */
    @Value("${jwt.secret}")
    private String secret;

    /**
     * Token expiration time in milliseconds.
     * Default: 86400000 (24 hours)
     * Configured in application.properties: jwt.expiration
     */
    @Value("${jwt.expiration}")
    private Long expiration;

    @Value("${jwt.expiration.user:${jwt.expiration}}")
    private Long userExpiration;

    @Value("${jwt.expiration.admin:${jwt.expiration}}")
    private Long adminExpiration;

    /**
     * Gets the signing key from secret string.
     * HMAC algorithm requires SecretKey of sufficient length.
     * Spring JJWT library handles key generation and validation.
     * 
     * @return SecretKey for HMAC-SHA512 signing
     */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    /**
     * Generates JWT token for authenticated user.
     * 
     * Token includes:
     * - User roles/authorities in claims
     * - User email as subject
     * - Current timestamp (issued at)
     * - Future timestamp (expiration)
     * - HMAC-SHA512 signature
     * 
     * Flow:
     * 1. Create claims map with user authorities
     * 2. Set subject to user email (unique identifier)
     * 3. Add issued-at and expiration timestamps
     * 4. Sign with HMAC-SHA512 using secret key
     * 5. Compact to Base64URL string
     * 
     * @param userDetails Spring Security UserDetails with authorities
     * @return Signed JWT token string (ready to send to client)
     * 
     * @example
     * String token = jwtUtil.generateToken(userDetails);
     * // Returns: "eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJyb2xlcyI6..."
     */
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", userDetails.getAuthorities());
        return createToken(claims, userDetails.getUsername(), resolveExpirationByRole(userDetails));
    }

    public String generateToken(UserDetails userDetails, long customExpirationMs) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", userDetails.getAuthorities());
        return createToken(claims, userDetails.getUsername(), customExpirationMs);
    }

    private long resolveExpirationByRole(UserDetails userDetails) {
        if (userDetails != null && userDetails.getAuthorities() != null) {
            boolean isAdmin = userDetails.getAuthorities().stream().anyMatch(authority -> {
                String role = authority.getAuthority();
                return "ROLE_ADMIN".equals(role) || "ROLE_SUPERADMIN".equals(role) || "ADMIN".equals(role) || "SUPERADMIN".equals(role);
            });
            return isAdmin ? adminExpiration : userExpiration;
        }
        return expiration;
    }

    /**
     * Internal method to create token with custom claims.
     * 
     * Steps:
     * 1. Get current time
     * 2. Calculate expiration as current time + expiration duration
     * 3. Build JWT using Jwts.builder()
     * 4. Add claims (custom data)
     * 5. Set subject (email, unique identifier)
     * 6. Set timestamps
     * 7. Sign with secret key using HS512 algorithm
     * 8. Compact to encoded string
     * 
     * @param claims Custom claims to include (e.g., roles, user ID)
     * @param subject User email (unique identifier)
     * @return Compact JWT token string
     */
    private String createToken(Map<String, Object> claims, String subject, long expirationMs) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    /**
     * Extracts username (email) from token claims.
     * Used to identify user after validation.
     * 
     * Flow:
     * 1. Parse token to get all claims
     * 2. Extract subject claim (which is email)
     * 3. Return email string
     * 
     * Note: Does NOT validate token. Use validateToken() first.
     * 
     * @param token JWT token string
     * @return User email from token subject
     * 
     * @example
     * String email = jwtUtil.extractUsername(token);
     * // Returns: "test@example.com"
     * 
     * @throws io.jsonwebtoken.JwtException if token invalid format
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extracts expiration date from token claims.
     * Used to determine when token expires.
     * 
     * @param token JWT token string
     * @return Expiration timestamp as Date object
     * 
     * @example
     * Date expiryDate = jwtUtil.extractExpiration(token);
     * boolean isExpired = expiryDate.before(new Date());
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Generic method to extract any claim from token.
     * Enables extraction of custom claims.
     * 
     * Uses Function interface to apply transformation.
     * 
     * @param token JWT token string
     * @param claimsResolver Function to apply to claims (e.g., Claims::getSubject)
     * @param <T> Type of claim value
     * @return Extracted and transformed claim value
     * 
     * @example
     * String role = extractClaim(token, claims -> claims.get("roles"));
     * Integer userId = extractClaim(token, claims -> claims.get("id", Integer.class));
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Extracts all claims from token.
     * This is the internal parsing step that verifies signature.
     * 
     * Security Process:
     * 1. Jwts.parser() creates JWT parser instance
     * 2. verifyWith(signingKey) sets key for signature verification
     * 3. build() completes parser configuration
     * 4. parseSignedClaims(token) parses and verifies signature
     * 5. getPayload() extracts claims from verified token
     * 
     * If signature invalid, parseSignedClaims() throws exception.
     * 
     * @param token JWT token string
     * @return All claims from token payload
     * 
     * @throws io.jsonwebtoken.JwtException if signature invalid or parsing fails
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Checks if token has expired.
     * Compares expiration timestamp to current time.
     * 
     * @param token JWT token string
     * @return true if token expired, false if still valid
     * 
     * @example
     * if (jwtUtil.isTokenExpired(token)) {
     *     // Token too old, reject it
     * }
     */
    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Validates token signature and expiration.
     * Primary validation method called by JwtAuthenticationFilter.
     * 
     * Validation Steps:
     * 1. Attempt to parse token with signature verification
     * 2. If parsing fails (invalid signature), return false
     * 3. Check if token not expired
     * 4. Return true only if both checks pass
     * 
     * Catches all exceptions to prevent filter chain breaking:
     * - io.jsonwebtoken.SignatureException: Invalid signature
     * - io.jsonwebtoken.MalformedJwtException: Invalid format
     * - io.jsonwebtoken.ExpiredJwtException: Token too old
     * - io.jsonwebtoken.UnsupportedJwtException: Wrong algorithm
     * - io.jsonwebtoken.IllegalArgumentException: Empty token
     * 
     * @param token JWT token string from Authorization header
     * @return true if token valid (correct signature, not expired), false otherwise
     * 
     * @example
     * if (jwtUtil.validateToken(token)) {
     *     String email = jwtUtil.extractUsername(token);
     *     // Token valid, use email to load user
     * } else {
     *     // Token invalid, return 401 Unauthorized
     * }
     */
    public Boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Validates token against specific user details.
     * Ensures token belongs to the requested user.
     * 
     * Validation Steps:
     * 1. Extract username from token
     * 2. Compare to userDetails username
     * 3. Verify token not expired
     * 4. Both must match
     * 
     * @param token JWT token string
     * @param userDetails UserDetails object to validate against
     * @return true if token valid and matches user, false otherwise
     * 
     * @example
     * UserDetails user = userDetailsService.loadUserByUsername(email);
     * if (jwtUtil.validateToken(token, user)) {
     *     // Token valid and matches user, allow request
     * }
     */
    public Boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    /**
     * Extract authorities from JWT roles claim without hitting the database.
     * Supports roles claim as either:
     * - List<Map<String, Object>> with key "authority"
     * - List<String>
     */
    public Collection<GrantedAuthority> extractAuthorities(String token) {
        try {
            Object rolesClaim = extractAllClaims(token).get("roles");
            if (!(rolesClaim instanceof Collection<?> roles)) {
                return Collections.emptyList();
            }

            Collection<GrantedAuthority> authorities = new ArrayList<>();
            for (Object role : roles) {
                if (role instanceof Map<?, ?> roleMap) {
                    Object authority = roleMap.get("authority");
                    if (authority instanceof String auth && !auth.isBlank()) {
                        authorities.add(new SimpleGrantedAuthority(auth));
                    }
                } else if (role instanceof String auth && !auth.isBlank()) {
                    authorities.add(new SimpleGrantedAuthority(auth));
                }
            }
            return authorities;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    /**
     * Generate reset token with 1 hour expiration.
     * Used for password reset functionality.
     * 
     * @param userDetails User details containing email
     * @return Reset token string
     */
    public String generateResetToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "reset");
        
        // 1 hour expiration for reset tokens
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + 1000 * 60 * 60); // 1 hour
        
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    /**
     * Extract email from token.
     * Handles both regular and reset tokens.
     * 
     * @param token JWT token string
     * @return Email (username) from token
     */
    public String extractEmailFromToken(String token) {
        return extractUsername(token);
    }
}

