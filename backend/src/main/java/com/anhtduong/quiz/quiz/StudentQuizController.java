package com.anhtduong.quiz.quiz;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/quizzes")
public class StudentQuizController {

    private final StudentQuizService service;

    public StudentQuizController(StudentQuizService service) { this.service = service; }

    @GetMapping
    public List<StudentDtos.Summary> list() {
        return service.listPublished();
    }

    @GetMapping("/{id}")
    public StudentDtos.Detail detail(@PathVariable Long id) {
        return service.detail(id);
    }
}
