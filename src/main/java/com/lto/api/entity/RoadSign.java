package com.lto.api.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

// Maps to tbl_road_signs.
@Entity
@Table(name = "tbl_road_signs")
public class RoadSign {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer signId;

    private String signCategory;  // REGULATORY, WARNING, GUIDE, EXPRESSWAY, INSTRUCTION, HAZARD, ROADWORK
    private String signName;
    private String description;
    private String imagePath;
    private Integer sortOrder = 0;
    private Boolean isActive = true;

    @Column(insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    public Integer getSignId() { return signId; }
    public String getSignCategory() { return signCategory; }
    public void setSignCategory(String signCategory) { this.signCategory = signCategory; }
    public String getSignName() { return signName; }
    public void setSignName(String signName) { this.signName = signName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
