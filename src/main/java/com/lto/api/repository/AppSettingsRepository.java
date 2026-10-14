package com.lto.api.repository;

import com.lto.api.entity.AppSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AppSettingsRepository extends JpaRepository<AppSettings, Integer> {
    Optional<AppSettings> findByUserId(Integer userId);
}
