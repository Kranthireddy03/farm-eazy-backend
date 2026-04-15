package com.farmeazy.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class GoogleCompleteProfileDto {

    @Size(max = 30, message = "Username cannot exceed 30 characters")
    @Pattern(regexp = "^$|^[a-zA-Z0-9_ ]*$", message = "Username can only contain letters, numbers, underscores, and spaces")
    private String username;

    private String password;

    @jakarta.validation.constraints.NotBlank(message = "Phone is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "Phone must be 10 digits")
    private String phone;

    private String address;
    private String city;
    private String state;
    private String pinCode;

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