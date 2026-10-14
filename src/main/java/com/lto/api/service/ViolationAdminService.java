package com.lto.api.service;

import com.lto.api.entity.Violation;
import com.lto.api.entity.ViolationFine;
import com.lto.api.repository.ViolationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// Creates and edits violations together with their fine tiers.
@Service
public class ViolationAdminService {

    public record FineInput(Integer offenseNumber, BigDecimal fineAmount, String additionalPenalty) {}

    private final ViolationRepository violations;

    public ViolationAdminService(ViolationRepository violations) {
        this.violations = violations;
    }

    @Transactional
    public Violation save(Violation violation, List<FineInput> fines) {
        validateFines(fines);

        // Remove the old tiers first (and flush), so re-adding "1st offense" doesn't clash
        // with the unique (violation_id, offense_number) key.
        if (violation.getViolationId() != null) {
            violation.getFines().clear();
            violation = violations.saveAndFlush(violation);
        }

        for (FineInput f : fines) {
            ViolationFine fine = new ViolationFine();
            fine.setOffenseNumber(f.offenseNumber());
            fine.setFineAmount(f.fineAmount());
            fine.setAdditionalPenalty(blankToNull(f.additionalPenalty()));
            violation.getFines().add(fine);
        }
        violation = violations.saveAndFlush(violation);
        violations.touch(violation.getViolationId());
        return violation;
    }

    private static void validateFines(List<FineInput> fines) {
        if (fines == null || fines.isEmpty()) {
            throw bad("Add at least one fine tier");
        }
        Set<Integer> seen = new HashSet<>();
        for (FineInput f : fines) {
            if (f.offenseNumber() == null || f.offenseNumber() < 1 || f.offenseNumber() > 4) {
                throw bad("Offense number must be 1 to 4 (4 = 4th and succeeding)");
            }
            if (!seen.add(f.offenseNumber())) {
                throw bad("Each offense number can only appear once");
            }
            if (f.fineAmount() == null || f.fineAmount().signum() < 0) {
                throw bad("Each fine tier needs an amount of 0 or more");
            }
        }
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private static ResponseStatusException bad(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
