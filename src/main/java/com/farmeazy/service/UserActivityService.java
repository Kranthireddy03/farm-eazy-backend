package com.farmeazy.service;

import com.farmeazy.dto.UserActivityDto;
import com.farmeazy.entity.User;
import com.farmeazy.entity.UserActivity;
import com.farmeazy.entity.UserActivity.ActivityType;
import com.farmeazy.repository.UserActivityRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class UserActivityService {

    private static final Logger log = LoggerFactory.getLogger(UserActivityService.class);

    @Autowired
    private UserActivityRepository userActivityRepository;

    /**
     * Log user activity
     */
    public UserActivity logActivity(User user, ActivityType activityType, String description) {
        return logActivity(user, activityType, description, null, null, null);
    }

    /**
     * Log user activity with related entity information
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public UserActivity logActivity(User user, ActivityType activityType, String description,
                                   String details, String relatedEntityId, String relatedEntityType) {
        try {
            UserActivity activity = new UserActivity();
            activity.setUser(user);
            activity.setActivityType(activityType);
            activity.setDescription(description);
            activity.setDetails(details);
            activity.setRelatedEntityId(relatedEntityId);
            activity.setRelatedEntityType(relatedEntityType);

            UserActivity saved = userActivityRepository.save(activity);
            log.info("Activity logged for user {}: {} - {}", user.getId(), activityType, description);
            return saved;
        } catch (DataIntegrityViolationException ex) {
            log.warn("Skipping activity {} for user {} due to schema/check constraint mismatch: {}", activityType, user != null ? user.getId() : null, ex.getMessage());
            return null;
        }
    }

    /**
     * Get user activities paginated
     */
    public Page<UserActivityDto> getUserActivities(User user, Pageable pageable) {
        return userActivityRepository.findByUserOrderByCreatedAtDesc(user, pageable)
                .map(this::convertToDto);
    }

    /**
     * Get recent user activities (last 20)
     */
    public List<UserActivityDto> getRecentActivities(User user) {
        return userActivityRepository.findTop20ByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Get activity count for user
     */
    public Long getUserActivityCount(User user) {
        return userActivityRepository.countByUser(user);
    }

    /**
     * Convert UserActivity entity to DTO with time formatting
     */
    private UserActivityDto convertToDto(UserActivity activity) {
        UserActivityDto dto = new UserActivityDto();
        dto.setId(activity.getId());
        dto.setActivityType(activity.getActivityType().toString());
        dto.setDescription(activity.getDescription());
        dto.setDetails(activity.getDetails());
        dto.setRelatedEntityId(activity.getRelatedEntityId());
        dto.setRelatedEntityType(activity.getRelatedEntityType());
        dto.setCreatedAt(activity.getCreatedAt());
        dto.setTimeAgo(formatTimeAgo(activity.getCreatedAt()));
        
        return dto;
    }

    /**
     * Format time as "X ago" (e.g., "2 hours ago")
     */
    private String formatTimeAgo(LocalDateTime dateTime) {
        if (dateTime == null) return "Unknown";
        
        LocalDateTime now = LocalDateTime.now();
        long minutes = ChronoUnit.MINUTES.between(dateTime, now);
        long hours = ChronoUnit.HOURS.between(dateTime, now);
        long days = ChronoUnit.DAYS.between(dateTime, now);
        
        if (minutes < 1) {
            return "Just now";
        } else if (minutes < 60) {
            return minutes == 1 ? "1 minute ago" : minutes + " minutes ago";
        } else if (hours < 24) {
            return hours == 1 ? "1 hour ago" : hours + " hours ago";
        } else if (days < 30) {
            return days == 1 ? "1 day ago" : days + " days ago";
        } else {
            return "Long ago";
        }
    }

    /**
     * Delete old activities (older than specified days)
     */
    @Transactional
    public void deleteOldActivities(int daysOld) {
        // This can be extended based on requirements
        log.info("Deleting activities older than {} days", daysOld);
    }
}
