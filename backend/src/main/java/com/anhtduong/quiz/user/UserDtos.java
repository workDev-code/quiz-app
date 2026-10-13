package com.anhtduong.quiz.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UserDtos {

    public record PublicUser(Long id, String name, String email, String role) {
        public static PublicUser of(UserEntity u) {
            return new PublicUser(u.getId(), u.getName(), u.getEmail(), u.getRole().name());
        }
    }

    public record RegisterRequest(
            @NotBlank @Size(min = 2, max = 120) String name,
            @NotBlank @Email @Size(max = 180) String email,
            @NotBlank @Size(min = 8, max = 80) String password
    ) {}

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password
    ) {}

    public record AuthResponse(String token, long expiresInSeconds, PublicUser user) {}

    public record PasswordChangeRequest(
            @NotBlank String currentPassword,
            @NotBlank @Size(min = 8, max = 80) String newPassword
    ) {}
}
