package com.farmeazy.service;

import com.farmeazy.dto.AuthLoginDto;
import com.farmeazy.dto.AuthRegisterDto;
import com.farmeazy.dto.AuthResponseDto;
import com.farmeazy.dto.GoogleCompleteProfileDto;
import com.farmeazy.dto.OtpRequestDto;
import com.farmeazy.dto.SmsResponseDto;
import com.farmeazy.entity.PasswordResetToken;
import com.farmeazy.entity.RefreshToken;
import com.farmeazy.entity.User;
import com.farmeazy.entity.UserActivity.ActivityType;
import com.farmeazy.exception.DuplicateResourceException;
import com.farmeazy.exception.ResourceNotFoundException;
import com.farmeazy.exception.UnauthorizedException;
import com.farmeazy.repository.PasswordResetTokenRepository;
import com.farmeazy.repository.RefreshTokenRepository;
import com.farmeazy.repository.UserRepository;
import com.farmeazy.service.OtpService;
import com.farmeazy.service.NotificationService;
import com.farmeazy.sms.SmsTemplate;
import com.farmeazy.security.JwtUtil;
import com.farmeazy.service.UserActivityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Set;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * AUTH SERVICE - USER AUTHENTICATION & AUTHORIZATION
 * 
 * PURPOSE: Handles all user authentication operations including registration, login, and user lookups.
 * Implements Spring Security's UserDetailsService for database-backed user loading.
 * 
 * KEY RESPONSIBILITIES:
 * 1. User Registration: Creates new user accounts with encrypted passwords
 * 2. User Login: Authenticates credentials and generates JWT tokens
 * 3. User Loading: Loads user details for Spring Security authentication
 * 4. Password Encryption: Encodes passwords using BCryptPasswordEncoder
 * 5. JWT Token Generation: Creates secure tokens for API authentication
 * 6. Duplicate Prevention: Prevents duplicate email registrations
 * 
 * SECURITY FEATURES:
 * - Passwords encrypted with BCryptPasswordEncoder (irreversible hashing)
 * - JWT tokens with HMAC-SHA512 signature verification
 * - User roles managed for authorization
 * - Transactional operations for data consistency
 * - Spring Security integration for stateless API authentication
 * 
 * DEPENDENCIES:
 * - UserRepository: Database access for user data
 * - PasswordEncoder: BCrypt password encryption/verification
 * - AuthenticationManager: Spring Security authentication processor
 * - JwtUtil: JWT token generation and validation
 * 
 * WORKFLOW:
 * REGISTRATION:
 *   1. Client sends email, password, name, phone to /api/auth/register
 *   2. Service checks if email already exists (prevents duplicates)
 *   3. Password is encrypted using BCryptPasswordEncoder
 *   4. User created with default "USER" role
 *   5. JWT token generated for immediate login
 *   6. AuthResponseDto returned with token and user info
 * 
 * LOGIN:
 *   1. Client sends email and password to /api/auth/login
 *   2. AuthenticationManager validates credentials against database
 *   3. On success: JWT token generated
 *   4. On failure: AuthenticationException thrown by Spring Security
 *   5. AuthResponseDto returned with token and user info
 * 
 * USER LOADING:
 *   1. Spring Security calls loadUserByUsername(email) during authentication
 *   2. Service looks up user by email in database
 *   3. Converts User entity to Spring Security UserDetails object
 *   4. Includes user roles converted to authorities (e.g., "USER" → "ROLE_USER")
 */
@Service
public class AuthService implements UserDetailsService {
    @Autowired
    private com.farmeazy.service.SmsService smsService;

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(AuthService.class);

