package com.farmeazy.service;

import com.farmeazy.entity.Farm;
import com.farmeazy.entity.IrrigationSchedule;
import com.farmeazy.entity.User;
import com.farmeazy.repository.FarmRepository;
import com.farmeazy.repository.IrrigationScheduleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Irrigation Scheduler Task - Runs automatically to send reminders
 * 
 * Executes daily at 6:00 AM to notify farmers about upcoming/overdue irrigations
 */
@Service
@EnableScheduling
public class IrrigationSchedulerTask {

    private static final Logger logger = LoggerFactory.getLogger(IrrigationSchedulerTask.class);

    @Autowired
    private IrrigationScheduleRepository irrigationRepository;

    @Autowired
    private FarmRepository farmRepository;

    @Autowired
    private SmsService smsService;

    @Autowired
    private EmailService emailService;

    /**
     * Send irrigation reminders - Runs daily at 6:00 AM
     */
    @Scheduled(cron = "${irrigation.reminder.schedule.cron:0 0 6 * * *}")
    @Transactional
    public void sendDailyIrrigationReminders() {
        logger.info("Starting daily irrigation reminder task");

        try {
            List<Farm> activeFarms = farmRepository.findAll();
            int remindersCount = 0;
            int successCount = 0;
            int failureCount = 0;

            for (Farm farm : activeFarms) {
                try {
                    LocalDate today = LocalDate.now();
                    LocalDate maxWindowEnd = today.plusDays(7);
                    List<IrrigationSchedule> dueIrrigations = irrigationRepository
                            .findByFarmIdAndIrrigationDateBetween(farm.getId(), today, maxWindowEnd);

                    for (IrrigationSchedule irrigation : dueIrrigations) {
                        if (!"SCHEDULED".equalsIgnoreCase(irrigation.getStatus()) && !"PENDING".equalsIgnoreCase(irrigation.getStatus())) {
                            continue;
                        }
                        if (Boolean.FALSE.equals(irrigation.getReminderEnabled())) {
                            continue;
                        }

                        LocalDate irrigDate = irrigation.getIrrigationDate();
                        if (irrigDate == null) {
                            continue;
                        }

                        int reminderDaysBefore = irrigation.getReminderDaysBefore() != null ? irrigation.getReminderDaysBefore() : 1;
                        long daysUntilDue = ChronoUnit.DAYS.between(today, irrigDate);

                        if (daysUntilDue < 0 || daysUntilDue > reminderDaysBefore) {
                            continue;
                        }
                        if (today.equals(irrigation.getLastReminderSentDate())) {
                            continue;
                        }

                        try {
                            sendReminderToUser(farm, irrigation);
                            irrigation.setLastReminderSentDate(today);
                            irrigationRepository.save(irrigation);
                            successCount++;
                        } catch (Exception e) {
                            logger.warn("Failed to send reminder for farm {}", farm.getId(), e);
                            failureCount++;
                        }
                        remindersCount++;
                    }
                } catch (Exception e) {
                    logger.error("Error processing farm {}", farm.getId(), e);
                    failureCount++;
                }
            }

            logger.info("Irrigation reminders completed - Total: {}, Success: {}, Failed: {}", 
                    remindersCount, successCount, failureCount);

        } catch (Exception e) {
            logger.error("Fatal error in irrigation reminder task", e);
        }
    }

    private void sendReminderToUser(Farm farm, IrrigationSchedule irrigation) throws Exception {
        User user = farm.getUser();
        String cropName = irrigation.getCrop() != null ? irrigation.getCrop().getCropName() : "Unknown";
        LocalDate irrigDate = irrigation.getIrrigationDate();
        Double waterQty = irrigation.getWaterAmount();

        String message = String.format(
                "Irrigation reminder: Irrigate %s on %s with %.0f liters. Farm: %s",
                cropName, irrigDate, waterQty != null ? waterQty : 0, farm.getFarmName()
        );

        try {
            if (user.getPhone() != null && !user.getPhone().isEmpty()) {
                smsService.sendIrrigationReminder(user.getPhone(), irrigation.getId().toString(), farm.getFarmName());
                logger.debug("Irrigation reminder SMS sent to {}", user.getPhone());
            }
        } catch (Exception e) {
            logger.warn("Irrigation reminder SMS send failed", e);
        }

        try {
            if (user.getEmail() != null && !user.getEmail().isEmpty()) {
                String scheduledTime = irrigDate + " " + irrigation.getStartTime();
                emailService.sendIrrigationReminder(user.getEmail(), user.getUsername(), farm.getFarmName(), cropName, scheduledTime);
                logger.debug("Irrigation reminder email sent to {}", user.getEmail());
            }
        } catch (Exception e) {
            logger.warn("Irrigation reminder email send failed", e);
        }

        logger.info("Reminder sent for farm {}", farm.getId());
    }
}

