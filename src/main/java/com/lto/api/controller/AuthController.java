package com.lto.api.controller;

import com.lto.api.entity.Admin;
import com.lto.api.entity.UserAccount;
import com.lto.api.repository.AdminRepository;
import com.lto.api.repository.UserAccountRepository;
import com.lto.api.security.JwtService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

// POST /api/auth/register, POST /api/auth/login, POST /api/admin/auth/login
@RestController
@RequestMapping("/api")
public class AuthController {

    public record RegisterRequest(
            @NotBlank String fullName,
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8, message = "must be at least 8 characters") String password,
            String targetLicense) {}

    public record LoginRequest(@NotBlank String email, @NotBlank String password) {}

    public record UserDto(int userId, String fullName, String email, String targetLicense,
                          String languagePref, boolean isOnboarded, int currentStreak, int longestStreak) {
        static UserDto from(UserAccount u) {
            return new UserDto(u.getUserId(), u.getFullName(), u.getEmail(), u.getTargetLicense(),
                    u.getLanguagePref(), Boolean.TRUE.equals(u.getIsOnboarded()),
                    u.getCurrentStreak(), u.getLongestStreak());
        }
    }

    public record AuthResponse(String token, UserDto user) {}

    public record AdminAuthResponse(String token, int adminId, String fullName, String email) {}

    private static final String WRONG_LOGIN = "Wrong email or password";

    private final UserAccountRepository users;
    private final AdminRepository admins;
    private final JwtService jwtService;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public AuthController(UserAccountRepository users, AdminRepository admins, JwtService jwtService) {
        this.users = users;
        this.admins = admins;
        this.jwtService = jwtService;
    }

    @PostMapping("/auth/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest req) {
        String email = req.email().trim().toLowerCase();
        if (users.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }

        UserAccount user = new UserAccount();
        user.setFullName(req.fullName().trim());
        user.setEmail(email);
        user.setPasswordHash(encoder.encode(req.password()));
        user.setTargetLicense("PROF".equals(req.targetLicense()) ? "PROF" : "NON_PROF");
        user = users.save(user);

        return new AuthResponse(jwtService.createToken(user.getUserId(), JwtService.ROLE_USER), UserDto.from(user));
    }

    @PostMapping("/auth/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req) {
        UserAccount user = users.findByEmail(req.email().trim().toLowerCase())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, WRONG_LOGIN));

        if (!encoder.matches(req.password(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, WRONG_LOGIN);
        }
        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This account has been deactivated");
        }

        return new AuthResponse(jwtService.createToken(user.getUserId(), JwtService.ROLE_USER), UserDto.from(user));
    }

    @PostMapping("/admin/auth/login")
    public AdminAuthResponse adminLogin(@Valid @RequestBody LoginRequest req) {
        Admin admin = admins.findByEmail(req.email().trim().toLowerCase())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, WRONG_LOGIN));

        if (!encoder.matches(req.password(), admin.getPasswordHash()) || !Boolean.TRUE.equals(admin.getIsActive())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, WRONG_LOGIN);
        }

        return new AdminAuthResponse(jwtService.createToken(admin.getAdminId(), JwtService.ROLE_ADMIN),
                admin.getAdminId(), admin.getFullName(), admin.getEmail());
    }
}
