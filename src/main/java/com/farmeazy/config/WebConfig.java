package com.farmeazy.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * WEB CONFIGURATION - MVC AND CORS SETUP
 * 
 * PURPOSE: Configure Spring Web MVC settings for REST API development.
 * Enables CORS (Cross-Origin Resource Sharing) for frontend integration.
 * 
 * CORE FEATURES:
 * 1. CORS CONFIGURATION:
 *    - Allows API requests from different domains/ports
 *    - Frontend runs on localhost:3000, localhost:3001
 *    - Backend API runs on localhost:8080
 *    - Without CORS, browser blocks cross-origin requests (security feature)
 * 
 * 2. ALLOWED ORIGINS:
 *    - http://localhost:4200 (Angular development server)
 *    - http://localhost:3000 (React/Node development server)
 *    - http://localhost:3001 (Alternate React/Node port)
 * 
 * 3. ALLOWED METHODS:
 *    - GET: Retrieve data (read-only, idempotent)
 *    - POST: Create new resources
 *    - PUT: Update existing resources
 *    - DELETE: Remove resources
 *    - OPTIONS: Pre-flight CORS checks (automatic)
 * 
 * 4. ALLOWED HEADERS:
 *    - * (asterisk): Accept all headers
 *    - Specifically allows: Authorization (JWT tokens), Content-Type, etc.
 * 
 * 5. CREDENTIALS SUPPORT:
 *    - allowCredentials(true): Allows cookies and auth headers
 *    - Essential for JWT authentication in cross-origin requests
 *    - Browser requires explicit allowCredentials for Authorization header
 * 
 * 6. PRE-FLIGHT CACHING:
 *    - maxAge(3600): Browser caches CORS policy for 1 hour
 *    - Reduces OPTIONS requests for repeated API calls
 *    - Improves performance in production
 * 
 * CORS FLOW DIAGRAM:
 * ┌─────────────────────────────────────────────────────────────┐
 * │ 1. Browser makes cross-origin API request (e.g., POST)       │
 * │    - Origin: http://localhost:3000                           │
 * │    - Target: http://localhost:8080/api/farms                 │
 * └─────────────────────────────────────────────────────────────┘
 *                              │
 *                              ▼
 * ┌─────────────────────────────────────────────────────────────┐
 * │ 2. Browser sends automatic OPTIONS pre-flight request        │
 * │    - Asks: "Is POST allowed? Can I send Authorization?"      │
 * │    - No actual data sent in pre-flight                       │
 * └─────────────────────────────────────────────────────────────┘
 *                              │
 *                              ▼
 * ┌─────────────────────────────────────────────────────────────┐
 * │ 3. WebConfig returns CORS headers in OPTIONS response        │
 * │    - Access-Control-Allow-Origin: http://localhost:3000      │
 * │    - Access-Control-Allow-Methods: GET, POST, PUT, DELETE    │
 * │    - Access-Control-Allow-Headers: *                         │
 * │    - Access-Control-Allow-Credentials: true                  │
 * │    - Access-Control-Max-Age: 3600                            │
 * └─────────────────────────────────────────────────────────────┘
 *                              │
 *                              ▼
 * ┌─────────────────────────────────────────────────────────────┐
 * │ 4. Browser permits actual request (POST with JWT token)      │
 * │    - Sends Authorization: Bearer {jwt_token}                 │
 * │    - Server processes request normally                       │
 * └─────────────────────────────────────────────────────────────┘
 *                              │
 *                              ▼
 * ┌─────────────────────────────────────────────────────────────┐
 * │ 5. Server responds with data                                 │
 * │    - Response includes CORS headers again                    │
 * │    - Browser validates and delivers data to frontend         │
 * └─────────────────────────────────────────────────────────────┘
 * 
 * NOTE: SecurityConfig also has CORS configuration via corsConfigurationSource().
 * Both configurations work together:
 * - WebConfig: Global MVC-level CORS (broader, applies to all requests)
 * - SecurityConfig: Security filter-level CORS (specific, applies to Spring Security)
 * - Both must allow the same origins for proper functionality
 * 
 * DEVELOPMENT WORKFLOW:
 * 1. Frontend developer starts React app on localhost:3000
 * 2. Frontend makes API request to http://localhost:8080/api/farms
 * 3. Browser detects cross-origin, sends OPTIONS pre-flight
 * 4. This WebConfig allows it, returns permission headers
 * 5. Browser then sends actual POST/GET/PUT/DELETE request
 * 6. JWT token included in Authorization header (credentials allowed)
 * 7. Backend SecurityConfig validates JWT and processes request
 * 
 * TROUBLESHOOTING CORS ERRORS:
 * ┌─────────────────────────────────────────────────────────────┐
 * │ Error: "Access to XMLHttpRequest blocked by CORS policy"     │
 * │ Solution: Add your frontend origin to allowedOrigins list    │
 * ├─────────────────────────────────────────────────────────────┤
 * │ Error: "Authorization header missing in CORS response"       │
 * │ Solution: Ensure allowCredentials(true) is set               │
 * ├─────────────────────────────────────────────────────────────┤
 * │ Error: "POST method not allowed"                             │
 * │ Solution: Verify POST is in allowedMethods array             │
 * ├─────────────────────────────────────────────────────────────┤
 * │ Error: "Custom header not allowed"                           │
 * │ Solution: Use allowedHeaders("*") to accept all headers      │
 * └─────────────────────────────────────────────────────────────┘
 * 
 * PRODUCTION CONSIDERATIONS:
 * - Replace localhost:3000 with actual frontend domain (e.g., farmeazy.com)
 * - Use HTTPS instead of HTTP
 * - Reduce maxAge if deploying frequently
 * - Consider rate limiting (prevent CORS-based DDoS)
 * - Log CORS rejections for debugging
 * 
 * RELATIONSHIP WITH SECURITY:
 * - CORS is NOT a security feature (clients can bypass it)
 * - CORS is a browser feature (server-to-server requests ignore CORS)
 * - Real security enforced by: JWT validation, Authorization checks, HTTPS
 * - CORS just enables browser-based frontend access
 * 
 * @author FarmEazy Development Team
 * @version 1.0.0
 * @since January 2026
 * @see SecurityConfig for complementary CORS configuration
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * Configure CORS for API endpoints.
     * 
     * This method:
     * 1. Enables CORS for all /api/** paths
     * 2. Specifies allowed origins (frontend domains)
     * 3. Specifies allowed HTTP methods
     * 4. Specifies allowed request headers
     * 5. Allows credentials (cookies, auth headers)
     * 6. Sets pre-flight cache duration
     * 
     * Applied to all API endpoints defined in controllers.
     * Pre-flight requests (OPTIONS) handled automatically by Spring.
     * 
     * @param registry CORS registry for adding mappings
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns(
                    "https://farm-eazy.com",
                    "https://www.farm-eazy.com",
                    "https://*.vercel.app",
                    "https://farm-eazy-backend.onrender.com",
                    "http://localhost:4200",
                    "http://localhost:3000",
                    "http://localhost:3001",
                    "http://localhost:5173"
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH", "HEAD")
                .allowedHeaders("*")
                .exposedHeaders("Authorization", "Content-Type")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
