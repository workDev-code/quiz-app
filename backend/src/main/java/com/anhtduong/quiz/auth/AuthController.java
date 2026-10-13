package com.anhtduong.quiz.auth;

import com.anhtduong.quiz.user.UserDtos;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService auth;

    public AuthController(AuthService auth) { this.auth = auth; }

    @PostMapping("/register")
    public ResponseEntity<UserDtos.AuthResponse> register(@Valid @RequestBody UserDtos.RegisterRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(auth.register(req));
    }

    @PostMapping("/login")
    public ResponseEntity<UserDtos.AuthResponse> login(@Valid @RequestBody UserDtos.LoginRequest req) {
        return ResponseEntity.ok(auth.login(req));
    }
}
