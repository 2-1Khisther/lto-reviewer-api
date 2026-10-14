package com.lto.api.controller;

import com.lto.api.entity.ExamSession;
import com.lto.api.repository.ExamSessionRepository;
import com.lto.api.security.CurrentUser;
import com.lto.api.service.ExamService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

// POST /api/exam-sessions  -> upload a finished exam (201 new, 200 if already uploaded)
// GET  /api/exam-sessions  -> the logged-in user's exam history, newest first
@RestController
@RequestMapping("/api/exam-sessions")
public class ExamController {

    public record SessionDto(int sessionId, String clientSessionId, String licenseCategory, String vehicleType,
                             long startTimestamp, Long endTimestamp, int totalItems, int rawScore,
                             int passingScore, boolean isCompleted, boolean isPassed, LocalDateTime uploadedAt) {
        static SessionDto from(ExamSession s) {
            return new SessionDto(s.getSessionId(), s.getClientSessionId(), s.getLicenseCategory(),
                    s.getVehicleType(), s.getStartTimestamp(), s.getEndTimestamp(), s.getTotalItems(),
                    s.getRawScore(), s.getPassingScore(), Boolean.TRUE.equals(s.getIsCompleted()),
                    Boolean.TRUE.equals(s.getIsPassed()), s.getUploadedAt());
        }
    }

    public record UploadResponse(int sessionId, int rawScore, int totalItems, int passingScore,
                                 boolean isPassed, boolean alreadyUploaded) {}

    private final ExamService examService;
    private final ExamSessionRepository sessions;

    public ExamController(ExamService examService, ExamSessionRepository sessions) {
        this.examService = examService;
        this.sessions = sessions;
    }

    @PostMapping
    public ResponseEntity<UploadResponse> upload(@RequestBody ExamService.UploadRequest body,
                                                 HttpServletRequest request) {
        int userId = CurrentUser.requireUserId(request);
        ExamService.UploadResult result = examService.upload(userId, body);
        ExamSession s = result.session();
        UploadResponse response = new UploadResponse(s.getSessionId(), s.getRawScore(), s.getTotalItems(),
                s.getPassingScore(), Boolean.TRUE.equals(s.getIsPassed()), result.duplicate());
        return ResponseEntity.status(result.duplicate() ? HttpStatus.OK : HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public List<SessionDto> history(HttpServletRequest request) {
        int userId = CurrentUser.requireUserId(request);
        return sessions.findByUserIdOrderByStartTimestampDesc(userId).stream().map(SessionDto::from).toList();
    }
}
