package com.anhtduong.quiz.auth;
public record AuthDtos_AuthResponse(String token, long expiresInSeconds, AuthDtos_PublicUser user) {}
