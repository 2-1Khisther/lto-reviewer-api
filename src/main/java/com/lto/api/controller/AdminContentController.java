package com.lto.api.controller;

import com.lto.api.entity.Question;
import com.lto.api.entity.RoadSign;
import com.lto.api.entity.Violation;
import com.lto.api.repository.ContentDomainRepository;
import com.lto.api.repository.QuestionRepository;
import com.lto.api.repository.RoadSignRepository;
import com.lto.api.repository.ViolationRepository;
import com.lto.api.service.ViolationAdminService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

// Admin-only (AuthInterceptor requires an ADMIN token for /api/admin/**).
// Add / edit / hide questions, road signs and violations.
// DELETE never removes a row: it sets isActive = false, so exam history stays valid
// and the app hides the item on its next sync.
@RestController
@RequestMapping("/api/admin")
public class AdminContentController {

    private static final Set<String> LICENSES = Set.of("NON_PROF", "PROF");
    private static final Set<String> VEHICLES = Set.of("MOTORCYCLE", "LIGHT", "HEAVY", "BUS");
    private static final Set<String> SIGN_CATEGORIES =
            Set.of("REGULATORY", "WARNING", "GUIDE", "EXPRESSWAY", "INSTRUCTION", "HAZARD", "ROADWORK");

    public record QuestionInput(
            @NotNull Integer domainId,
            @NotBlank String licenseCategory,
            @NotBlank String vehicleType,
            @NotBlank String questionText,
            String graphicPath,
            @NotBlank String choiceA,
            @NotBlank String choiceB,
            @NotBlank String choiceC,
            @NotBlank String choiceD,
            @NotNull @Min(1) @Max(4) Integer correctChoice,
            String explanationText,
            String sourceReference,
            Boolean isActive) {}

    public record RoadSignInput(
            @NotBlank String signCategory,
            @NotBlank String signName,
            @NotBlank String description,
            String imagePath,
            Integer sortOrder,
            Boolean isActive) {}

    public record ViolationInput(
            @NotBlank String violationName,
            @NotBlank String description,
            String legalBasis,
            String imagePath,
            Integer sortOrder,
            Boolean isActive,
            List<ViolationAdminService.FineInput> fines) {}

    private final QuestionRepository questions;
    private final RoadSignRepository roadSigns;
    private final ViolationRepository violations;
    private final ContentDomainRepository domains;
    private final ViolationAdminService violationService;

    public AdminContentController(QuestionRepository questions, RoadSignRepository roadSigns,
                                  ViolationRepository violations, ContentDomainRepository domains,
                                  ViolationAdminService violationService) {
        this.questions = questions;
        this.roadSigns = roadSigns;
        this.violations = violations;
        this.domains = domains;
        this.violationService = violationService;
    }

    // ---------------- Questions ----------------

    @GetMapping("/questions")
    public List<ContentController.QuestionDto> listQuestions() {
        return questions.findAll().stream()
                .sorted(Comparator.comparing(Question::getQuestionId))
                .map(ContentController.QuestionDto::from).toList();
    }

    @PostMapping("/questions")
    @ResponseStatus(HttpStatus.CREATED)
    public ContentController.QuestionDto createQuestion(@Valid @RequestBody QuestionInput in) {
        Question q = new Question();
        apply(q, in);
        return ContentController.QuestionDto.from(questions.saveAndFlush(q));
    }

    @PutMapping("/questions/{id}")
    public ContentController.QuestionDto updateQuestion(@PathVariable int id, @Valid @RequestBody QuestionInput in) {
        Question q = questions.findById(id).orElseThrow(() -> notFound("Question"));
        apply(q, in);
        return ContentController.QuestionDto.from(questions.saveAndFlush(q));
    }

    @DeleteMapping("/questions/{id}")
    public ContentController.QuestionDto hideQuestion(@PathVariable int id) {
        Question q = questions.findById(id).orElseThrow(() -> notFound("Question"));
        q.setIsActive(false);
        return ContentController.QuestionDto.from(questions.saveAndFlush(q));
    }

