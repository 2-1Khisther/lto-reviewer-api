package com.lto.api.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

// Maps to tbl_domain_performance_analytics: running totals per user per domain,
// used for the Profile screen statistics.
@Entity
@Table(name = "tbl_domain_performance_analytics")
public class DomainPerformanceAnalytics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer analyticsId;

    private Integer userId;
    private Integer domainId;
    private Integer totalAttempted = 0;   // answered (not skipped)
    private Integer totalCorrect = 0;
    private Integer totalIncorrect = 0;
    private Integer totalSkipped = 0;
    private Integer totalMastered = 0;    // questions answered correctly on every attempt
    private BigDecimal accuracyRate = BigDecimal.ZERO;

    @Column(insertable = false, updatable = false)
    private LocalDateTime lastUpdated;

    public Integer getAnalyticsId() { return analyticsId; }
    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }
    public Integer getDomainId() { return domainId; }
    public void setDomainId(Integer domainId) { this.domainId = domainId; }
    public Integer getTotalAttempted() { return totalAttempted; }
    public void setTotalAttempted(Integer totalAttempted) { this.totalAttempted = totalAttempted; }
    public Integer getTotalCorrect() { return totalCorrect; }
    public void setTotalCorrect(Integer totalCorrect) { this.totalCorrect = totalCorrect; }
    public Integer getTotalIncorrect() { return totalIncorrect; }
    public void setTotalIncorrect(Integer totalIncorrect) { this.totalIncorrect = totalIncorrect; }
    public Integer getTotalSkipped() { return totalSkipped; }
    public void setTotalSkipped(Integer totalSkipped) { this.totalSkipped = totalSkipped; }
    public Integer getTotalMastered() { return totalMastered; }
    public void setTotalMastered(Integer totalMastered) { this.totalMastered = totalMastered; }
    public BigDecimal getAccuracyRate() { return accuracyRate; }
    public void setAccuracyRate(BigDecimal accuracyRate) { this.accuracyRate = accuracyRate; }
    public LocalDateTime getLastUpdated() { return lastUpdated; }
}
