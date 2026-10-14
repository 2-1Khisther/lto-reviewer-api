package com.lto.api.repository;

import com.lto.api.entity.RoadSign;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoadSignRepository extends JpaRepository<RoadSign, Integer> {
    java.util.List<RoadSign> findByUpdatedAtAfter(java.time.LocalDateTime since);
}
