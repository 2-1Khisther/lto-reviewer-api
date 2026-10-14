package com.lto.api.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

// Maps to tbl_exam_sessions: one finished mock exam uploaded by the app.
@Entity
@Table(name = "tbl_exam_sessions")
public class ExamSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer sessionId;

    private String clientSessionId;   // UUID made by the phone (prevents duplicate uploads)
    private Integer userId;
    private String licenseCategory;   // NON_PROF or PROF
    private String vehicleType;       // MOTORCYCLE, LIGHT, HEAVY, BUS
    private Long startTimestamp;      // epoch milliseconds
    private Long endTimestamp;
    private Integer timeAllocated = 3600;
    private Integer totalItems = 60;
    private Integer rawScore = 0;
    private Integer passingScore = 48;
    private Boolean isCompleted = false;
    private Boolean isPassed = false;

    @Column(insertable = false, updatable = false)
    private LocalDateTime uploadedAt;

    public Integer getSessionId() { return sessionId; }
    public String getClientSessionId() { return clientSessionId; }
    public void setClientSessionId(String clientSessionId) { this.clientSessionId = clientSessionId; }
    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }
    public String getLicenseCategory() { return licenseCategory; }
    public void setLicenseCategory(String licenseCategory) { this.licenseCategory = licenseCategory; }
    public String getVehicleType() { return vehicleType; }
    public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }
    public Long getStartTimestamp() { return startTimestamp; }
    public void setStartTimestamp(Long startTimestamp) { this.startTimestamp = startTimestamp; }
    public Long getEndTimestamp() { return endTimestamp; }
    public void setEndTimestamp(Long endTimestamp) { this.endTimestamp = endTimestamp; }
    public Integer getTimeAllocated() { return timeAllocated; }
    public void setTimeAllocated(Integer timeAllocated) { this.timeAllocated = timeAllocated; }
    public Integer getTotalItems() { return totalItems; }
    public void setTotalItems(Integer totalItems) { this.totalItems = totalItems; }
    public Integer getRawScore() { return rawScore; }
    public void setRawScore(Integer rawScore) { this.rawScore = rawScore; }
    public Integer getPassingScore() { return passingScore; }
    public void setPassingScore(Integer passingScore) { this.passingScore = passingScore; }
    public Boolean getIsCompleted() { return isCompleted; }
    public void setIsCompleted(Boolean isCompleted) { this.isCompleted = isCompleted; }
    public Boolean getIsPassed() { return isPassed; }
    public void setIsPassed(Boolean isPassed) { this.isPassed = isPassed; }
    public LocalDateTime getUploadedAt() { return uploadedAt; }
}
