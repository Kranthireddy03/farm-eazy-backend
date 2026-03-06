package com.farmeazy.controller;

import com.farmeazy.dto.AuthLoginDto;
import com.farmeazy.dto.AuthRegisterDto;
import com.farmeazy.dto.AuthResponseDto;
import com.farmeazy.dto.ForgotPasswordDto;
import com.farmeazy.dto.ResetPasswordDto;
import com.farmeazy.dto.OtpRequestDto;
import com.farmeazy.dto.OtpVerifyDto;
import com.farmeazy.service.AuthService;
import com.farmeazy.service.OtpService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = {
    "https://farm-eazy.com",
    "https://www.farm-eazy.com",
    "http://localhost:4200",
    "http://localhost:3000",
    "http://localhost:3001",
    "http://localhost:5173"
}, allowCredentials = "true")
@Tag(name = "Authentication", description = "User authentication endpoints")
public class AuthController {
    @Autowired
    private AuthService authService;

    @Autowired
    private OtpService otpService;

    /**
     * REQUEST OTP ENDPOINT
     */
    @PostMapping("/request-otp")
    @Operation(summary = "Request OTP via email and SMS")
    public ResponseEntity<?> requestOtp(@Valid @RequestBody OtpRequestDto otpRequestDto) {
        String result = otpService.generateAndSendOtp(otpRequestDto);
        return ResponseEntity.ok(new java.util.HashMap<String, String>() {{
            put("message", result);
        }});
    }

    /**
     * VERIFY OTP ENDPOINT
     */
    @PostMapping("/verify-otp")
    @Operation(summary = "Verify OTP for email/phone")
    public ResponseEntity<?> verifyOtp(@Valid @RequestBody OtpVerifyDto otpVerifyDto) {
        boolean verified = otpService.verifyOtp(otpVerifyDto);
        return ResponseEntity.ok(new java.util.HashMap<String, Object>() {{
            put("verified", verified);
        }});
    }

