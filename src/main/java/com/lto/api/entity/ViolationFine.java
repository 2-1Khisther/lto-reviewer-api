package com.lto.api.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

// Maps to tbl_violation_fines: one row per offense tier (1st, 2nd, 3rd, 4th+).
@Entity
@Table(name = "tbl_violation_fines")
public class ViolationFine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer fineId;

    private Integer offenseNumber;
    private BigDecimal fineAmount;
    private String additionalPenalty;

    public Integer getFineId() { return fineId; }
    public Integer getOffenseNumber() { return offenseNumber; }
    public void setOffenseNumber(Integer offenseNumber) { this.offenseNumber = offenseNumber; }
    public BigDecimal getFineAmount() { return fineAmount; }
    public void setFineAmount(BigDecimal fineAmount) { this.fineAmount = fineAmount; }
    public String getAdditionalPenalty() { return additionalPenalty; }
    public void setAdditionalPenalty(String additionalPenalty) { this.additionalPenalty = additionalPenalty; }
}
