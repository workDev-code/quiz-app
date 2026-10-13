package com.anhtduong.quiz.user;

import java.io.Serializable;
import java.security.Principal;

public record AuthPrincipal(Long id, String email, UserEntity.Role role) implements Principal, Serializable {
    @Override
    public String getName() { return email; }
}
