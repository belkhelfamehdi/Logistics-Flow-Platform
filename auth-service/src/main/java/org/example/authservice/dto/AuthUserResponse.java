package org.example.authservice.dto;

import java.util.List;

public record AuthUserResponse(
        String username,
        String displayName,
        List<String> roles
) {
}