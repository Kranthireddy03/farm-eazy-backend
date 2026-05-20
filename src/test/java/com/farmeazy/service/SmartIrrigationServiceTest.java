package com.farmeazy.service;

import com.farmeazy.dto.IrrigationRecommendationDto;
import com.farmeazy.entity.Crop;
import com.farmeazy.entity.Farm;
import com.farmeazy.entity.IrrigationSchedule;
import com.farmeazy.entity.User;
import com.farmeazy.exception.ResourceNotFoundException;
import com.farmeazy.repository.CropIrrigationRuleRepository;
import com.farmeazy.repository.CropRepository;
import com.farmeazy.repository.FarmRepository;
import com.farmeazy.repository.IrrigationHistoryRepository;
import com.farmeazy.repository.IrrigationRemindersLogRepository;
import com.farmeazy.repository.IrrigationScheduleRepository;
import com.farmeazy.repository.UserRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SmartIrrigationServiceTest {

    @Mock
    private CropRepository cropRepository;

    @Mock
    private IrrigationScheduleRepository irrigationScheduleRepository;

    @Mock
    private CropIrrigationRuleRepository cropIrrigationRuleRepository;

    @Mock
    private IrrigationHistoryRepository irrigationHistoryRepository;

    @Mock
    private IrrigationRemindersLogRepository irrigationRemindersLogRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FarmRepository farmRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private SmsService smsService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private SmartIrrigationService smartIrrigationService;

    @Test
    void getRecommendation_throwsWhenLocationOutsidePilot() {
        User user = new User();
        user.setId(1L);

        Farm farm = new Farm();
        farm.setId(100L);
        farm.setUser(user);
        farm.setLocation("KURNOOL, ANDHRA PRADESH");

        Crop crop = new Crop();
        crop.setId(200L);
        crop.setFarm(farm);
        crop.setCropName("GROUNDNUT");

        when(cropRepository.findByIdAndFarmUserId(200L, 1L)).thenReturn(Optional.of(crop));
        when(userRepository.findByEmail("test@farmeazy.com")).thenReturn(Optional.of(user));

        ResourceNotFoundException ex = Assertions.assertThrows(
            ResourceNotFoundException.class,
            () -> smartIrrigationService.getIrrigationRecommendation(200L, "test@farmeazy.com")
        );

        Assertions.assertTrue(ex.getMessage().contains("Ananthapur"));
    }

    @Test
    void getRecommendation_throwsWhenCropOutsidePilot() {
        User user = new User();
        user.setId(1L);

        Farm farm = new Farm();
        farm.setId(101L);
        farm.setUser(user);
        farm.setLocation("Ananthapur, Andhra Pradesh");

        Crop crop = new Crop();
        crop.setId(201L);
        crop.setFarm(farm);
        crop.setCropName("BANANA");

        when(cropRepository.findByIdAndFarmUserId(201L, 1L)).thenReturn(Optional.of(crop));
        when(userRepository.findByEmail("test@farmeazy.com")).thenReturn(Optional.of(user));

        ResourceNotFoundException ex = Assertions.assertThrows(
            ResourceNotFoundException.class,
            () -> smartIrrigationService.getIrrigationRecommendation(201L, "test@farmeazy.com")
        );

        Assertions.assertTrue(ex.getMessage().contains("supports only"));
    }

    @Test
    void getRecommendation_returnsExistingScheduleForPilotScope() {
        User user = new User();
        user.setId(1L);

        Farm farm = new Farm();
        farm.setId(102L);
        farm.setUser(user);
        farm.setLocation("Ananthapur, Andhra Pradesh");

        Crop crop = new Crop();
        crop.setId(202L);
        crop.setFarm(farm);
        crop.setCropName("GROUNDNUT");

        IrrigationSchedule schedule = new IrrigationSchedule();
        schedule.setId(900L);
        schedule.setCrop(crop);
        schedule.setFarm(farm);
        schedule.setNextIrrigationDate(LocalDate.now().plusDays(2));
        schedule.setRecommendedWaterQuantityMm(34.0);
        schedule.setIntervalDays(6);
        schedule.setLastIrrigationDate(LocalDate.now().minusDays(4));

        when(cropRepository.findByIdAndFarmUserId(202L, 1L)).thenReturn(Optional.of(crop));
        when(userRepository.findByEmail("test@farmeazy.com")).thenReturn(Optional.of(user));
        when(irrigationScheduleRepository.findByCropId(202L)).thenReturn(Collections.singletonList(schedule));

        IrrigationRecommendationDto result = smartIrrigationService
            .getIrrigationRecommendation(202L, "test@farmeazy.com");

        Assertions.assertEquals(202L, result.getCropId());
        Assertions.assertEquals(34.0, result.getWaterQuantityMm());
        Assertions.assertEquals(6, result.getIntervalDays());
    }
}
