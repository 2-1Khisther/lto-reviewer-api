package com.lto.api.controller;

import com.lto.api.repository.ContentDomainRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

// GET /api/health -> checks the API is running AND can read the database.
@RestController
@RequestMapping("/api")
public class HealthController {

    private final ContentDomainRepository domains;

    public HealthController(ContentDomainRepository domains) {
        this.domains = domains;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
                "status", "UP",
                "contentDomains", domains.count() // should be 4 after setup_all.sql
        );
    }
}
