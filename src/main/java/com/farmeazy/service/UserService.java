package com.farmeazy.service;

import com.farmeazy.entity.User;
import com.farmeazy.exception.DuplicateResourceException;
import com.farmeazy.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class UserService {

    private static final Pattern PHONE_10_DIGIT = Pattern.compile("^[0-9]{10}$");

    @Autowired
    private UserRepository userRepository;

    public User findByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }

    public User updateVendorOnboardingProfile(User user,
                                              String phone,
                                              String address,
                                              String city,
                                              String state,
                                              String pinCode) {
        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }

        String normalizedPhone = normalize(phone);
        String normalizedAddress = normalize(address);
        String normalizedCity = normalize(city);
        String normalizedState = normalize(state);
        String normalizedPinCode = normalize(pinCode);

        if (normalizedPhone == null || !PHONE_10_DIGIT.matcher(normalizedPhone).matches()) {
            throw new IllegalArgumentException("Phone number is required and must be 10 digits");
        }
        if (normalizedAddress == null) {
            throw new IllegalArgumentException("Address is required");
        }
        if (normalizedCity == null) {
            throw new IllegalArgumentException("City is required");
        }
        if (normalizedState == null) {
            throw new IllegalArgumentException("State is required");
        }

        Optional<User> existingByPhone = userRepository.findByPhone(normalizedPhone);
        if (existingByPhone.isPresent() && !existingByPhone.get().getId().equals(user.getId())) {
            throw new DuplicateResourceException("Phone number already registered. Please use a different number.");
        }

        user.setPhone(normalizedPhone);
        user.setAddress(normalizedAddress);
        user.setCity(normalizedCity);
        user.setState(normalizedState);
        user.setPinCode(normalizedPinCode);

        return userRepository.save(user);
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