    private void apply(Question q, QuestionInput in) {
        if (!domains.existsById(in.domainId())) throw bad("Unknown domain");
        if (!LICENSES.contains(in.licenseCategory())) throw bad("License must be NON_PROF or PROF");
        if (!VEHICLES.contains(in.vehicleType())) throw bad("Vehicle type must be MOTORCYCLE, LIGHT, HEAVY or BUS");

        q.setDomainId(in.domainId());
        q.setLicenseCategory(in.licenseCategory());
        q.setVehicleType(in.vehicleType());
        q.setQuestionText(in.questionText().trim());
        q.setGraphicPath(blankToNull(in.graphicPath()));
        q.setChoiceA(in.choiceA().trim());
        q.setChoiceB(in.choiceB().trim());
        q.setChoiceC(in.choiceC().trim());
        q.setChoiceD(in.choiceD().trim());
        q.setCorrectChoice(in.correctChoice());
        q.setExplanationText(in.explanationText() == null ? "" : in.explanationText().trim());
        q.setSourceReference(blankToNull(in.sourceReference()));
        q.setIsActive(in.isActive() == null || in.isActive());
    }

    // ---------------- Road signs ----------------

    @GetMapping("/road-signs")
    public List<ContentController.RoadSignDto> listRoadSigns() {
        return roadSigns.findAll().stream()
                .sorted(Comparator.comparing(RoadSign::getSortOrder).thenComparing(RoadSign::getSignId))
                .map(ContentController.RoadSignDto::from).toList();
    }

    @PostMapping("/road-signs")
    @ResponseStatus(HttpStatus.CREATED)
    public ContentController.RoadSignDto createRoadSign(@Valid @RequestBody RoadSignInput in) {
        RoadSign s = new RoadSign();
        apply(s, in);
        return ContentController.RoadSignDto.from(roadSigns.saveAndFlush(s));
    }

    @PutMapping("/road-signs/{id}")
    public ContentController.RoadSignDto updateRoadSign(@PathVariable int id, @Valid @RequestBody RoadSignInput in) {
        RoadSign s = roadSigns.findById(id).orElseThrow(() -> notFound("Road sign"));
        apply(s, in);
        return ContentController.RoadSignDto.from(roadSigns.saveAndFlush(s));
    }

    @DeleteMapping("/road-signs/{id}")
    public ContentController.RoadSignDto hideRoadSign(@PathVariable int id) {
        RoadSign s = roadSigns.findById(id).orElseThrow(() -> notFound("Road sign"));
        s.setIsActive(false);
        return ContentController.RoadSignDto.from(roadSigns.saveAndFlush(s));
    }

    private void apply(RoadSign s, RoadSignInput in) {
        if (!SIGN_CATEGORIES.contains(in.signCategory())) throw bad("Unknown sign category");
        s.setSignCategory(in.signCategory());
        s.setSignName(in.signName().trim());
        s.setDescription(in.description().trim());
        s.setImagePath(blankToNull(in.imagePath()));
        s.setSortOrder(in.sortOrder() == null ? 0 : in.sortOrder());
        s.setIsActive(in.isActive() == null || in.isActive());
    }

    // ---------------- Violations ----------------

    @GetMapping("/violations")
    public List<ContentController.ViolationDto> listViolations() {
        return violations.findAll().stream()
                .sorted(Comparator.comparing(Violation::getSortOrder).thenComparing(Violation::getViolationId))
                .map(ContentController.ViolationDto::from).toList();
    }

    @PostMapping("/violations")
    @ResponseStatus(HttpStatus.CREATED)
    public ContentController.ViolationDto createViolation(@Valid @RequestBody ViolationInput in) {
        Violation v = new Violation();
        apply(v, in);
        return ContentController.ViolationDto.from(violationService.save(v, in.fines()));
    }

    @PutMapping("/violations/{id}")
    public ContentController.ViolationDto updateViolation(@PathVariable int id, @Valid @RequestBody ViolationInput in) {
        Violation v = violations.findById(id).orElseThrow(() -> notFound("Violation"));
        apply(v, in);
        return ContentController.ViolationDto.from(violationService.save(v, in.fines()));
    }

    @DeleteMapping("/violations/{id}")
    public ContentController.ViolationDto hideViolation(@PathVariable int id) {
        Violation v = violations.findById(id).orElseThrow(() -> notFound("Violation"));
        v.setIsActive(false);
        return ContentController.ViolationDto.from(violations.saveAndFlush(v));
    }

    private void apply(Violation v, ViolationInput in) {
        v.setViolationName(in.violationName().trim());
        v.setDescription(in.description().trim());
        v.setLegalBasis(blankToNull(in.legalBasis()));
        v.setImagePath(blankToNull(in.imagePath()));
        v.setSortOrder(in.sortOrder() == null ? 0 : in.sortOrder());
        v.setIsActive(in.isActive() == null || in.isActive());
    }

    // ---------------- helpers ----------------

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private static ResponseStatusException bad(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private static ResponseStatusException notFound(String what) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, what + " not found");
    }
}
