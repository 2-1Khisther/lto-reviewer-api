package com.lto.api.entity;

import jakarta.persistence.*;

// Maps to tbl_session_item_logs: one answered (or skipped) question in an exam.
@Entity
@Table(name = "tbl_session_item_logs")
public class SessionItemLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer logId;

    private Integer sessionId;
    private Integer questionId;
    private Integer selectedChoice;   // 1=A, 2=B, 3=C, 4=D, null = skipped
    private Boolean isCorrect;        // null = skipped
    private Integer responseTimeSec = 0;

    public Integer getLogId() { return logId; }
    public Integer getSessionId() { return sessionId; }
    public void setSessionId(Integer sessionId) { this.sessionId = sessionId; }
    public Integer getQuestionId() { return questionId; }
    public void setQuestionId(Integer questionId) { this.questionId = questionId; }
    public Integer getSelectedChoice() { return selectedChoice; }
    public void setSelectedChoice(Integer selectedChoice) { this.selectedChoice = selectedChoice; }
    public Boolean getIsCorrect() { return isCorrect; }
    public void setIsCorrect(Boolean isCorrect) { this.isCorrect = isCorrect; }
    public Integer getResponseTimeSec() { return responseTimeSec; }
    public void setResponseTimeSec(Integer responseTimeSec) { this.responseTimeSec = responseTimeSec; }
}
