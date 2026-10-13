package com.anhtduong.quiz.auth;

import com.anhtduong.quiz.common.BusinessException;
import com.anhtduong.quiz.security.JwtService;
import com.anhtduong.quiz.user.UserDtos;
import com.anhtduong.quiz.user.UserEntity;
import com.anhtduong.quiz.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Pattern;

@Service
public class AuthService {
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthService(UserRepository users, PasswordEncoder encoder, JwtService jwt) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    @Transactional
    public UserDtos.AuthResponse register(UserDtos.RegisterRequest req) {
        String name = req.name() == null ? "" : req.name().trim();
        String email = req.email() == null ? "" : req.email().trim().toLowerCase();
        String password = req.password() == null ? "" : req.password();

        java.util.Map<String, String> errs = new java.util.LinkedHashMap<>();
        if (name.length() < 2) errs.put("name", "Họ tên cần ít nhất 2 ký tự.");
        if (!EMAIL.matcher(email).matches()) errs.put("email", "Nhập email hợp lệ.");
        if (password.length() < 8 || !password.matches(".*[A-Za-z].*") || !password.matches(".*\\d.*")) {
            errs.put("password", "Mật khẩu cần ít nhất 8 ký tự, gồm cả chữ và số.");
        }
        if (!errs.isEmpty()) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "VALIDATION_FAILED", "Dữ liệu chưa hợp lệ.", errs);
        }
        if (users.existsByEmail(email)) {
            throw new BusinessException(HttpStatus.CONFLICT, "EMAIL_EXISTS", "Email này đã được đăng ký.",
                    java.util.Map.of("email", "Email này đã được đăng ký."));
        }
        UserEntity u = new UserEntity();
        u.setName(name);
        u.setEmail(email);
        u.setRole(UserEntity.Role.STUDENT);
        u.setPasswordHash(encoder.encode(password));
        u = users.save(u);

        return new UserDtos.AuthResponse(jwt.issue(u.getId(), u.getEmail(), u.getRole()),
                jwt.expirySeconds(), UserDtos.PublicUser.of(u));
    }

    @Transactional(readOnly = true)
    public UserDtos.AuthResponse login(UserDtos.LoginRequest req) {
        String email = req.email() == null ? "" : req.email().trim().toLowerCase();
        if (!EMAIL.matcher(email).matches()) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "VALIDATION_FAILED",
                    "Dữ liệu chưa hợp lệ.",
                    java.util.Map.of("email", "Nhập email hợp lệ."));
        }
        UserEntity u = users.findByEmail(email).orElse(null);
        if (u == null || u.isDisabled() || !encoder.matches(req.password(), u.getPasswordHash())) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "BAD_CREDENTIALS",
                    "Email hoặc mật khẩu không đúng.");
        }
        return new UserDtos.AuthResponse(jwt.issue(u.getId(), u.getEmail(), u.getRole()),
                jwt.expirySeconds(), UserDtos.PublicUser.of(u));
    }
}
