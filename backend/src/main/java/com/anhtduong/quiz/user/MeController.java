package com.anhtduong.quiz.user;

import com.anhtduong.quiz.common.BusinessException;
import com.anhtduong.quiz.user.UserDtos.PublicUser;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class MeController {

    private final UserRepository users;

    public MeController(UserRepository users) { this.users = users; }

    @GetMapping("/me")
    public PublicUser me() {
        Long id = CurrentUser.id();
        if (id == null) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "Bạn cần đăng nhập để tiếp tục.");
        }
        UserEntity u = users.findById(id).orElseThrow(() ->
                new BusinessException(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "Phiên không hợp lệ."));
        return PublicUser.of(u);
    }
}
