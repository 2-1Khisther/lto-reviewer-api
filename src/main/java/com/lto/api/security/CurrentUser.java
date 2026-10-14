package com.lto.api.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

// Reads who is calling, from the values AuthInterceptor stored after checking the token.
public final class CurrentUser {

    private CurrentUser() {}

    // For app-user endpoints (exam upload, profile, settings): admins are refused.
    public static int requireUserId(HttpServletRequest request) {
        Object role = request.getAttribute(AuthInterceptor.ATTR_ROLE);
        Object id = request.getAttribute(AuthInterceptor.ATTR_ACCOUNT_ID);
        if (!JwtService.ROLE_USER.equals(role) || !(id instanceof Integer userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "A user login token is required");
        }
        return userId;
    }
}
