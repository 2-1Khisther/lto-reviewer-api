package com.lto.api.repository;

import com.lto.api.entity.Violation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ViolationRepository extends JpaRepository<Violation, Integer> {

    List<Violation> findByUpdatedAtAfter(LocalDateTime since);

    // Fine tiers live in another table, so editing only the fines would not change
    // tbl_violations.updated_at. This marks the violation as changed so the app syncs it.
    @Modifying
    @Query(value = "UPDATE tbl_violations SET updated_at = NOW() WHERE violation_id = :id", nativeQuery = true)
    void touch(@Param("id") int id);
}
