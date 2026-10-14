package com.lto.api.controller;

import com.lto.api.entity.ExamSession;
import com.lto.api.entity.UserAccount;
import com.lto.api.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

// Admin-only: dashboard numbers and user management.
@RestController
@RequestMapping("/api/admin")
public class AdminUserController {

    public record MissedDto(int questionId, String questionText, long timesWrong, long timesAnswered) {}

    public record StatsDto(long totalUsers, long activeUsers, long activeQuestions, long totalQuestions,
                           long activeRoadSigns, long activeViolations, long examsTaken, long examsPassed,
                           double passRate, List<MissedDto> mostMissed) {}

    public record UserAdminDto(int userId, String fullName, String email, String targetLicense, boolean isActive,
                               int currentStreak, int longestStreak, long examsTaken, LocalDateTime createdAt) {}

    public record ActiveInput(Boolean isActive) {}

    private final UserAccountRepository users;
    private final QuestionRepository questions;
    private final RoadSignRepository roadSigns;
    private final ViolationRepository violations;
    private final ExamSessionRepository sessions;
    private final SessionItemLogRepository logs;

    public AdminUserController(UserAccountRepository users, QuestionRepository questions,
                               RoadSignRepository roadSigns, ViolationRepository violations,
                               ExamSessionRepository sessions, SessionItemLogRepository logs) {
        this.users = users;
        this.questions = questions;
        this.roadSigns = roadSigns;
        this.violations = violations;
        this.sessions = sessions;
        this.logs = logs;
    }

    @GetMapping("/stats")
    public StatsDto stats() {
        List<UserAccount> allUsers = users.findAll();
        List<ExamSession> allSessions = sessions.findAll();
        long passed = allSessions.stream().filter(s -> Boolean.TRUE.equals(s.getIsPassed())).count();

        List<MissedDto> missed = logs.findMostMissed().stream()
                .map(m -> new MissedDto(m.getQuestionId(), m.getQuestionText(),
                        m.getTimesWrong() == null ? 0 : m.getTimesWrong(),
                        m.getTimesAnswered() == null ? 0 : m.getTimesAnswered()))
                .toList();

        return new StatsDto(
                allUsers.size(),
                allUsers.stream().filter(u -> Boolean.TRUE.equals(u.getIsActive())).count(),
                questions.findAll().stream().filter(q -> Boolean.TRUE.equals(q.getIsActive())).count(),
                questions.count(),
                roadSigns.findAll().stream().filter(s -> Boolean.TRUE.equals(s.getIsActive())).count(),
                violations.findAll().stream().filter(v -> Boolean.TRUE.equals(v.getIsActive())).count(),
                allSessions.size(),
                passed,
                allSessions.isEmpty() ? 0 : Math.round(passed * 1000.0 / allSessions.size()) / 10.0,
                missed);
    }

    @GetMapping("/users")
    public List<UserAdminDto> listUsers() {
        return users.findAll().stream()
                .sorted(Comparator.comparing(UserAccount::getUserId))
                .map(this::toDto).toList();
    }

    // Deactivate (isActive=false) or reactivate (isActive=true) an account.
    // A deactivated user can no longer log in.
    @PutMapping("/users/{id}/active")
    public UserAdminDto setActive(@PathVariable int id, @RequestBody ActiveInput in) {
        if (in.isActive() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "isActive is required");
        }
        UserAccount u = users.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        u.setIsActive(in.isActive());
        return toDto(users.save(u));
    }

    private UserAdminDto toDto(UserAccount u) {
        return new UserAdminDto(u.getUserId(), u.getFullName(), u.getEmail(), u.getTargetLicense(),
                Boolean.TRUE.equals(u.getIsActive()), u.getCurrentStreak(), u.getLongestStreak(),
                sessions.countByUserId(u.getUserId()), u.getCreatedAt());
    }
}
