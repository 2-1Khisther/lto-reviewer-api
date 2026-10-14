package com.lto.api.repository;

import com.lto.api.entity.Violation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ViolationRepository extends JpaRepository<Violation, Integer> {
    java.util.List<Violation> findByUpdatedAtAfter(java.time.LocalDateTime since);
}