    @Autowired
    private OtpService otpService;
    /**
     * CHANGE PASSWORD LOGIC
     * Validates current password, updates to new password, sends confirmation email.
     */
    @Transactional
    public void changePassword(com.farmeazy.dto.ChangePasswordDto dto) {
        // Get authenticated user email from SecurityContext
        String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new com.farmeazy.exception.ResourceNotFoundException("User not found"));
        // Validate current password
        if (!passwordEncoder.matches(dto.getCurrentPassword(), user.getPassword())) {
            throw new com.farmeazy.exception.UnauthorizedException("Current password is incorrect");
        }
        // Update password
        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userRepository.save(user);
        // Send confirmation email via Resend (HttpEmailService)
        httpEmailService.sendPasswordChangedConfirmation(user.getEmail(), user.getUsername());
        // Log activity
        userActivityService.logActivity(user, com.farmeazy.entity.UserActivity.ActivityType.PASSWORD_CHANGED, "Password changed successfully");
    }

    @Transactional
    public void changePasswordWithOtp(String phone, String otpCode, String newPassword) {
        // In verify -> change flow, avoid throwing for already-verified OTPs inside this
        // transaction, otherwise Spring marks transaction rollback-only.
        if (otpService.isVerifiedLoginOtpStillValid(phone, otpCode)) {
            logger.info("Using previously verified LOGIN OTP for password change: phone={}", phone);
        } else {
            otpService.verifyLoginOtp(phone, otpCode);
        }

        var user = resolveUserByPhone(phone)
                .orElseThrow(() -> new com.farmeazy.exception.ResourceNotFoundException("User not found with this phone number"));

        if (user.getActive() == null || !user.getActive()) {
            throw new com.farmeazy.exception.UnauthorizedException("User account is inactive");
        }

        // Only admin users allowed to change password via admin settings OTP path
        if (user.getRoles() == null || !user.getRoles().contains("ADMIN")) {
            throw new com.farmeazy.exception.UnauthorizedException("Only admin users can use this endpoint");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        httpEmailService.sendPasswordChangedConfirmation(user.getEmail(), user.getUsername());

        userActivityService.logActivity(user, com.farmeazy.entity.UserActivity.ActivityType.PASSWORD_CHANGED,
                "Password changed via OTP from settings");
    }

    @Autowired
    private UserRepository userRepository;

    private PasswordEncoder passwordEncoder;

    @Autowired
    public void setPasswordEncoder(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private EmailService emailService;

    @Autowired
    private HttpEmailService httpEmailService;

    @Autowired
    private UserActivityService userActivityService;

    @Autowired
    private NotificationService notificationService;
    
    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh-expiration:2592000000}")
    private Long refreshTokenExpirationMs;

    @Value("${google.oauth.client-id:1034508002249-ms9o9tpqd0cs4jrkubhicg36oskot3a1.apps.googleusercontent.com}")
    private String googleOAuthClientId;
    
    private static final String SHORT_CODE_CHARS = "abcdefghijklmnopqrstuvwxyz0123456789";
    private static final int SHORT_CODE_LENGTH = 8;
    private static final int REFRESH_TOKEN_BYTES = 64;
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * SET AUTHENTICATION MANAGER
     * 
     * PURPOSE: Initializes AuthenticationManager via setter injection.
     * Avoids circular dependency issues with constructor injection.
     * 
     * @param authenticationManager Spring Security's authentication processor
     * 
     * WHAT IT DOES: Stores reference to AuthenticationManager for later use in login()
     */
    @Autowired
    public void setAuthenticationManager(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    /**
     * LOAD USER BY USERNAME (SPRING SECURITY)
     * 
     * PURPOSE: Implements Spring Security's UserDetailsService interface.
     * Called during authentication to load user details from database.
     * 
     * PARAMETERS:
     * - identifier: User's email address OR username
     * 
     * RETURNS: Spring Security UserDetails object with:
     * - Username: email
     * - Password: encrypted password from database
     * - Active: account enabled/disabled status
     * - Authorities: user roles (e.g., "ROLE_USER", "ROLE_ADMIN")
     * 
     * HOW IT WORKS:
     * 1. Query database for user by email first, then by username
     * 2. If not found, throw IllegalArgumentException
     * 3. Create Spring Security User object
     * 4. Return UserDetails for authentication comparison
     * 
     * USED BY: Spring Security's DaoAuthenticationProvider during login
     */
    // Note: loadUserByUsername is defined after resetPassword method to support email OR username login

    /**
     * USER REGISTRATION - CREATE NEW ACCOUNT
     * 
     * PURPOSE: Registers a new user in the system.
     * Handles validation, encryption, and JWT token generation.
     * 
     * PARAMETERS:
     * - registerDto: Registration data (username, email, password, phone, address, city, state, pinCode)
     * 
     * RETURNS: AuthResponseDto with:
     * - User ID, email, fullName, roles
     * - JWT token for immediate authentication
     * 
     * SECURITY:
     * - Password encrypted with BCryptPasswordEncoder (one-way hashing)
     * - Email uniqueness enforced (prevents duplicate accounts)
     * - Transactional ensures all-or-nothing database consistency
     * 
     * PROCESS:
     * 1. Check if email already registered (prevent duplicates)
     * 2. Create new User entity
     * 3. Encrypt password using BCryptPasswordEncoder
     * 4. Set all provided user information
     * 5. Assign default "USER" role
     * 6. Save user to database (auto-generates ID, timestamps)
     * 7. Load user details (invokes loadUserByUsername)
     * 8. Generate JWT token with user roles
     * 9. Return response with token (user can login immediately)
     * 
     * THROWS: DuplicateResourceException if email already registered
     * 
     * THROWS: Validation exceptions from DTO constraints
     */
    @Transactional
    public AuthResponseDto register(AuthRegisterDto registerDto) {
        // Check if email already exists in database
        if (userRepository.existsByEmail(registerDto.getEmail())) {
            throw new DuplicateResourceException("Email already registered");
        }

        // Validate username (required - no auto-generation)
        String username = registerDto.getUsername();
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username is required");
        }

        // Check if username already exists
        if (userRepository.existsByUsername(username)) {
            throw new DuplicateResourceException("Username '" + username + "' is already taken. Please choose another username.");
        }

        // Check if phone number already exists
        if (registerDto.getPhone() != null && !registerDto.getPhone().trim().isEmpty()) {
            if (userRepository.existsByPhone(registerDto.getPhone())) {
                throw new DuplicateResourceException("Phone number already registered. Please use a different number or login with OTP.");
            }
        }

        // Create new user entity
        User user = new User();
        user.setEmail(registerDto.getEmail());
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(registerDto.getPassword()));
        user.setPhone(registerDto.getPhone());
        user.setAddress(registerDto.getAddress());
        user.setCity(registerDto.getCity());
        user.setState(registerDto.getState());
        user.setPinCode(registerDto.getPinCode());
        user.setAuthProvider("PASSWORD");
        user.setProfileCompleted(true);

        // Assign default role for new users
        Set<String> roles = new HashSet<>();
        roles.add("USER");
        user.setRoles(roles);

        // Save user to database (use saveAndFlush to ensure ID is generated immediately)
        user = userRepository.saveAndFlush(user);


        // Log registration activity (only if user has valid ID)
        if (user.getId() != null) {
            try {
                userActivityService.logActivity(
                        user,
                        ActivityType.REGISTERED,
                        "Registered a new account (instant registration)"
                );
            } catch (Exception e) {
                // Only log if external API call fails
                logger.warn("Failed to log registration activity for {}: {}", user.getEmail(), e.getMessage(), e);
            }
        }

        triggerWelcomeCommunicationsAfterCommit(user);

        // Return response with user info and JWT token
        UserDetails userDetails = loadUserByUsername(user.getEmail());
        String token = jwtUtil.generateToken(userDetails);
        return mapUserToAuthResponseDto(user, token, null);
    }

    /**
     * USER LOGIN - AUTHENTICATE & GET TOKEN
     * 
     * PURPOSE: Authenticates user credentials and generates JWT token.
     * Stateless authentication for REST API (no sessions).
     * 
     * PARAMETERS:
     * - loginDto: Login credentials (email, password)
     * 
     * RETURNS: AuthResponseDto with:
     * - User ID, email, fullName, roles
     * - JWT token for authenticating future requests
     * 
     * PROCESS:
     * 1. Create UsernamePasswordAuthenticationToken from email/password
     * 2. AuthenticationManager validates credentials:
     *    - Calls loadUserByUsername(email) to load user from DB
     *    - Uses PasswordEncoder.matches() to verify password
     * 3. If credentials valid: returns Authentication object
     * 4. If credentials invalid: throws AuthenticationException
     * 5. Extract UserDetails from Authentication
     * 6. Generate JWT token with user roles
     * 7. Return token and user info
     * 
     * RETURNS: AuthResponseDto with JWT token
     * 
     * THROWS: BadCredentialsException if password incorrect
     * THROWS: DisabledException if user account inactive
     * THROWS: LockedException if account is locked
     * 
     * HANDLED BY: GlobalExceptionHandler converts to HTTP 401 Unauthorized
     */
    public AuthResponseDto login(AuthLoginDto loginDto, HttpServletRequest request) {
        String identifier = loginDto.getIdentifier();
        
        // Resolve identifier to user (supports email, username, or user ID)
        User user = resolveUserByIdentifier(identifier);
        
        // Authenticate using email (Spring Security uses email as username)
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        user.getEmail(),
                        loginDto.getPassword()
                )
        );

        // OTP/2FA for login removed: login is now password-only

        // Proceed to generate JWT
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String token = jwtUtil.generateToken(userDetails);
        String refreshToken = null;
        if (Boolean.TRUE.equals(loginDto.getRememberMe())) {
            refreshToken = issueRefreshToken(user, request);
        }

        // Log login activity
        try {
            userActivityService.logActivity(
                    user,
                    ActivityType.LOGGED_IN,
                    "Logged in to the system"
            );
        } catch (Exception e) {
            System.err.println("Failed to log login activity: " + e.getMessage());
        }

        // Return response with user info and token
        return mapUserToAuthResponseDto(user, token, refreshToken);
    }

    @Transactional(readOnly = true)
    public AuthResponseDto loginWithGoogle(String credential, HttpServletRequest request) {
        String normalizedCredential = credential == null ? "" : credential.trim();
        if (normalizedCredential.isBlank()) {
            throw new UnauthorizedException("Google credential is required");
        }

        GoogleProfile profile = verifyGoogleCredential(normalizedCredential);
        User user = userRepository.findByEmail(profile.email())
                .orElseThrow(() -> new UnauthorizedException("No FarmEazy account is registered with this Google email. Please sign up first."));

        if (user.getActive() == null || !user.getActive()) {
            throw new UnauthorizedException("User account is inactive");
        }

        UserDetails userDetails = loadUserByUsername(user.getEmail());
        String token = jwtUtil.generateToken(userDetails);
        String refreshToken = issueRefreshToken(user, request);

        try {
            userActivityService.logActivity(
                    user,
                    ActivityType.LOGGED_IN,
                    "Logged in with Google"
            );
        } catch (Exception e) {
            logger.warn("Failed to log Google login activity for {}: {}", user.getEmail(), e.getMessage(), e);
        }

        AuthResponseDto response = mapUserToAuthResponseDto(user, token, refreshToken);
        response.setRequiresProfileCompletion(Boolean.FALSE.equals(user.getProfileCompleted()));
        return response;
    }

    @Transactional
    public AuthResponseDto registerWithGoogle(String credential, HttpServletRequest request) {
        String normalizedCredential = credential == null ? "" : credential.trim();
        if (normalizedCredential.isBlank()) {
            throw new UnauthorizedException("Google credential is required");
        }

        GoogleProfile profile = verifyGoogleCredential(normalizedCredential);
        User user = userRepository.findByEmail(profile.email()).orElse(null);

        if (user == null) {
            user = createGoogleUser(profile);
        } else if (Boolean.TRUE.equals(user.getProfileCompleted())) {
            throw new DuplicateResourceException("A FarmEazy account already exists for this Google email. Please sign in instead.");
        }

        if (user.getActive() == null || !user.getActive()) {
            throw new UnauthorizedException("User account is inactive");
        }

        UserDetails userDetails = loadUserByUsername(user.getEmail());
        String token = jwtUtil.generateToken(userDetails);
        String refreshToken = issueRefreshToken(user, request);

        AuthResponseDto response = mapUserToAuthResponseDto(user, token, refreshToken);
        response.setRequiresProfileCompletion(true);
        return response;
    }

    @Transactional
    public AuthResponseDto completeGoogleProfile(String userEmail, GoogleCompleteProfileDto dto, HttpServletRequest request) {
        if (userEmail == null || userEmail.isBlank()) {
            throw new UnauthorizedException("Authentication is required");
        }

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!"GOOGLE".equalsIgnoreCase(user.getAuthProvider())) {
            throw new UnauthorizedException("Profile completion is only available for Google sign-up accounts");
        }

        String desiredUsername = dto.getUsername() == null ? "" : dto.getUsername().trim();
        if (!desiredUsername.isBlank()) {
            if (!desiredUsername.matches("^[a-zA-Z0-9_ ]*$")) {
                throw new UnauthorizedException("Username can only contain letters, numbers, underscores, and spaces");
            }
            if (desiredUsername.length() < 3) {
                throw new UnauthorizedException("Username must be at least 3 characters");
            }
            if (!desiredUsername.equals(user.getUsername()) && userRepository.existsByUsername(desiredUsername)) {
                throw new DuplicateResourceException("Username '" + desiredUsername + "' is already taken. Please choose another username.");
            }
            user.setUsername(desiredUsername);
        }

        String password = dto.getPassword() == null ? "" : dto.getPassword().trim();
        if (!password.isBlank()) {
            if (password.length() < 6) {
                throw new UnauthorizedException("Password must be at least 6 characters");
            }
            user.setPassword(passwordEncoder.encode(password));
        }

        user.setPhone(dto.getPhone() == null ? null : dto.getPhone().trim());
        user.setAddress(dto.getAddress());
        user.setCity(dto.getCity());
        user.setState(dto.getState());
        user.setPinCode(dto.getPinCode());
        user.setProfileCompleted(true);
        user = userRepository.saveAndFlush(user);

        UserDetails userDetails = loadUserByUsername(user.getEmail());
        String token = jwtUtil.generateToken(userDetails);
        String refreshToken = issueRefreshToken(user, request);

        try {
            userActivityService.logActivity(
                    user,
                    ActivityType.REGISTERED,
                    "Registered a new account (Google signup completed)"
            );
        } catch (Exception e) {
            logger.warn("Failed to log Google registration activity for {}: {}", user.getEmail(), e.getMessage(), e);
        }

        triggerWelcomeCommunicationsAfterCommit(user);

        AuthResponseDto response = mapUserToAuthResponseDto(user, token, refreshToken);
        response.setRequiresProfileCompletion(false);
        return response;
    }

    @Transactional
    public void deferGoogleProfileCompletion(String userEmail) {
        if (userEmail == null || userEmail.isBlank()) {
            throw new UnauthorizedException("Authentication is required");
        }

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!"GOOGLE".equalsIgnoreCase(user.getAuthProvider())) {
            throw new UnauthorizedException("Profile completion defer is only available for Google sign-up accounts");
        }

        if (Boolean.TRUE.equals(user.getProfileCompleted())) {
            return;
        }

        try {
            userActivityService.logActivity(
                    user,
                    ActivityType.PROFILE_COMPLETION_DEFERRED,
                    "Deferred Google account setup"
            );
        } catch (Exception e) {
            logger.warn("Failed to log Google profile defer activity for {}: {}", user.getEmail(), e.getMessage(), e);
        }
    }

    @Transactional
    public AuthResponseDto refreshAccessToken(String rawRefreshToken, HttpServletRequest request) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new UnauthorizedException("Refresh token is required");
        }

        String currentHash = hashToken(rawRefreshToken);
        RefreshToken storedToken = refreshTokenRepository.findByTokenHashAndRevokedFalse(currentHash)
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (storedToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            storedToken.setRevoked(true);
            refreshTokenRepository.save(storedToken);
            throw new UnauthorizedException("Refresh token expired");
        }

        User user = storedToken.getUser();
        if (user == null || user.getActive() == null || !user.getActive()) {
            throw new UnauthorizedException("User account is inactive");
        }

        UserDetails userDetails = loadUserByUsername(user.getEmail());
        String newAccessToken = jwtUtil.generateToken(userDetails);
        String newRefreshToken = issueRefreshToken(user, request);

        storedToken.setRevoked(true);
        storedToken.setLastUsedAt(LocalDateTime.now());
        storedToken.setReplacedByHash(hashToken(newRefreshToken));
        refreshTokenRepository.save(storedToken);

        return mapUserToAuthResponseDto(user, newAccessToken, newRefreshToken);
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }
        String tokenHash = hashToken(rawRefreshToken);
        refreshTokenRepository.findByTokenHashAndRevokedFalse(tokenHash).ifPresent(token -> {
            token.setRevoked(true);
            token.setLastUsedAt(LocalDateTime.now());
            refreshTokenRepository.save(token);
        });
    }
    
    /**
     * RESOLVE USER BY IDENTIFIER
     * 
     * PURPOSE: Finds user by email, username, or user ID.
     * Supports flexible login with any of these identifiers.
     * 
     * @param identifier Email, username, or numeric user ID
     * @return User entity
     * @throws IllegalArgumentException if user not found
     */
    private User resolveUserByIdentifier(String identifier) {
        User user = null;
        
        // Check if identifier is a numeric user ID
        if (identifier.matches("^\\d+$")) {
            try {
                Long userId = Long.parseLong(identifier);
                user = userRepository.findById(userId).orElse(null);
            } catch (NumberFormatException e) {
                // Not a valid number, continue to email/username lookup
            }
        }
        
        // If not found by ID, try email
        if (user == null) {
            user = userRepository.findByEmail(identifier).orElse(null);
        }
        
        // If not found by email, try username
        if (user == null) {
            user = userRepository.findByUsername(identifier).orElse(null);
        }
        
        if (user == null) {
            throw new IllegalArgumentException("User not found with email, username, or user ID: " + identifier);
        }
        
        return user;
    }

    private GoogleProfile verifyGoogleCredential(String credential) {
        if (googleOAuthClientId == null || googleOAuthClientId.isBlank()) {
            throw new UnauthorizedException("Google sign-in is not configured");
        }

        try {
            String url = "https://oauth2.googleapis.com/tokeninfo?id_token=" + URLEncoder.encode(credential, StandardCharsets.UTF_8);
            String payload = new RestTemplate().getForObject(url, String.class);
            if (payload == null || payload.isBlank()) {
                throw new UnauthorizedException("Unable to validate Google sign-in");
            }

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode root = objectMapper.readTree(payload);

            String audience = root.path("aud").asText("");
            if (!googleOAuthClientId.equals(audience)) {
                throw new UnauthorizedException("Google sign-in client mismatch");
            }

            boolean emailVerified = root.path("email_verified").asBoolean(false)
                    || "true".equalsIgnoreCase(root.path("email_verified").asText());
            if (!emailVerified) {
                throw new UnauthorizedException("Google email is not verified");
            }

            String email = root.path("email").asText("").trim();
            if (email.isBlank()) {
                throw new UnauthorizedException("Google account email is missing");
            }

            String displayName = firstNonBlank(
                    root.path("name").asText("").trim(),
                    root.path("given_name").asText("").trim(),
                    email.split("@")[0]
            );

            return new GoogleProfile(email, displayName);
        } catch (UnauthorizedException e) {
            throw e;
        } catch (Exception e) {
            throw new UnauthorizedException("Google sign-in failed: " + e.getMessage());
        }
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "google_user";
    }

    @Transactional
    private User createGoogleUser(GoogleProfile profile) {
        User user = new User();
        user.setEmail(profile.email());
        user.setUsername(generateUniqueGoogleUsername(profile.email(), profile.displayName()));
        user.setPassword(passwordEncoder.encode(generateGoogleSeedPassword()));
        user.setActive(true);
        user.setAuthProvider("GOOGLE");
        user.setProfileCompleted(false);

        Set<String> roles = new HashSet<>();
        roles.add("USER");
        user.setRoles(roles);

        return userRepository.saveAndFlush(user);
    }

    private String generateUniqueGoogleUsername(String email, String displayName) {
        String source = displayName != null && !displayName.isBlank() ? displayName : email.split("@")[0];
        String normalized = source.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_");
        normalized = normalized.replaceAll("_+", "_").replaceAll("^_+|_+$", "");
        if (normalized.isBlank()) {
            normalized = "google_user";
        }

        String candidate = normalized;
        int suffix = 1;
        while (userRepository.existsByUsername(candidate)) {
            candidate = normalized + "_" + suffix++;
        }
        return candidate;
    }

    private String generateGoogleSeedPassword() {
        byte[] bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private void triggerWelcomeCommunications(User user) {
        try {
            notificationService.sendWelcomeNotification(user);
        } catch (Exception e) {
            logger.warn("Failed to send welcome notification for user {}: {}", user.getEmail(), e.getMessage(), e);
        }

        try {
            logger.info("Triggering welcome email for new user: {} <{}>", user.getUsername(), user.getEmail());
            httpEmailService.sendWelcomeEmailAsync(
                    user.getEmail(),
                    user.getUsername(),
                    user.getEmail(),
                    user.getId() == null ? null : user.getId().toString(),
                    user.getPhone(),
                    user.getCreatedAt() == null ? null : user.getCreatedAt().format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"))
            );
        } catch (Exception e) {
            logger.warn("Failed to trigger welcome email for {}: {}", user.getEmail(), e.getMessage(), e);
        }

        try {
            if (user.getPhone() != null && !user.getPhone().isEmpty()) {
                logger.info("Triggering welcome SMS for new user: {} (phone={})", user.getUsername(), user.getPhone());
                SmsResponseDto smsResponse = smsService.sendWelcome(user.getPhone(), user.getUsername());
                if (!smsResponse.isSuccess()) {
                    logger.warn("Welcome SMS failed for {}: {} (display: {})", user.getPhone(), smsResponse.getMessage(), smsResponse.getDisplayMessage());
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to send welcome SMS to {}: {}", user.getPhone(), e.getMessage(), e);
        }
    }

    private void triggerWelcomeCommunicationsAfterCommit(User user) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    triggerWelcomeCommunications(user);
                }
            });
            return;
        }
        triggerWelcomeCommunications(user);
    }

    /**
     * MAP USER TO AUTH RESPONSE DTO
     * 
     * PURPOSE: Converts User entity to AuthResponseDto for API response.
     * Extracts relevant user info and includes JWT token.
     * 
     * PARAMETERS:
     * - user: User entity from database
     * - token: JWT token generated by JwtUtil
     * 
     * RETURNS: AuthResponseDto ready to send to client
     * 
     * WHAT IT DOES:
     * - Copies user ID, email, fullName, roles from entity
     * - Includes JWT token for API authentication
     * - Sets token type to "Bearer" (standard JWT format)
     * - Client will use: Authorization: "Bearer <token>"
     * 
     * WHY SEPARATE: Keeps Service-to-DTO conversion logic centralized
     */
    private AuthResponseDto mapUserToAuthResponseDto(User user, String token, String refreshToken) {
        AuthResponseDto response = new AuthResponseDto();
        response.setId(user.getId());
        response.setEmail(user.getEmail());
        response.setUsername(user.getUsername());
        response.setRoles(user.getRoles());
        response.setToken(token);
        response.setRefreshToken(refreshToken);
        response.setRequiresProfileCompletion(false);
        return response;
    }

    private String issueRefreshToken(User user, HttpServletRequest request) {
        String rawToken = generateRawRefreshToken();
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setTokenHash(hashToken(rawToken));
        refreshToken.setExpiresAt(LocalDateTime.now().plusNanos(refreshTokenExpirationMs * 1_000_000));
        refreshToken.setRevoked(false);
        if (request != null) {
            refreshToken.setUserAgent(request.getHeader("User-Agent"));
            refreshToken.setIpAddress(request.getRemoteAddr());
        }
        refreshTokenRepository.save(refreshToken);
        return rawToken;
    }

    private record GoogleProfile(String email, String displayName) {}

    @Transactional(readOnly = true)
    public Map<String, Object> checkRegistrationAvailability(String username, String email, String phone) {
        String normalizedUsername = username == null ? "" : username.trim();
        String normalizedEmail = email == null ? "" : email.trim();
        String normalizedPhone = phone == null ? "" : phone.trim();

        boolean usernameAvailable = !userRepository.existsByUsername(normalizedUsername);
        boolean emailAvailable = !userRepository.existsByEmail(normalizedEmail);
        boolean phoneAvailable = !userRepository.existsByPhone(normalizedPhone);

        Map<String, Object> response = new HashMap<>();
        response.put("usernameAvailable", usernameAvailable);
        response.put("emailAvailable", emailAvailable);
        response.put("phoneAvailable", phoneAvailable);
        response.put("available", usernameAvailable && emailAvailable && phoneAvailable);

        if (!usernameAvailable) {
            response.put("message", "Username is already taken");
        } else if (!emailAvailable) {
            response.put("message", "Email is already registered");
        } else if (!phoneAvailable) {
            response.put("message", "Phone number is already registered");
        } else {
            response.put("message", "All fields are available");
        }

        return response;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getOtpLoginUserPreview(String phone) {
        String normalizedPhone = phone == null ? "" : phone.trim();
        Map<String, Object> response = new HashMap<>();

        var userOpt = resolveUserByPhone(normalizedPhone);
        if (userOpt.isEmpty()) {
            response.put("exists", false);
            response.put("message", "This phone number is not registered. Please sign up first.");
            return response;
        }

        User user = userOpt.get();
        if (user.getActive() == null || !user.getActive()) {
            response.put("exists", false);
            response.put("message", "User account is inactive");
            return response;
        }

        response.put("exists", true);
        response.put("username", user.getUsername());
        response.put("userId", user.getId());
        response.put("maskedPhone", maskPhone(normalizedPhone));
        response.put("message", "User found. Confirm details before requesting OTP.");
        return response;
    }

    private String generateRawRefreshToken() {
        byte[] bytes = new byte[REFRESH_TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", e);
        }
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) {
            return "****";
        }
        return phone.substring(0, 2) + "****" + phone.substring(phone.length() - 2);
    }

    private java.util.Optional<User> resolveUserByPhone(String phone) {
        List<User> users = userRepository.findAllByPhone(phone);
        if (users == null || users.isEmpty()) {
            return java.util.Optional.empty();
        }
        if (users.size() > 1) {
            logger.warn("Duplicate users found for phone {}. Selecting first active user.", maskPhone(phone));
        }
        return users.stream()
                .filter(user -> user.getActive() == null || user.getActive())
                .findFirst()
                .or(() -> users.stream().findFirst());
    }

    /**
     * FORGOT PASSWORD - REQUEST PASSWORD RESET
     * 
     * PURPOSE: Sends password reset link via email when user requests it.
     * Generates a short code that maps to JWT token for cleaner URLs.
     * 
     * @param email User's email address
     * @throws ResourceNotFoundException if email not found in system
     * 
     * WORKFLOW:
     * 1. Check if email exists in database
     * 2. Generate short 8-character code (e.g., "a3x9m2k7")
     * 3. Store mapping: short code -> full JWT token
    * 4. Send email with short URL ({APP_BASE_URL}/r/a3x9m2k7)
     * 5. User clicks, frontend fetches full token, redirects to reset page
     */
    @Transactional
    public void forgotPassword(String email) {
        forgotPassword(email, null, null, null);
    }

    @Transactional
    public void forgotPassword(String email, String ipAddress, String location, String deviceInfo) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Email not found in system"));

        if (user.getActive() == null || !user.getActive()) {
            throw new UnauthorizedException("User account is inactive");
        }

        // Delete any existing reset tokens for this email
        passwordResetTokenRepository.deleteByEmail(email);

        // Generate full JWT reset token
        UserDetails userDetails = loadUserByUsername(email);
        String resetToken = jwtUtil.generateResetToken(userDetails);

        // Generate unique short code and store mapping
        String shortCode = generateUniqueShortCode();
        PasswordResetToken resetTokenEntity = new PasswordResetToken();
        resetTokenEntity.setShortCode(shortCode);
        resetTokenEntity.setFullToken(resetToken);
        resetTokenEntity.setEmail(email);
        resetTokenEntity.setCreatedAt(LocalDateTime.now());
        resetTokenEntity.setExpiresAt(LocalDateTime.now().plusHours(1));
        resetTokenEntity.setUsed(false);
        passwordResetTokenRepository.save(resetTokenEntity);

        // Send email with short URL using HTTP-based email service (works on Render)
        // Exception will propagate if email sending fails - this is intentional
        // so the frontend can show appropriate error message
        httpEmailService.sendPasswordResetEmail(user.getEmail(), shortCode, ipAddress, location, deviceInfo);
    }

    /**
     * GENERATE UNIQUE SHORT CODE
     * Keeps trying until unique code found
     */
    private String generateUniqueShortCode() {
        String shortCode;
        do {
            shortCode = generateShortCode();
        } while (passwordResetTokenRepository.findByShortCode(shortCode).isPresent());
        return shortCode;
    }

    /**
     * GENERATE SHORT CODE
     * Creates random 8-character alphanumeric string
     */
    private String generateShortCode() {
        StringBuilder code = new StringBuilder(SHORT_CODE_LENGTH);
        for (int i = 0; i < SHORT_CODE_LENGTH; i++) {
            code.append(SHORT_CODE_CHARS.charAt(RANDOM.nextInt(SHORT_CODE_CHARS.length())));
        }
        return code.toString();
    }

    /**
     * GET FULL TOKEN BY SHORT CODE
     * Retrieves full JWT token from database using short code
     */
    @Transactional(readOnly = true)
    public String getFullTokenByShortCode(String shortCode) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new UnauthorizedException("Invalid or expired reset link"));

        if (resetToken.isUsed()) {
            throw new UnauthorizedException("Reset link has already been used");
        }

        if (resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new UnauthorizedException("Reset link has expired");
        }

        return resetToken.getFullToken();
    }

    /**
     * RESET PASSWORD - CHANGE PASSWORD WITH TOKEN
     * 
     * PURPOSE: Updates user password after verifying reset token.
     * Token must be valid and not expired.
     * 
     * @param token Reset token from email link
     * @param newPassword New password to set
     * @throws IllegalArgumentException if token invalid or expired
     * @throws ResourceNotFoundException if user not found
     * 
     * WORKFLOW:
     * 1. Validate and extract email from reset token
     * 2. Check if email exists
     * 3. Encrypt new password
     * 4. Update user password in database
     * 5. Send confirmation email
     * 6. Return success message
     */
    @Transactional
    public void resetPassword(String token, String newPassword) {
        // Validate token and extract email
        String email = jwtUtil.extractEmailFromToken(token);
        
        if (!jwtUtil.validateToken(token)) {
            throw new IllegalArgumentException("Invalid or expired reset token");
        }

        // Find user
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Encrypt and update password
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Send confirmation email asynchronously - does not block response
        String message = "Your password has been successfully reset. "
                + "If you did not make this change, please contact support immediately.";
        httpEmailService.sendNotificationAsync(user.getEmail(), user.getUsername(),
            "Password Reset Confirmation - FarmEazy", message);
    }

    /**
     * LOAD USER BY IDENTIFIER (SPRING SECURITY)
     * Supports login with email, username, or user ID
     */
    @Override
    public UserDetails loadUserByUsername(String identifier) {
        // Try to find user by different methods:
        // 1. If identifier is numeric, try to find by user ID first
        // 2. Try to find by email
        // 3. Try to find by username
        User user = null;
        
        // Check if identifier is a numeric user ID
        if (identifier.matches("^\\d+$")) {
            try {
                Long userId = Long.parseLong(identifier);
                user = userRepository.findById(userId).orElse(null);
            } catch (NumberFormatException e) {
                // Not a valid number, continue to email/username lookup
            }
        }
        
        // If not found by ID, try email
        if (user == null) {
            user = userRepository.findByEmail(identifier).orElse(null);
        }
        
        // If not found by email, try username
        if (user == null) {
            user = userRepository.findByUsername(identifier)
                    .orElseThrow(() -> new IllegalArgumentException("User not found with email, username, or user ID: " + identifier));
        }

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                user.getActive(),
                true,
                true,
                true,
                org.springframework.security.core.authority.AuthorityUtils.createAuthorityList(
                        user.getRoles().stream()
                                .map(r -> {
                                    String normalized = r == null ? "" : r.trim();
                                    if (normalized.isEmpty()) {
                                        return normalized;
                                    }
                                    return normalized.startsWith("ROLE_") ? normalized : "ROLE_" + normalized;
                                })
                                .filter(role -> !role.isEmpty())
                                .toArray(String[]::new)
                )
        );
    }
    
    /**
     * SUGGEST USERNAME - PROVIDE USERNAME OPTIONS
     * 
     * PURPOSE: Generates multiple username suggestions for user to choose from.
     * Helps users pick a username if they don't want auto-generated one.
     * 
     * @param email User's email address
     * @param phone User's phone number
     * @return Set of 3-5 available username suggestions
     * 
     * SUGGESTION STRATEGIES:
     * 1. emailPrefix_phoneDigits (default)
     * 2. emailPrefix + random 3-digit
     * 3. emailPrefix + random 4-digit
     * 
     * RETURNS: Set<String> with 3 unique available usernames
     */
    public Set<String> suggestUsernames(String email, String phone) {
        Set<String> suggestions = new HashSet<>();
        
        String emailPrefix = email.substring(0, email.indexOf('@'))
                .replaceAll("[^a-zA-Z0-9]", "_")
                .toLowerCase();
        
        // Truncate email prefix to ensure final username doesn't exceed 20 characters
        // Max username length: 20 chars
        // Format: emailPrefix + "_" + suffix (where suffix can be 3-5 digits)
        // So emailPrefix max should be: 20 - 1 (underscore) - 4 (digits) = 15 chars
        if (emailPrefix.length() > 15) {
            emailPrefix = emailPrefix.substring(0, 15);
        }
        
        String phoneDigits = phone.substring(phone.length() - 4);
        
        // Suggestion 1: emailPrefix_phoneDigits
        String suggestion1 = emailPrefix + "_" + phoneDigits;
        if (!userRepository.existsByUsername(suggestion1)) {
            suggestions.add(suggestion1);
        }
        
        // Suggestion 2: emailPrefix + random 3-digit
        for (int i = 0; i < 10 && suggestions.size() < 3; i++) {
            String suggestion = emailPrefix + "_" + (100 + RANDOM.nextInt(900));
            if (!userRepository.existsByUsername(suggestion)) {
                suggestions.add(suggestion);
            }
        }
        
        // Suggestion 3: emailPrefix + random 4-digit
        for (int i = 0; i < 10 && suggestions.size() < 3; i++) {
            String suggestion = emailPrefix + "_" + (1000 + RANDOM.nextInt(9000));
            if (!userRepository.existsByUsername(suggestion)) {
                suggestions.add(suggestion);
            }
        }
        
        // Ensure at least 3 suggestions
        while (suggestions.size() < 3) {
            String suggestion = emailPrefix + "_" + RANDOM.nextInt(100000);
            if (!userRepository.existsByUsername(suggestion)) {
                suggestions.add(suggestion);
            }
        }
        
        return suggestions;
    }
    
    // ========== OTP-BASED LOGIN ==========
    
    /**
     * LOGIN WITH OTP - PHONE-BASED LOGIN
     * 
     * PURPOSE: Logs in user using phone number + OTP code.
     * Alternative to password-based login.
     * 
     * WORKFLOW:
     * 1. Verify OTP code is valid for phone
     * 2. Find user by phone number
     * 3. Generate JWT token
     * 4. Log activity
     * 5. Return AuthResponseDto with token
     * 
     * @param phone 10-digit phone number
     * @param otpCode 6-digit OTP code
     * @return AuthResponseDto with JWT token
     * @throws UnauthorizedException if OTP invalid/expired or user not found
     */
    public AuthResponseDto loginWithOtp(String phone, String otpCode) {
        // Create a trace identifier to link OTP verification -> welcome email -> welcome SMS
        String traceId = java.util.UUID.randomUUID().toString();
        org.slf4j.MDC.put("traceId", traceId);

        // Verify OTP once; if it was already verified in a prior step and is still valid,
        // continue without forcing a second verify that can fail and poison transactional flows.
        try {
            logger.info("[traceId={}] Verifying OTP for phone {}", traceId, phone);
            if (otpService.isVerifiedLoginOtpStillValid(phone, otpCode)) {
                logger.info("[traceId={}] Using previously verified OTP for phone {}", traceId, phone);
            } else {
                otpService.verifyLoginOtp(phone, otpCode);
                logger.info("[traceId={}] OTP verified successfully for phone {}", traceId, phone);
            }
        } catch (UnauthorizedException ue) {
            logger.warn("[traceId={}] OTP verification failed for phone {}: {}", traceId, phone, ue.getMessage());
            throw ue;
        }

        // Find user by phone
        User user = resolveUserByPhone(phone)
            .orElseThrow(() -> new UnauthorizedException("User not found with this phone number"));
        
        // Check if user is active
        if (user.getActive() == null || !user.getActive()) {
            throw new UnauthorizedException("User account is inactive");
        }
        
        // Generate JWT token
        UserDetails userDetails = loadUserByUsername(user.getEmail());
        String token = jwtUtil.generateToken(userDetails);
        
        // Log login activity
        try {
            userActivityService.logActivity(
                user,
                ActivityType.LOGGED_IN,
                "Logged in via OTP"
            );
        } catch (Exception e) {
            logger.warn("Failed to log OTP login activity for {}: {}", user.getEmail(), e.getMessage(), e);
        }

        // If user was just created (e.g., registration flow + OTP), send welcome communications
        try {
            if (user.getCreatedAt() != null && user.getCreatedAt().isAfter(java.time.LocalDateTime.now().minusMinutes(10))) {
                logger.info("[traceId={}] New user detected via OTP login (created recently). Sending welcome email/sms: {}", traceId, user.getEmail());
                httpEmailService.sendWelcomeEmailAsync(
                        user.getEmail(),
                        user.getUsername(),
                        user.getEmail(),
                        user.getId() == null ? null : user.getId().toString(),
                        user.getPhone(),
                        user.getCreatedAt() == null ? null : user.getCreatedAt().format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"))
                );
                if (user.getPhone() != null && !user.getPhone().isEmpty()) {
                    smsService.sendWelcome(user.getPhone(), user.getUsername());
                }
            }
        } catch (Exception e) {
            logger.warn("[traceId={}] Failed to send welcome communications after OTP login for {}: {}", traceId, user.getEmail(), e.getMessage(), e);
        }

        // Return response
        try {
            return mapUserToAuthResponseDto(user, token, null);
        } finally {
            // Clear trace ID after request is finished
            org.slf4j.MDC.remove("traceId");
        }
    }
}

