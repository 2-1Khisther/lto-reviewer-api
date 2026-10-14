package com.lto.api.repository;

import com.lto.api.entity.ExamSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExamSessionRepository extends JpaRepository<ExamSession, Integer> {
    Optional<ExamSession> findByClientSessionId(String clientSessionId);
    List<ExamSession> findByUserIdOrderByStartTimestampDesc(Integer userId);
    long countByUserId(Integer userId);
    long countByUserIdAndIsPassedTrue(Integer userId);
}
