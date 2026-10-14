package com.lto.api.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

// Runs before every /api/** request (except health and login/register, see WebConfig).
// Checks the "Authorization: Bearer <token>" header and blocks requests without a valid token.
// /api/admin/** additionally needs an ADMIN token.
@Component
public class AuthInterceptor implements HandlerInterceptor {

    public static final String ATTR_ACCOUNT_ID = "authAccountId";
    public static final String ATTR_ROLE = "authRole";

    private final JwtService jwtService;

    public AuthInterceptor(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true; // browser CORS pre-check for the admin web panel
        }

        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing login token");
            return false;
        }

        try {
            Claims claims = jwtService.parse(header.substring(7));
            String role = claims.get("role", String.class);

            if (request.getRequestURI().startsWith("/api/admin/") && !JwtService.ROLE_ADMIN.equals(role)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Admin only");
                return false;
            }

            request.setAttribute(ATTR_ACCOUNT_ID, Integer.valueOf(claims.getSubject()));
            request.setAttribute(ATTR_ROLE, role);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired login token");
            return false;
        }
    }
}
