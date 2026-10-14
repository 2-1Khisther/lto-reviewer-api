package com.lto.api.controller;

import com.lto.api.entity.ContentDomain;
import com.lto.api.entity.Question;
import com.lto.api.entity.RoadSign;
import com.lto.api.entity.Violation;
import com.lto.api.repository.ContentDomainRepository;
import com.lto.api.repository.QuestionRepository;
import com.lto.api.repository.RoadSignRepository;
import com.lto.api.repository.ViolationRepository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

// Content the Android app downloads into Room. Needs a login token.
// Optional ?since=2026-10-14T12:00:00 returns only rows changed after that time.
// Inactive rows are included (isActive=false) so the app knows to hide them.
// Save "serverTime" from each response and send it as "since" next time.
@RestController
@RequestMapping("/api/content")
public class ContentController {

    public record SyncResponse<T>(LocalDateTime serverTime, List<T> items) {}

    public record DomainDto(int domainId, String domainCode, String domainName, String description, boolean isActive) {
        static DomainDto from(ContentDomain d) {
            return new DomainDto(d.getDomainId(), d.getDomainCode(), d.getDomainName(), d.getDescription(),
                    Boolean.TRUE.equals(d.getIsActive()));
        }
    }

    public record QuestionDto(int questionId, int domainId, String licenseCategory, String vehicleType,
                              String questionText, String graphicPath, String choiceA, String choiceB,
                              String choiceC, String choiceD, int correctChoice, String explanationText,
                              String sourceReference, boolean isActive, LocalDateTime updatedAt) {
        static QuestionDto from(Question q) {
            return new QuestionDto(q.getQuestionId(), q.getDomainId(), q.getLicenseCategory(), q.getVehicleType(),
                    q.getQuestionText(), q.getGraphicPath(), q.getChoiceA(), q.getChoiceB(), q.getChoiceC(),
                    q.getChoiceD(), q.getCorrectChoice(), q.getExplanationText(), q.getSourceReference(),
                    Boolean.TRUE.equals(q.getIsActive()), q.getUpdatedAt());
        }
    }

    public record RoadSignDto(int signId, String signCategory, String signName, String description,
                              String imagePath, int sortOrder, boolean isActive, LocalDateTime updatedAt) {
        static RoadSignDto from(RoadSign s) {
            return new RoadSignDto(s.getSignId(), s.getSignCategory(), s.getSignName(), s.getDescription(),
                    s.getImagePath(), s.getSortOrder(), Boolean.TRUE.equals(s.getIsActive()), s.getUpdatedAt());
        }
    }

    public record FineDto(int offenseNumber, BigDecimal fineAmount, String additionalPenalty) {}

    public record ViolationDto(int violationId, String violationName, String description, String legalBasis,
                               String imagePath, int sortOrder, boolean isActive, LocalDateTime updatedAt,
                               List<FineDto> fines) {
        static ViolationDto from(Violation v) {
            List<FineDto> fines = v.getFines().stream()
                    .map(f -> new FineDto(f.getOffenseNumber(), f.getFineAmount(), f.getAdditionalPenalty()))
                    .toList();
            return new ViolationDto(v.getViolationId(), v.getViolationName(), v.getDescription(),
                    v.getLegalBasis(), v.getImagePath(), v.getSortOrder(), Boolean.TRUE.equals(v.getIsActive()),
                    v.getUpdatedAt(), fines);
        }
    }

    private final ContentDomainRepository domains;
    private final QuestionRepository questions;
    private final RoadSignRepository roadSigns;
    private final ViolationRepository violations;

    public ContentController(ContentDomainRepository domains, QuestionRepository questions,
                             RoadSignRepository roadSigns, ViolationRepository violations) {
        this.domains = domains;
        this.questions = questions;
        this.roadSigns = roadSigns;
        this.violations = violations;
    }

    @GetMapping("/domains")
    public List<DomainDto> domains() {
        return domains.findAll().stream().map(DomainDto::from).toList();
    }

    @GetMapping("/questions")
    public SyncResponse<QuestionDto> questions(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since) {
        LocalDateTime now = LocalDateTime.now(); // taken BEFORE the query so no change is missed
        List<Question> rows = (since == null) ? questions.findAll() : questions.findByUpdatedAtAfter(since);
        return new SyncResponse<>(now, rows.stream().map(QuestionDto::from).toList());
    }

    @GetMapping("/road-signs")
    public SyncResponse<RoadSignDto> roadSigns(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since) {
        LocalDateTime now = LocalDateTime.now();
        List<RoadSign> rows = (since == null) ? roadSigns.findAll() : roadSigns.findByUpdatedAtAfter(since);
        return new SyncResponse<>(now, rows.stream().map(RoadSignDto::from).toList());
    }

    @GetMapping("/violations")
    public SyncResponse<ViolationDto> violations(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since) {
        LocalDateTime now = LocalDateTime.now();
        List<Violation> rows = (since == null) ? violations.findAll() : violations.findByUpdatedAtAfter(since);
        return new SyncResponse<>(now, rows.stream().map(ViolationDto::from).toList());
    }
}
