package com.lto.api.service;

import com.lto.api.entity.*;
import com.lto.api.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

// Saves an uploaded exam and updates the user's statistics and streak.
// The SERVER checks each answer against tbl_question_bank, so the score cannot be faked by the app.
@Service
public class ExamService {

    private static final Set<String> LICENSES = Set.of("NON_PROF", "PROF");
    private static final Set<String> VEHICLES = Set.of("MOTORCYCLE", "LIGHT", "HEAVY", "BUS");
    private static final double PASSING_RATE = 0.80; // 48 of 60

    public record ItemRequest(Integer questionId, Integer selectedChoice, Integer responseTimeSec) {}

    public record UploadRequest(String clientSessionId, String licenseCategory, String vehicleType,
                                Long startTimestamp, Long endTimestamp, Integer timeAllocated,
                                Boolean isCompleted, List<ItemRequest> items) {}

    public record UploadResult(ExamSession session, boolean duplicate) {}

    private final ExamSessionRepository sessions;
    private final SessionItemLogRepository logs;
    private final QuestionRepository questions;
    private final DomainPerformanceAnalyticsRepository analytics;
    private final UserAccountRepository users;

    public ExamService(ExamSessionRepository sessions, SessionItemLogRepository logs,
                       QuestionRepository questions, DomainPerformanceAnalyticsRepository analytics,
                       UserAccountRepository users) {
        this.sessions = sessions;
        this.logs = logs;
        this.questions = questions;
        this.analytics = analytics;
        this.users = users;
    }

    @Transactional
    public UploadResult upload(int userId, UploadRequest req) {
        if (req.clientSessionId() == null || req.clientSessionId().isBlank() || req.clientSessionId().length() > 36) {
            throw bad("clientSessionId is required (a UUID, max 36 characters)");
        }

        // Same exam sent twice (e.g. the app retried after losing connection): return the saved one.
        Optional<ExamSession> existing = sessions.findByClientSessionId(req.clientSessionId());
        if (existing.isPresent()) {
            if (!existing.get().getUserId().equals(userId)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "clientSessionId already used");
            }
            return new UploadResult(existing.get(), true);
        }

        if (!LICENSES.contains(req.licenseCategory())) throw bad("licenseCategory must be NON_PROF or PROF");
        if (!VEHICLES.contains(req.vehicleType())) throw bad("vehicleType must be MOTORCYCLE, LIGHT, HEAVY or BUS");
        if (req.startTimestamp() == null) throw bad("startTimestamp is required");
        if (req.items() == null || req.items().isEmpty()) throw bad("items must not be empty");
        if (req.items().size() > 120) throw bad("too many items");

        List<Integer> ids = req.items().stream().map(ItemRequest::questionId).distinct().toList();
        if (ids.contains(null)) throw bad("every item needs a questionId");
        Map<Integer, Question> questionMap = questions.findAllById(ids).stream()
                .collect(Collectors.toMap(Question::getQuestionId, Function.identity()));
        if (questionMap.size() != ids.size()) throw bad("items contain an unknown questionId");

        int total = req.items().size();
        int passing = (int) Math.ceil(total * PASSING_RATE);

        ExamSession session = new ExamSession();
        session.setClientSessionId(req.clientSessionId());
        session.setUserId(userId);
        session.setLicenseCategory(req.licenseCategory());
        session.setVehicleType(req.vehicleType());
        session.setStartTimestamp(req.startTimestamp());
        session.setEndTimestamp(req.endTimestamp());
        session.setTimeAllocated(req.timeAllocated() != null ? req.timeAllocated() : 3600);
        session.setTotalItems(total);
        session.setPassingScore(passing);
        session.setIsCompleted(req.isCompleted() == null || req.isCompleted());
        session = sessions.save(session);

        int correct = 0;
        List<SessionItemLog> itemLogs = new ArrayList<>();
        for (ItemRequest item : req.items()) {
            Integer choice = item.selectedChoice();
            if (choice != null && (choice < 1 || choice > 4)) throw bad("selectedChoice must be 1-4 or null");

            Question q = questionMap.get(item.questionId());
            Boolean isCorrect = (choice == null) ? null : choice.equals(q.getCorrectChoice());
            if (Boolean.TRUE.equals(isCorrect)) correct++;

            SessionItemLog log = new SessionItemLog();
            log.setSessionId(session.getSessionId());
            log.setQuestionId(item.questionId());
            log.setSelectedChoice(choice);
            log.setIsCorrect(isCorrect);
            log.setResponseTimeSec(item.responseTimeSec() != null ? Math.max(0, item.responseTimeSec()) : 0);
            itemLogs.add(log);
        }
        logs.saveAllAndFlush(itemLogs);

        session.setRawScore(correct);
        session.setIsPassed(correct >= passing);
        session = sessions.save(session);

        updateAnalytics(userId, itemLogs, questionMap);
        updateStreak(userId);

        return new UploadResult(session, false);
    }

    private void updateAnalytics(int userId, List<SessionItemLog> itemLogs, Map<Integer, Question> questionMap) {
        Map<Integer, List<SessionItemLog>> byDomain = itemLogs.stream()
                .collect(Collectors.groupingBy(l -> questionMap.get(l.getQuestionId()).getDomainId()));

        for (Map.Entry<Integer, List<SessionItemLog>> entry : byDomain.entrySet()) {
            int domainId = entry.getKey();
            DomainPerformanceAnalytics a = analytics.findByUserIdAndDomainId(userId, domainId).orElseGet(() -> {
                DomainPerformanceAnalytics n = new DomainPerformanceAnalytics();
                n.setUserId(userId);
                n.setDomainId(domainId);
                return n;
            });

            for (SessionItemLog l : entry.getValue()) {
                if (l.getIsCorrect() == null) {
                    a.setTotalSkipped(a.getTotalSkipped() + 1);
                } else {
                    a.setTotalAttempted(a.getTotalAttempted() + 1);
                    if (l.getIsCorrect()) a.setTotalCorrect(a.getTotalCorrect() + 1);
                    else a.setTotalIncorrect(a.getTotalIncorrect() + 1);
                }
            }

            a.setAccuracyRate(a.getTotalAttempted() == 0 ? BigDecimal.ZERO
                    : BigDecimal.valueOf(a.getTotalCorrect() * 100.0 / a.getTotalAttempted())
                            .setScale(2, RoundingMode.HALF_UP));
            a.setTotalMastered((int) logs.countMastered(userId, domainId));
            analytics.save(a);
        }
    }

    // Streak = consecutive days with at least one finished exam.
    private void updateStreak(int userId) {
        UserAccount user = users.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
        LocalDate today = LocalDate.now();
        LocalDate last = user.getLastActiveDate();

        int current;
        if (last == null || last.isBefore(today.minusDays(1))) current = 1;            // new or broken streak
        else if (last.equals(today.minusDays(1))) current = user.getCurrentStreak() + 1; // continued
        else current = Math.max(1, user.getCurrentStreak());                            // already counted today

        user.setCurrentStreak(current);
        user.setLongestStreak(Math.max(current, user.getLongestStreak()));
        user.setLastActiveDate(today);
        users.save(user);
    }

    private static ResponseStatusException bad(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
