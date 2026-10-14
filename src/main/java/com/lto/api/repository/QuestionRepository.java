package com.lto.api.repository;

import com.lto.api.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionRepository extends JpaRepository<Question, Integer> {
    java.util.List<Question> findByUpdatedAtAfter(java.time.LocalDateTime since);
}
