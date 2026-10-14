package com.lto.api.entity;

import jakarta.persistence.*;

// Maps to tbl_app_settings: the Settings screen toggles plus custom exam length.
@Entity
@Table(name = "tbl_app_settings")
public class AppSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer settingId;

    private Integer userId;
    private Integer customExamLength = 60;
    private Boolean notificationsEnabled = true;
    private Boolean soundEffects = true;
    private Boolean vibrationEnabled = true;
    private Boolean darkMode = false;
    private Boolean offlineModeReady = true;

    public Integer getSettingId() { return settingId; }
    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }
    public Integer getCustomExamLength() { return customExamLength; }
    public void setCustomExamLength(Integer customExamLength) { this.customExamLength = customExamLength; }
    public Boolean getNotificationsEnabled() { return notificationsEnabled; }
    public void setNotificationsEnabled(Boolean notificationsEnabled) { this.notificationsEnabled = notificationsEnabled; }
    public Boolean getSoundEffects() { return soundEffects; }
    public void setSoundEffects(Boolean soundEffects) { this.soundEffects = soundEffects; }
    public Boolean getVibrationEnabled() { return vibrationEnabled; }
    public void setVibrationEnabled(Boolean vibrationEnabled) { this.vibrationEnabled = vibrationEnabled; }
    public Boolean getDarkMode() { return darkMode; }
    public void setDarkMode(Boolean darkMode) { this.darkMode = darkMode; }
    public Boolean getOfflineModeReady() { return offlineModeReady; }
    public void setOfflineModeReady(Boolean offlineModeReady) { this.offlineModeReady = offlineModeReady; }
}
