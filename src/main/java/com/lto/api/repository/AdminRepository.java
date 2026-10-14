package com.lto.api.repository;

import com.lto.api.entity.Admin;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminRepository extends JpaRepository<Admin, Integer> {
    java.util.Optional<Admin> findByEmail(String email);
}
