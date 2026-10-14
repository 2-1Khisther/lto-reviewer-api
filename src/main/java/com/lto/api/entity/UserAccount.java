package com.lto.api.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

// Maps to tbl_user_profile (server version, with email and password_hash for login).
@Entity
@Table(name = "tbl_user_profile")
public class UserAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer userId;

    private String fullName;
    private String email;
    private String passwordHash;
    private String targetLicense = "NON_PROF";
    private String languagePref = "ENGLISH";
    private Boolean isOnboarded = false;
    private Boolean isActive = true;
    private Integer currentStreak = 0;
    private Integer longestStreak = 0;
    private LocalDate lastActiveDate;

    @Column(insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public Integer getUserId() { return userId; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getTargetLicense() { return targetLicense; }
    public void setTargetLicense(String targetLicense) { this.targetLicense = targetLicense; }
    public String getLanguagePref() { return languagePref; }
    public void setLanguagePref(String languagePref) { this.languagePref = languagePref; }
    public Boolean getIsOnboarded() { return isOnboarded; }
    public void setIsOnboarded(Boolean isOnboarded) { this.isOnboarded = isOnboarded; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    public Integer getCurrentStreak() { return currentStreak; }
    public void setCurrentStreak(Integer currentStreak) { this.currentStreak = currentStreak; }
    public Integer getLongestStreak() { return longestStreak; }
    public void setLongestStreak(Integer longestStreak) { this.longestStreak = longestStreak; }
    public LocalDate getLastActiveDate() { return lastActiveDate; }
    public void setLastActiveDate(LocalDate lastActiveDate) { this.lastActiveDate = lastActiveDate; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
