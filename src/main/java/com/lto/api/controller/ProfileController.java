package com.lto.api.controller;

import com.lto.api.entity.AppSettings;
import com.lto.api.entity.DomainPerformanceAnalytics;
import com.lto.api.entity.UserAccount;
import com.lto.api.repository.AppSettingsRepository;
import com.lto.api.repository.DomainPerformanceAnalyticsRepository;
import com.lto.api.repository.ExamSessionRepository;
import com.lto.api.repository.UserAccountRepository;
import com.lto.api.security.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

// GET/PUT /api/profile   -> Profile screen (user info + the 4 stats + streak)
// GET/PUT /api/settings  -> Settings screen toggles and custom exam length
@RestController
@RequestMapping("/api")
public class ProfileController {

    public record ProfileStats(int questionsAnswered, int masteredAnswers, int correctAnswers,
                               int incorrectAnswers, int skipped, long examsTaken, long examsPassed) {}

    public record ProfileDto(int userId, String fullName, String email, String targetLicense, String languagePref,
                             boolean isOnboarded, int currentStreak, int longestStreak, ProfileStats stats) {}

    public record ProfileUpdate(@Size(min = 1, max = 100) String fullName, String targetLicense,
                                String languagePref, Boolean isOnboarded) {}

    public record SettingsDto(int customExamLength, boolean notificationsEnabled, boolean soundEffects,
                              boolean vibrationEnabled, boolean darkMode, boolean offlineModeReady) {
        static SettingsDto from(AppSettings s) {
            return new SettingsDto(s.getCustomExamLength(), Boolean.TRUE.equals(s.getNotificationsEnabled()),
                    Boolean.TRUE.equals(s.getSoundEffects()), Boolean.TRUE.equals(s.getVibrationEnabled()),
                    Boolean.TRUE.equals(s.getDarkMode()), Boolean.TRUE.equals(s.getOfflineModeReady()));
        }
    }

    // Every field is optional: only the ones sent are changed.
    public record SettingsUpdate(@Min(5) @Max(100) Integer customExamLength, Boolean notificationsEnabled,
                                 Boolean soundEffects, Boolean vibrationEnabled, Boolean darkMode,
                                 Boolean offlineModeReady) {}

    private final UserAccountRepository users;
    private final DomainPerformanceAnalyticsRepository analytics;
    private final ExamSessionRepository sessions;
    private final AppSettingsRepository settingsRepo;

    public ProfileController(UserAccountRepository users, DomainPerformanceAnalyticsRepository analytics,
                             ExamSessionRepository sessions, AppSettingsRepository settingsRepo) {
        this.users = users;
        this.analytics = analytics;
        this.sessions = sessions;
        this.settingsRepo = settingsRepo;
    }

    @GetMapping("/profile")
    public ProfileDto getProfile(HttpServletRequest request) {
        int userId = CurrentUser.requireUserId(request);
        return toDto(loadUser(userId));
    }

    @PutMapping("/profile")
    public ProfileDto updateProfile(@Valid @RequestBody ProfileUpdate body, HttpServletRequest request) {
        int userId = CurrentUser.requireUserId(request);
        UserAccount user = loadUser(userId);

        if (body.fullName() != null) user.setFullName(body.fullName().trim());
        if (body.targetLicense() != null) {
            if (!List.of("NON_PROF", "PROF").contains(body.targetLicense())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "targetLicense must be NON_PROF or PROF");
            }
            user.setTargetLicense(body.targetLicense());
        }
        if (body.languagePref() != null) {
            if (!List.of("ENGLISH", "TAGALOG").contains(body.languagePref())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "languagePref must be ENGLISH or TAGALOG");
            }
            user.setLanguagePref(body.languagePref());
        }
        if (body.isOnboarded() != null) user.setIsOnboarded(body.isOnboarded());

        return toDto(users.save(user));
    }

    @GetMapping("/settings")
    public SettingsDto getSettings(HttpServletRequest request) {
        int userId = CurrentUser.requireUserId(request);
        return SettingsDto.from(loadOrCreateSettings(userId));
    }

    @PutMapping("/settings")
    public SettingsDto updateSettings(@Valid @RequestBody SettingsUpdate body, HttpServletRequest request) {
        int userId = CurrentUser.requireUserId(request);
        AppSettings s = loadOrCreateSettings(userId);

        if (body.customExamLength() != null) s.setCustomExamLength(body.customExamLength());
        if (body.notificationsEnabled() != null) s.setNotificationsEnabled(body.notificationsEnabled());
        if (body.soundEffects() != null) s.setSoundEffects(body.soundEffects());
        if (body.vibrationEnabled() != null) s.setVibrationEnabled(body.vibrationEnabled());
        if (body.darkMode() != null) s.setDarkMode(body.darkMode());
        if (body.offlineModeReady() != null) s.setOfflineModeReady(body.offlineModeReady());

        return SettingsDto.from(settingsRepo.save(s));
    }

    private UserAccount loadUser(int userId) {
        return users.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    private AppSettings loadOrCreateSettings(int userId) {
        return settingsRepo.findByUserId(userId).orElseGet(() -> {
            AppSettings s = new AppSettings();
            s.setUserId(userId);
            return settingsRepo.save(s);
        });
    }

    private ProfileDto toDto(UserAccount u) {
        List<DomainPerformanceAnalytics> rows = analytics.findByUserId(u.getUserId());
        ProfileStats stats = new ProfileStats(
                rows.stream().mapToInt(DomainPerformanceAnalytics::getTotalAttempted).sum(),
                rows.stream().mapToInt(DomainPerformanceAnalytics::getTotalMastered).sum(),
                rows.stream().mapToInt(DomainPerformanceAnalytics::getTotalCorrect).sum(),
                rows.stream().mapToInt(DomainPerformanceAnalytics::getTotalIncorrect).sum(),
                rows.stream().mapToInt(DomainPerformanceAnalytics::getTotalSkipped).sum(),
                sessions.countByUserId(u.getUserId()),
                sessions.countByUserIdAndIsPassedTrue(u.getUserId()));

        // A streak only counts if the last exam was today or yesterday.
        LocalDate last = u.getLastActiveDate();
        boolean alive = last != null && !last.isBefore(LocalDate.now().minusDays(1));

        return new ProfileDto(u.getUserId(), u.getFullName(), u.getEmail(), u.getTargetLicense(),
                u.getLanguagePref(), Boolean.TRUE.equals(u.getIsOnboarded()),
                alive ? u.getCurrentStreak() : 0, u.getLongestStreak(), stats);
    }
}
