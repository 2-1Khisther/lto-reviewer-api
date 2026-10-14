package com.lto.api.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// Maps to tbl_violations. Its fine tiers (tbl_violation_fines) load together with it.
@Entity
@Table(name = "tbl_violations")
public class Violation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer violationId;

    private String violationName;
    private String description;
    private String legalBasis;
    private String imagePath;
    private Integer sortOrder = 0;
    private Boolean isActive = true;

    @Column(insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "violation_id", nullable = false)
    @OrderBy("offenseNumber")
    private List<ViolationFine> fines = new ArrayList<>();

    public Integer getViolationId() { return violationId; }
    public String getViolationName() { return violationName; }
    public void setViolationName(String violationName) { this.violationName = violationName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getLegalBasis() { return legalBasis; }
    public void setLegalBasis(String legalBasis) { this.legalBasis = legalBasis; }
    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public List<ViolationFine> getFines() { return fines; }
}
