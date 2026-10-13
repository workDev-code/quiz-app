package com.anhtduong.quiz.attempt;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
public class StudentAttemptController {

    private final AttemptService service;

    public StudentAttemptController(AttemptService service) { this.service = service; }

    @PostMapping("/api/quizzes/{quizId}/attempts")
    public AttemptDtos.Brief submit(@PathVariable Long quizId, @Valid @RequestBody AttemptDtos.SubmitRequest req) {
        return service.submit(quizId, req);
    }

    @GetMapping("/api/attempts/me")
    public AttemptDtos.HistoryPage history(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long quizId) {
        return service.history(page, size, quizId);
    }

    @GetMapping("/api/attempts/{id}")
    public AttemptDtos.Detail detail(@PathVariable Long id) {
        return service.get(id);
    }
}
