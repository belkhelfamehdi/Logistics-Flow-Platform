package org.example.orderservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    public static final String ATTR_USER = "auth.user";
    public static final String ATTR_ROLES = "auth.roles";

    private final SecretKey verifyingKey;

    public JwtAuthFilter(@Value("${security.jwt.secret}") String secret) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("security.jwt.secret must be at least 32 bytes for HS256");
        }
        this.verifyingKey = Keys.hmacShaKeyFor(bytes);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator/") || path.equals("/error");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            reject(response, "Missing bearer token");
            return;
        }

        String token = authorization.substring("Bearer ".length()).trim();
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(verifyingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String username = claims.getSubject();
            Object rolesClaim = claims.get("roles");
            Set<String> roles = rolesClaim instanceof List<?> list
                    ? list.stream().map(Object::toString).map(String::toUpperCase).collect(java.util.stream.Collectors.toUnmodifiableSet())
                    : Set.of();

            request.setAttribute(ATTR_USER, username);
            request.setAttribute(ATTR_ROLES, roles);
            chain.doFilter(request, response);
        } catch (JwtException exception) {
            reject(response, "Invalid token: " + exception.getMessage());
        }
    }

    private static void reject(HttpServletResponse response, String reason) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setHeader("X-Auth-Error", reason);
        response.getWriter().flush();
    }
}
