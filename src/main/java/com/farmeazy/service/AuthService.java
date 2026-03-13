package com.farmeazy.service;

import com.farmeazy.dto.AuthLoginDto;
import com.farmeazy.dto.AuthRegisterDto;
import com.farmeazy.dto.AuthResponseDto;
import com.farmeazy.entity.PasswordResetToken;
import com.farmeazy.entity.User;
import com.farmeazy.entity.UserActivity.ActivityType;
import com.farmeazy.exception.DuplicateResourceException;
import com.farmeazy.exception.UnauthorizedException;
import com.farmeazy.repository.PasswordResetTokenRepository;
import com.farmeazy.repository.UserRepository;
import com.farmeazy.service.OtpService;
import com.farmeazy.service.NotificationService;
import com.farmeazy.security.JwtUtil;
import com.farmeazy.service.UserActivityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

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
    
    private static final String SHORT_CODE_CHARS = "abcdefghijklmnopqrstuvwxyz0123456789";
    private static final int SHORT_CODE_LENGTH = 8;
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

        // Assign default role for new users
        Set<String> roles = new HashSet<>();
        roles.add("USER");
        user.setRoles(roles);

        // Save user to database (use saveAndFlush to ensure ID is generated immediately)
        user = userRepository.saveAndFlush(user);

        // Send welcome email asynchronously (optional)
        httpEmailService.sendWelcomeEmailAsync(user.getEmail(), user.getUsername());

        // Send in-app welcome notification
        try {
            notificationService.sendWelcomeNotification(user);
        } catch (Exception e) {
            System.err.println("Failed to send welcome notification: " + e.getMessage());
        }

        // Log registration activity (only if user has valid ID)
        if (user.getId() != null) {
            try {
                userActivityService.logActivity(
                        user,
                        ActivityType.REGISTERED,
                        "Registered a new account (instant registration)"
                );
            } catch (Exception e) {
                System.err.println("Failed to log registration activity: " + e.getMessage());
            }
        }

        // Return response with user info and JWT token (no OTP required)
        UserDetails userDetails = loadUserByUsername(user.getEmail());
        String token = jwtUtil.generateToken(userDetails);
        return mapUserToAuthResponseDto(user, token);
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
    public AuthResponseDto login(AuthLoginDto loginDto) {
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
        return mapUserToAuthResponseDto(user, token);
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
    private AuthResponseDto mapUserToAuthResponseDto(User user, String token) {
        AuthResponseDto response = new AuthResponseDto();
        response.setId(user.getId());
        response.setEmail(user.getEmail());
        response.setUsername(user.getUsername());
        response.setRoles(user.getRoles());
        response.setToken(token);
        return response;
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
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new com.farmeazy.exception.ResourceNotFoundException("Email not found in system"));

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
        httpEmailService.sendPasswordResetEmail(user.getEmail(), shortCode);
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
                .orElseThrow(() -> new com.farmeazy.exception.ResourceNotFoundException("User not found"));

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
                                .map(r -> "ROLE_" + r)
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
    @Transactional
    public AuthResponseDto loginWithOtp(String phone, String otpCode) {
        // Verify OTP (throws UnauthorizedException if invalid)
        otpService.verifyLoginOtp(phone, otpCode);
        
        // Find user by phone
        User user = userRepository.findByPhone(phone)
            .orElseThrow(() -> new UnauthorizedException("User not found with this phone number"));
        
        // Check if user is active
        if (user.getActive() == null || !user.getActive()) {
            throw new UnauthorizedException("User account is inactive");
        }
        
        // Generate JWT token
        UserDetails userDetails = loadUserByUsername(user.getEmail());
        String token = jwtUtil.generateToken(userDetails);
        
        // Log activity
        try {
            userActivityService.logActivity(
                user,
                ActivityType.LOGGED_IN,
                "Logged in via OTP"
            );
        } catch (Exception e) {
            System.err.println("Failed to log OTP login activity: " + e.getMessage());
        }
        
        // Return response
        return mapUserToAuthResponseDto(user, token);
    }
}

