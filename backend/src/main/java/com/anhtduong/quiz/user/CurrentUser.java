package com.anhtduong.quiz.user;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Helper static lấy user hiện tại từ SecurityContext.
 * Service / Controller chỉ cần {@code CurrentUser.id()} hay {@code CurrentUser.role()}.
 */
public final class CurrentUser {
    private CurrentUser() {}

    public static Long id() {
        Object p = principal();
        return (p instanceof AuthPrincipal ap) ? ap.id() : null;
    }

    public static String email() {
        Object p = principal();
        return (p instanceof AuthPrincipal ap) ? ap.email() : null;
    }

    public static UserEntity.Role role() {
        Object p = principal();
        return (p instanceof AuthPrincipal ap) ? ap.role() : null;
    }

    public static boolean isTeacher() {
        return role() == UserEntity.Role.TEACHER;
    }

    public static boolean isStudent() {
        return role() == UserEntity.Role.STUDENT;
    }

    private static Object principal() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return a == null ? null : a.getPrincipal();
    }
}
