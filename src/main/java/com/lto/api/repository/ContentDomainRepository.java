package com.lto.api.repository;

import com.lto.api.entity.ContentDomain;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContentDomainRepository extends JpaRepository<ContentDomain, Integer> {
    
}
