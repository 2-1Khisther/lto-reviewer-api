package com.lto.api.config;

import com.lto.api.entity.Admin;
import com.lto.api.repository.AdminRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

// On startup: if there is no admin yet, create one from application.properties
// (app.admin.*), with the password stored as a BCrypt hash.
@Component
public class DefaultAdminCreator implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DefaultAdminCreator.class);

    private final AdminRepository admins;
    private final String fullName;
    private final String email;
    private final String password;

    public DefaultAdminCreator(AdminRepository admins,
                               @Value("${app.admin.full-name}") String fullName,
                               @Value("${app.admin.email}") String email,
                               @Value("${app.admin.password}") String password) {
        this.admins = admins;
        this.fullName = fullName;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(String... args) {
        if (admins.count() > 0) {
            return;
        }
        Admin admin = new Admin();
        admin.setFullName(fullName);
        admin.setEmail(email.trim().toLowerCase());
        admin.setPasswordHash(new BCryptPasswordEncoder().encode(password));
        admins.save(admin);
        log.info("Created first admin account: {}", admin.getEmail());
    }
}
