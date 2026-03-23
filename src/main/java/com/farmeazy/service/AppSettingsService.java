package com.farmeazy.service;

import com.farmeazy.entity.AppSettings;
import com.farmeazy.repository.AppSettingsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.Optional;

@Service
public class AppSettingsService {

    @Autowired
    private AppSettingsRepository appSettingsRepository;

    @PostConstruct
    public void ensureDefaultSettings() {
        if (!appSettingsRepository.existsById(1L)) {
            AppSettings defaults = new AppSettings();
            defaults.setId(1L);
            appSettingsRepository.save(defaults);
        }
    }

    public AppSettings getSettings() {
        return appSettingsRepository.findById(1L).orElseGet(() -> {
            AppSettings defaults = new AppSettings();
            defaults.setId(1L);
            return appSettingsRepository.save(defaults);
        });
    }

    public AppSettings updateSettings(AppSettings settings) {
        settings.setId(1L);
        settings.setUpdatedAt(java.time.LocalDateTime.now());
        return appSettingsRepository.save(settings);
    }
}
