package org.example.apigateway.security;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class GatewaySecurityFilter implements GlobalFilter, Ordered {

    private final WebClient webClient;

    public GatewaySecurityFilter(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    @Override
    public int getOrder() {
        return -100;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        HttpMethod method = request.getMethod();
        String path = request.getURI().getPath();

        if (HttpMethod.OPTIONS.equals(method) || isPublicPath(path)) {
            return chain.filter(exchange);
        }

        String cookieHeader = request.getHeaders().getFirst(HttpHeaders.COOKIE);
        if (cookieHeader == null || cookieHeader.isBlank()) {
            return unauthorized(exchange, "Missing session cookie");
        }

        if (requiresCsrf(method) && !isValidCsrf(request)) {
            return forbidden(exchange, "Invalid CSRF token");
        }

        return webClient.get()
                .uri("http://auth-service/auth/me")
                .header(HttpHeaders.COOKIE, cookieHeader)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> Mono.error(new IllegalStateException("UNAUTHORIZED")))
                .bodyToMono(AuthMeResponse.class)
                .flatMap(profile -> {
                    if (profile == null || profile.username() == null || profile.roles() == null || profile.roles().isEmpty()) {
                        return unauthorized(exchange, "Session is not authenticated");
                    }

                    Set<String> roles = new HashSet<>(profile.roles().stream().map(String::toUpperCase).toList());
                    if (!isRoleAllowed(path, method, roles)) {
                        return forbidden(exchange, "Role is not allowed for this action");
                    }

                    ServerHttpRequest mutatedRequest = request.mutate()
                            .header("X-Authenticated-User", profile.username())
                            .header("X-Authenticated-Roles", String.join(",", roles))
                            .build();

                    return chain.filter(exchange.mutate().request(mutatedRequest).build());
                })
                .onErrorResume(exception -> unauthorized(exchange, "Authentication failed"));
    }

    private static boolean isPublicPath(String path) {
        return path.startsWith("/auth/")
                || path.startsWith("/platform/")
                || path.startsWith("/actuator/")
                || path.startsWith("/error");
    }

    private static boolean requiresCsrf(HttpMethod method) {
        return method != null
                && method != HttpMethod.GET
                && method != HttpMethod.HEAD
                && method != HttpMethod.OPTIONS;
    }

    private static boolean isValidCsrf(ServerHttpRequest request) {
        String headerToken = request.getHeaders().getFirst("X-XSRF-TOKEN");
        if (headerToken == null || headerToken.isBlank()) {
            return false;
        }

        String cookieToken = request.getCookies().containsKey("XSRF-TOKEN")
                ? request.getCookies().getFirst("XSRF-TOKEN").getValue()
                : null;

        return cookieToken != null && cookieToken.equals(headerToken);
    }

    private static boolean isRoleAllowed(String path, HttpMethod method, Set<String> roles) {
        if (path.startsWith("/orders")) {
            return HttpMethod.POST.equals(method)
                    ? hasAnyRole(roles, "ADMIN", "OPS")
                    : hasAnyRole(roles, "ADMIN", "OPS", "DELIVERY");
        }

        if (path.startsWith("/inventory")) {
            return HttpMethod.GET.equals(method)
                    ? hasAnyRole(roles, "ADMIN", "OPS", "DELIVERY")
                    : hasAnyRole(roles, "ADMIN", "OPS");
        }

        if (path.startsWith("/shipments")) {
            return HttpMethod.PATCH.equals(method)
                    ? hasAnyRole(roles, "ADMIN", "OPS", "DELIVERY")
                    : hasAnyRole(roles, "ADMIN", "OPS", "DELIVERY");
        }

        return true;
    }

    private static boolean hasAnyRole(Set<String> roles, String... expectedRoles) {
        return Arrays.stream(expectedRoles).anyMatch(roles::contains);
    }

    private static Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().set("X-Auth-Error", message);
        return exchange.getResponse().setComplete();
    }

    private static Mono<Void> forbidden(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
        exchange.getResponse().getHeaders().set("X-Auth-Error", message);
        return exchange.getResponse().setComplete();
    }

    private record AuthMeResponse(String username, String displayName, List<String> roles) {
    }
}