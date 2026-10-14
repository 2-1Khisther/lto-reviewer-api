package com.lto.api.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

// Maps to tbl_admins (admin web panel accounts, separate from app users).
@Entity
@Table(name = "tbl_admins")
public class Admin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer adminId;

    private String fullName;
    private String email;
    private String passwordHash;
    private Boolean isActive = true;

    @Column(insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public Integer getAdminId() { return adminId; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
