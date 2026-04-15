package com.farmeazy.service;

import com.farmeazy.dto.CommunicationPreferenceDto;
import com.farmeazy.dto.CommunicationPreferenceResponseDto;
import com.farmeazy.entity.CommunicationPreference;
import com.farmeazy.entity.CommunicationPreference.CommunicationChannel;
import com.farmeazy.entity.CommunicationPreference.NotificationType;
import com.farmeazy.entity.User;
import com.farmeazy.exception.ResourceNotFoundException;
import com.farmeazy.repository.CommunicationPreferenceRepository;
import com.farmeazy.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * COMMUNICATION PREFERENCE SERVICE
 * 
 * PURPOSE: Manages user communication channel preferences.
 * Determines whether to send Email, SMS, or Both for each notification type.
 * 
 * BUSINESS RULES:
 * - Email is FREE and the default channel
 * - SMS costs ₹0.25 per message - requires user consent
 * - Marketing notifications are email-only (no SMS spam)
 * - Users must explicitly opt-in for SMS
 */
@Service
public class CommunicationPreferenceService {

    private static final Logger logger = LoggerFactory.getLogger(CommunicationPreferenceService.class);

    @Autowired
    private CommunicationPreferenceRepository preferenceRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Get user's communication preferences
     * Creates default preferences if none exist
     */
        @Transactional(readOnly = true)
    public CommunicationPreferenceResponseDto getPreferences(String userIdentifier) {
        User user = resolveUserForPreferences(userIdentifier);

        return preferenceRepository.findByUser(user)
            .map(CommunicationPreferenceResponseDto::fromEntity)
            .orElseGet(this::createDefaultResponse);
    }

    private User resolveUserForPreferences(String userIdentifier) {
        if (userIdentifier == null || userIdentifier.isBlank()) {
            throw new ResourceNotFoundException("User not found");
        }

        if (userIdentifier.contains("@")) {
            return userRepository.findByEmail(userIdentifier)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        }

        List<User> users = userRepository.findAllByPhone(userIdentifier);
        if (users.isEmpty()) {
            throw new ResourceNotFoundException("User not found");
        }

        if (users.size() > 1) {
            logger.warn("Multiple users found with phone {}. Using latest active record for preference lookup.", userIdentifier);
        }

        return users.stream()
                .filter(u -> Boolean.TRUE.equals(u.getActive()))
                .max((a, b) -> Long.compare(a.getId() == null ? 0L : a.getId(), b.getId() == null ? 0L : b.getId()))
                .orElse(users.get(users.size() - 1));
    }

    /**
     * Update user's communication preferences
     */
    @Transactional
    public CommunicationPreferenceResponseDto updatePreferences(String userEmail, CommunicationPreferenceDto dto) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        CommunicationPreference prefs = preferenceRepository.findByUser(user)
                .orElseGet(() -> createDefaultPreferences(user));

        // Update preferences
        prefs.setOtpChannel(dto.getOtpChannel());
        prefs.setOrderChannel(dto.getOrderChannel());
        prefs.setServiceChannel(dto.getServiceChannel());
        prefs.setIrrigationChannel(dto.getIrrigationChannel());
        
        // Marketing is always email-only (no SMS spam)
        prefs.setMarketingChannel(CommunicationChannel.EMAIL_ONLY);
        
        // Update SMS consent
        prefs.setSmsConsent(dto.getSmsConsent());

        // If SMS consent revoked, reset all channels to email-only
        if (!Boolean.TRUE.equals(dto.getSmsConsent())) {
            prefs.setOtpChannel(CommunicationChannel.EMAIL_ONLY);
            prefs.setOrderChannel(CommunicationChannel.EMAIL_ONLY);
            prefs.setServiceChannel(CommunicationChannel.EMAIL_ONLY);
            prefs.setIrrigationChannel(CommunicationChannel.EMAIL_ONLY);
        }

        CommunicationPreference saved = preferenceRepository.save(prefs);
        logger.info("Updated communication preferences for user: {} - SMS consent: {}", 
                user.getEmail(), dto.getSmsConsent());

        return CommunicationPreferenceResponseDto.fromEntity(saved);
    }

    /**
     * Check if SMS should be sent for a notification type
     */
    public boolean shouldSendSms(User user, NotificationType type) {
        return preferenceRepository.findByUser(user)
                .map(pref -> pref.shouldSendSms(type))
                .orElse(true); // Default: send SMS until user sets preferences
    }

    /**
     * Check if Email should be sent for a notification type
     */
    public boolean shouldSendEmail(User user, NotificationType type) {
        return preferenceRepository.findByUser(user)
                .map(pref -> pref.shouldSendEmail(type))
                .orElse(true); // Default: always send email (it's free)
    }

    /**
     * Get communication channels for a notification type
     * Returns which channels to use for sending
     */
    public CommunicationChannels getChannels(User user, NotificationType type) {
        CommunicationPreference prefs = preferenceRepository.findByUser(user)
                .orElse(null);

        if (prefs == null) {
            // Default: send both email and SMS until user sets preferences
            return new CommunicationChannels(true, true);
        }

        return new CommunicationChannels(
                prefs.shouldSendEmail(type),
                prefs.shouldSendSms(type)
        );
    }

    /**
     * Create default preferences for a user (send both email + SMS until user chooses)
     */
    private CommunicationPreference createDefaultPreferences(User user) {
        CommunicationPreference prefs = new CommunicationPreference(user);
        prefs.setOtpChannel(CommunicationChannel.BOTH);
        prefs.setOrderChannel(CommunicationChannel.BOTH);
        prefs.setServiceChannel(CommunicationChannel.BOTH);
        prefs.setIrrigationChannel(CommunicationChannel.BOTH);
        prefs.setMarketingChannel(CommunicationChannel.BOTH);
        prefs.setSmsConsent(true);
        return preferenceRepository.save(prefs);
    }

    private CommunicationPreferenceResponseDto createDefaultResponse() {
        CommunicationPreferenceResponseDto dto = new CommunicationPreferenceResponseDto();
        dto.setOtpChannel(CommunicationChannel.BOTH);
        dto.setOrderChannel(CommunicationChannel.BOTH);
        dto.setServiceChannel(CommunicationChannel.BOTH);
        dto.setIrrigationChannel(CommunicationChannel.BOTH);
        dto.setMarketingChannel(CommunicationChannel.BOTH);
        dto.setSmsConsent(true);
        return dto;
    }

    /**
     * Simple record to hold channel decisions
     */
    public record CommunicationChannels(boolean sendEmail, boolean sendSms) {}
}
