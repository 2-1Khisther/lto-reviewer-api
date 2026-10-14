package com.lto.api.repository;

import com.lto.api.entity.DomainPerformanceAnalytics;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DomainPerformanceAnalyticsRepository extends JpaRepository<DomainPerformanceAnalytics, Integer> {
    Optional<DomainPerformanceAnalytics> findByUserIdAndDomainId(Integer userId, Integer domainId);
    List<DomainPerformanceAnalytics> findByUserId(Integer userId);
}