    /**
     * USER REGISTRATION ENDPOINT
     * HTTP METHOD: POST
     * ENDPOINT PATH: /api/auth/register
     * ACCESS: Public (no authentication required)
     * PURPOSE: Creates a new user account and returns JWT token for immediate login.
     * REQUEST BODY (JSON):
     * {
     *     "fullName": "Rajesh Kumar",
     *     "email": "rajesh@example.com",
     *     "password": "SecurePass123",
     *     "phone": "9876543210",
     *     "address": "123 Farm Lane",
     *     "city": "Mumbai",
     *     "state": "Maharashtra",
     *     "pinCode": "400001"
     * }
     * VALIDATION APPLIED:
     * - fullName: Required, non-blank
     * - email: Required, valid email format
     * - password: Required, minimum 6 characters
     * - phone: Required, 10 digits format
     * - Other fields: Optional
     * RESPONSE (201 CREATED):
     * {
     *     "id": 1,
     *     "email": "rajesh@example.com",
     *     "fullName": "Rajesh Kumar",
     *     "roles": ["USER"],
     *     "token": "eyJhbGciOiJIUzUxMiJ9...",
     *     "tokenType": "Bearer"
     * }
     * ERROR RESPONSES:
     * - 400 BAD REQUEST: Validation failed (missing/invalid fields)
     * - 409 CONFLICT: Email already registered
     * PROCESS:
     * 1. Validate request body against AuthRegisterDto constraints
     * 2. Call authService.register() to create user
     * 3. Return 201 status with user info and JWT token
     * 4. Client stores token and uses for future authenticated requests
     * @param registerDto User registration data (validated by @Valid)
     * @return ResponseEntity with AuthResponseDto (201 CREATED)
     */
    @PostMapping("/register")
    @Operation(summary = "Register a new user")
    public ResponseEntity<AuthResponseDto> register(@Valid @RequestBody AuthRegisterDto registerDto) {
        // Call service to register user and get response with token
        AuthResponseDto response = authService.register(registerDto);
        // Return 201 CREATED status (user successfully created)
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * USER LOGIN ENDPOINT
     * 
     * HTTP METHOD: POST
     * ENDPOINT PATH: /api/auth/login
     * ACCESS: Public (no authentication required)
     * 
     * PURPOSE: Authenticates user with email/password and returns JWT token.
     * Used for all subsequent API calls.
     * 
     * REQUEST BODY (JSON):
     * {
     *     "email": "rajesh@example.com",
     *     "password": "SecurePass123"
     * }
     * 
     * VALIDATION APPLIED:
     * - email: Required, valid email format
     * - password: Required, minimum 6 characters
     * 
     * RESPONSE (200 OK):
     * {
     *     "id": 1,
     *     "email": "rajesh@example.com",
     *     "fullName": "Rajesh Kumar",
     *     "roles": ["USER"],
     *     "token": "eyJhbGciOiJIUzUxMiJ9...",
     *     "tokenType": "Bearer"
     * }
     * 
     * ERROR RESPONSES:
     * - 400 BAD REQUEST: Validation failed
     * - 401 UNAUTHORIZED: Invalid email/password
     * 
     * PROCESS:
     * 1. Validate request body against AuthLoginDto constraints
     * 2. Call authService.login() to authenticate
     * 3. Return 200 status with user info and JWT token
     * 4. Client includes token in future requests:
     *    Authorization: Bearer <token>
     * 
     * @param loginDto User login credentials (validated by @Valid)
     * @return ResponseEntity with AuthResponseDto (200 OK)
     */
    @PostMapping("/login")
    @Operation(summary = "Login user")
    public ResponseEntity<AuthResponseDto> login(@Valid @RequestBody AuthLoginDto loginDto) {
        // Call service to authenticate user and get response with token
        AuthResponseDto response = authService.login(loginDto);
        // Return 200 OK status with user info and token
        return ResponseEntity.ok(response);
    }

    /**
     * USERNAME SUGGESTION ENDPOINT
     * 
     * HTTP METHOD: POST
     * ENDPOINT PATH: /api/auth/suggest-username
     * ACCESS: Public (no authentication required)
     * 
     * PURPOSE: Generates unique username suggestions based on email and phone.
     * Helps users choose a username during registration.
     * 
     * REQUEST BODY (JSON):
     * {
     *     "email": "john@example.com",
     *     "phone": "9876543210"
     * }
     * 
     * RESPONSE (200 OK):
     * {
     *     "suggestions": [
     *         "john_3210",
     *         "john_456",
     *         "john_7890"
     *     ]
     * }
     * 
     * VALIDATION APPLIED:
     * - email: Required, valid email format
     * - phone: Required, exactly 10 digits
     * 
     * PROCESS:
     * 1. Validate email and phone format
     * 2. Call authService.suggestUsernames()
     * 3. Service generates 3-5 available usernames
     * 4. Return username options to user
     * 
     * @param suggestionDto Email and phone for generating suggestions
     * @return ResponseEntity with username suggestions (200 OK)
     */
    @PostMapping("/suggest-username")
    @Operation(summary = "Get username suggestions")
    public ResponseEntity<?> suggestUsername(@Valid @RequestBody com.farmeazy.dto.UsernameSuggestionDto suggestionDto) {
        // Generate username suggestions based on email and phone
        java.util.Set<String> suggestions = authService.suggestUsernames(
            suggestionDto.getEmail(), 
            suggestionDto.getPhone()
        );
        
        // Return suggestions as JSON response
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        response.put("suggestions", suggestions);
        return ResponseEntity.ok(response);
    }

    /**
     * FORGOT PASSWORD ENDPOINT
     * 
     * HTTP METHOD: POST
     * ENDPOINT PATH: /api/auth/forgot-password
     * ACCESS: Public (no authentication required)
     * 
     * PURPOSE: Sends password reset link to user's email.
     * 
     * REQUEST BODY (JSON):
     * {
     *     "email": "user@example.com"
     * }
     * 
     * RESPONSE (200 OK):
     * {
     *     "message": "Password reset link sent to your email"
     * }
     * 
     * ERROR RESPONSES:
     * - 404 NOT FOUND: Email not found in system
     * 
     * PROCESS:
     * 1. Validate email format
     * 2. Call authService.forgotPassword()
     * 3. Service generates reset token
     * 4. Service sends reset link via email
     * 5. Return success message
     * 
     * @param forgotPasswordDto Email address
     * @return ResponseEntity with success message
     */
    @PostMapping("/forgot-password")
    @Operation(summary = "Request password reset")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordDto forgotPasswordDto) {
        authService.forgotPassword(forgotPasswordDto.getEmail());
        return ResponseEntity.ok(new java.util.HashMap<String, String>() {{
            put("message", "Password reset link sent to your email. Please check your inbox.");
        }});
    }

