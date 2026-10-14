package com.lto.api.repository;

import com.lto.api.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccountRepository extends JpaRepository<UserAccount, Integer> {
    java.util.Optional<UserAccount> findByEmail(String email);
    boolean existsByEmail(String email);
}
