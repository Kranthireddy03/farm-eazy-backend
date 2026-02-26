package com.farmeazy.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

/**
 * AUTH REGISTER DTO (Data Transfer Object)
 * 
 * PURPOSE: Validates and transfers user registration data from the client to the server.
 * Acts as a contract for the registration API endpoint (/api/auth/register).
 * 
 * KEY FEATURES:
 * 1. Input Validation: Uses Jakarta validation annotations to ensure data quality
 * 2. Security: Validates password strength and email format
 * 3. User Contact: Collects personal information (name, phone, address)
 * 4. Geographic Data: Collects location information (city, state, pinCode)
 * 
 * HOW IT WORKS:
 * - Client sends registration request with JSON body mapped to this DTO
 * - Spring validates all @NotBlank, @Email, @Pattern, @Size annotations
 * - If validation fails, GlobalExceptionHandler returns error response
 * - If validation passes, AuthService.register() processes the request
 * - User is created in database and JWT token is returned
 * 
 * VALIDATION RULES:
 * - fullName: Required, non-blank string (e.g., "John Farmer")
 * - email: Required, must be valid email format (e.g., "john@example.com")
 * - password: Required, minimum 6 characters (e.g., "SecurePass123")
 * - phone: Required, exactly 10 digits (India format, e.g., "9876543210")
 * - address: Optional field
 * - city: Optional field
 * - state: Optional field
 * - pinCode: Optional field
 * 
 * USAGE EXAMPLE:
 * POST /api/auth/register
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
 */
public class AuthRegisterDto {
    
    /**
     * FULL NAME - USER'S COMPLETE NAME
     * @NotBlank: Field cannot be null or empty
     * Used as display name throughout the application
     */
    @NotBlank(message = "Full name is required")
    private String fullName;
    
    /**
     * EMAIL - UNIQUE LOGIN IDENTIFIER
     * @NotBlank: Field cannot be null or empty
     * @Email: Must be valid email format (checked by regex pattern)
     * Used as unique username for login and user identification
     * Example: "farmer@example.com"
     */
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;
    
    /**
     * USERNAME - UNIQUE DISPLAY NAME
     * Optional field (can be auto-generated if not provided)
     * @Size(min = 3, max = 20): Must be between 3-20 characters
     * @Pattern: Alphanumeric with underscores only
     * Example: "rajesh_9876" or "john_farmer"
     */
    @Size(min = 3, max = 20, message = "Username must be between 3 and 20 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_]*$", message = "Username can only contain letters, numbers, and underscores")
    private String username;
    
    /**
     * PASSWORD - LOGIN CREDENTIAL (ENCRYPTED)
     * @NotBlank: Field cannot be null or empty
     * @Size(min = 6): Minimum 6 characters for security
     * Password is encrypted by BCryptPasswordEncoder before storage
     * Never stored or transmitted in plain text
     * Example: "SecurePass123"
     */
    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;
    
    /**
     * PHONE - USER'S CONTACT NUMBER
     * @NotBlank: Field cannot be null or empty
     * @Pattern: Validates 10-digit format (India-specific)
     * Regex "^[0-9]{10}$" means: start with digit, exactly 10 digits, end
     * Example: "9876543210"
     */
    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "Phone must be 10 digits")
    private String phone;
    
    /**
     * ADDRESS - STREET ADDRESS (OPTIONAL)
     * Optional field for user's residential address
     * Example: "123 Farm Lane"
     */
    private String address;
    
    /**
     * CITY - CITY/TOWN NAME (OPTIONAL)
     * Optional field for user's city
     * Example: "Mumbai"
     */
    private String city;
    
    /**
     * STATE - STATE/PROVINCE NAME (OPTIONAL)
     * Optional field for user's state
     * Example: "Maharashtra"
     */
    private String state;
    
    /**
     * PIN CODE - POSTAL CODE (OPTIONAL)
     * Optional field for user's postal code
     * Example: "400001"
     */
    private String pinCode;
    
    public AuthRegisterDto() {}
    
    public AuthRegisterDto(String fullName, String email, String password, String phone, String address, String city, String state, String pinCode) {
        this.fullName = fullName;
        this.email = email;
        this.password = password;
        this.phone = phone;
        this.address = address;
        this.city = city;
        this.state = state;
        this.pinCode = pinCode;
    }
    
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public String getPinCode() { return pinCode; }
    public void setPinCode(String pinCode) { this.pinCode = pinCode; }
}