    /**
     * RESET PASSWORD ENDPOINT
     * 
     * HTTP METHOD: POST
     * ENDPOINT PATH: /api/auth/reset-password
     * ACCESS: Public (only valid reset token required)
     * 
     * PURPOSE: Resets user password after verifying reset token.
     * 
     * REQUEST BODY (JSON):
     * {
     *     "token": "eyJhbGciOiJIUzUxMiJ9...",
     *     "newPassword": "NewPassword123"
     * }
     * 
     * RESPONSE (200 OK):
     * {
     *     "message": "Password reset successfully"
     * }
     * 
     * ERROR RESPONSES:
     * - 400 BAD REQUEST: Invalid token or password validation failed
     * - 404 NOT FOUND: User not found
     * 
     * PROCESS:
     * 1. Validate reset token and new password
     * 2. Call authService.resetPassword()
     * 3. Service verifies token
     * 4. Service updates password in database
     * 5. Service sends confirmation email
     * 6. Return success message
     * 
     * @param resetPasswordDto Token and new password
     * @return ResponseEntity with success message
     */
    @PostMapping("/reset-password")
    @Operation(summary = "Reset password with token")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordDto resetPasswordDto) {
        authService.resetPassword(resetPasswordDto.getToken(), resetPasswordDto.getNewPassword());
        return ResponseEntity.ok(new java.util.HashMap<String, String>() {{
            put("message", "Password reset successfully");
        }});
    }
    
    /**
     * REDIRECT SHORT CODE TO FULL TOKEN
     * 
     * HTTP METHOD: GET
     * ENDPOINT PATH: /api/auth/r/{shortCode}
     * ACCESS: Public (no authentication required)
     * 
     * PURPOSE: Retrieves full JWT token from database using short code.
     * Used by password reset flow to convert short URL to full reset token.
     * 
     * URL PARAMETER:
     * - shortCode: 8-character alphanumeric code (e.g., "a3x9m2k7")
     * 
     * RESPONSE (200 OK):
     * {
     *     "token": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ1c2VyQGV4YW1wbGUuY29tIiwiaWF0IjoxNjQwOTk1MjAwLCJleHAiOjE2NDA5OTg4MDB9.abc123..."
     * }
     * 
     * ERROR RESPONSES:
     * - 404 NOT FOUND: Invalid or expired short code
     * - 401 UNAUTHORIZED: Reset link already used or expired
     * 
     * PROCESS:
     * 1. Extract short code from URL path
     * 2. Look up short code in database
     * 3. Verify not expired and not already used
     * 4. Return full JWT token
     * 5. Frontend uses token to redirect to /reset-password?token=...s
     * 
     * @param shortCode 8-character short code
     * @return ResponseEntity with full JWT token
     */
    @GetMapping("/r/{shortCode}")
    @Operation(summary = "Get full reset token from short code")
    public ResponseEntity<?> getResetToken(@PathVariable String shortCode) {
        String fullToken = authService.getFullTokenByShortCode(shortCode);
        return ResponseEntity.ok(new java.util.HashMap<String, String>() {{
            put("token", fullToken);
        }});
    }
    
    // ========== OTP-BASED LOGIN ==========
    
    /**
     * REQUEST OTP FOR LOGIN
     * 
     * HTTP METHOD: POST
     * ENDPOINT PATH: /api/auth/login/request-otp
     * ACCESS: Public (no authentication required)
     * 
     * PURPOSE: Sends LOGIN_OTP via SMS to registered phone number.
     * First step in OTP-based login flow.
     * 
     * REQUEST BODY (JSON):
     * {
     *     "phone": "9876543210"
     * }
     * 
     * RESPONSE (200 OK):
     * {
     *     "success": true,
     *     "message": "OTP sent to your registered mobile number.",
     *     "displayMessage": "OTP sent to 98****10. Valid for 10 minutes."
     * }
     * 
     * ERROR RESPONSES:
     * - 400 BAD REQUEST: Phone format invalid
     * - 404 NOT FOUND: Phone number not registered
     * 
     * @param dto Phone number request
     * @return OTP response with send status
     */
    @PostMapping("/login/request-otp")
    @Operation(summary = "Request OTP for phone login")
    public ResponseEntity<?> requestLoginOtp(@Valid @RequestBody com.farmeazy.dto.OtpLoginRequestDto dto) {
        com.farmeazy.dto.OtpResponseDto response = otpService.generateLoginOtp(dto.getPhone());
        return ResponseEntity.ok(response);
    }
    
    /**
     * VERIFY OTP AND LOGIN
     * 
     * HTTP METHOD: POST
     * ENDPOINT PATH: /api/auth/login/verify-otp
     * ACCESS: Public (requires valid OTP)
     * 
     * PURPOSE: Verifies OTP and returns JWT token for login.
     * Second step in OTP-based login flow.
     * 
     * REQUEST BODY (JSON):
     * {
     *     "phone": "9876543210",
     *     "otpCode": "123456"
     * }
     * 
     * RESPONSE (200 OK):
     * {
     *     "id": 1,
     *     "email": "user@example.com",
     *     "username": "farmer_1234",
     *     "roles": ["USER"],
     *     "token": "eyJhbGciOiJIUzUxMiJ9..."
     * }
     * 
     * ERROR RESPONSES:
     * - 400 BAD REQUEST: Invalid phone or OTP format
     * - 401 UNAUTHORIZED: Invalid/expired OTP
     * 
     * @param dto Phone and OTP code
     * @return AuthResponseDto with JWT token
     */
    @PostMapping("/login/verify-otp")
    @Operation(summary = "Verify OTP and login")
    public ResponseEntity<AuthResponseDto> verifyOtpAndLogin(@Valid @RequestBody com.farmeazy.dto.OtpLoginVerifyDto dto) {
        AuthResponseDto response = authService.loginWithOtp(dto.getPhone(), dto.getOtpCode());
        return ResponseEntity.ok(response);
    }
}


