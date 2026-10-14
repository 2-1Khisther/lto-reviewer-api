package com.lto.api.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

// Maps to tbl_question_bank (4 choices; correctChoice 1=A, 2=B, 3=C, 4=D).
@Entity
@Table(name = "tbl_question_bank")
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer questionId;

    private Integer domainId;
    private String licenseCategory;   // NON_PROF or PROF
    private String vehicleType;       // MOTORCYCLE, LIGHT, HEAVY, BUS
    private String questionText;
    private String graphicPath;
    private String choiceA;
    private String choiceB;
    private String choiceC;
    private String choiceD;
    private Integer correctChoice;
    private String explanationText;
    private String sourceReference;
    private Boolean isActive = true;

    @Column(insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    public Integer getQuestionId() { return questionId; }
    public Integer getDomainId() { return domainId; }
    public void setDomainId(Integer domainId) { this.domainId = domainId; }
    public String getLicenseCategory() { return licenseCategory; }
    public void setLicenseCategory(String licenseCategory) { this.licenseCategory = licenseCategory; }
    public String getVehicleType() { return vehicleType; }
    public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }
    public String getQuestionText() { return questionText; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }
    public String getGraphicPath() { return graphicPath; }
    public void setGraphicPath(String graphicPath) { this.graphicPath = graphicPath; }
    public String getChoiceA() { return choiceA; }
    public void setChoiceA(String choiceA) { this.choiceA = choiceA; }
    public String getChoiceB() { return choiceB; }
    public void setChoiceB(String choiceB) { this.choiceB = choiceB; }
    public String getChoiceC() { return choiceC; }
    public void setChoiceC(String choiceC) { this.choiceC = choiceC; }
    public String getChoiceD() { return choiceD; }
    public void setChoiceD(String choiceD) { this.choiceD = choiceD; }
    public Integer getCorrectChoice() { return correctChoice; }
    public void setCorrectChoice(Integer correctChoice) { this.correctChoice = correctChoice; }
    public String getExplanationText() { return explanationText; }
    public void setExplanationText(String explanationText) { this.explanationText = explanationText; }
    public String getSourceReference() { return sourceReference; }
    public void setSourceReference(String sourceReference) { this.sourceReference = sourceReference; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
