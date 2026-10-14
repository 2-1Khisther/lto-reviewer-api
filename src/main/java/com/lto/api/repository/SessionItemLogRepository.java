package com.lto.api.repository;

import com.lto.api.entity.SessionItemLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SessionItemLogRepository extends JpaRepository<SessionItemLog, Integer> {

    List<SessionItemLog> findBySessionId(Integer sessionId);

    // "Mastered" = questions in this domain the user has answered correctly on EVERY attempt
    // (a wrong answer or a skip on any attempt means not mastered).
    @Query(value = """
            SELECT COUNT(*) FROM (
                SELECT l.question_id
                FROM tbl_session_item_logs l
                JOIN tbl_exam_sessions s ON s.session_id = l.session_id
                JOIN tbl_question_bank q ON q.question_id = l.question_id
                WHERE s.user_id = :userId AND q.domain_id = :domainId
                GROUP BY l.question_id
                HAVING SUM(CASE WHEN l.is_correct = 1 THEN 0 ELSE 1 END) = 0
            ) mastered
            """, nativeQuery = true)
    long countMastered(@Param("userId") int userId, @Param("domainId") int domainId);
}
