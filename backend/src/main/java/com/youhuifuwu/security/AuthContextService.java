package com.youhuifuwu.security;

import com.youhuifuwu.common.exception.BusinessException;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import org.springframework.stereotype.Component;

@Component
public class AuthContextService {

    private final JwtTokenProvider jwtTokenProvider;

    public AuthContextService(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public CurrentUser requireCurrentUser(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new BusinessException(401, "Missing or invalid Authorization header");
        }
        String token = authHeader.substring(7);
        Claims claims = jwtTokenProvider.parseToken(token).getPayload();
        return CurrentUser.builder()
                .accountId(Long.valueOf(claims.getSubject()))
                .role((String) claims.get("role"))
                .displayName((String) claims.get("displayName"))
                .build();
    }

    public CurrentUser requireRole(HttpServletRequest request, String... roles) {
        CurrentUser currentUser = requireCurrentUser(request);
        boolean matched = Arrays.stream(roles).anyMatch(role -> role.equals(currentUser.getRole()));
        if (!matched) {
            throw new BusinessException(403, "No permission to access this resource");
        }
        return currentUser;
    }
}

