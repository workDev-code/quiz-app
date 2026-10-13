package com.anhtduong.quiz.admin;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminQuizController {

    private final AdminQuizService service;

    public AdminQuizController(AdminQuizService service) { this.service = service; }

    @GetMapping("/stats")
    public AdminQuizDtos.TeacherStats stats() { return service.stats(); }

    @GetMapping("/quizzes")
    public List<AdminQuizDtos.AdminSummary> list() { return service.list(); }

    @PostMapping("/quizzes")
    public AdminQuizDtos.AdminFull create(@Valid @RequestBody AdminQuizDtos.QuizUpsertRequest req) {
        return service.create(req);
    }

    @GetMapping("/quizzes/{id}")
    public AdminQuizDtos.AdminFull get(@PathVariable Long id) { return service.get(id); }

    @PutMapping("/quizzes/{id}")
    public AdminQuizDtos.AdminFull update(@PathVariable Long id,
                                          @Valid @RequestBody AdminQuizDtos.QuizUpsertRequest req) {
        return service.update(id, req);
    }

    @PostMapping("/quizzes/{id}/publish")
    public AdminQuizDtos.AdminFull publish(@PathVariable Long id) { return service.publish(id); }

    @PostMapping("/quizzes/{id}/unpublish")
    public AdminQuizDtos.AdminFull unpublish(@PathVariable Long id) { return service.unpublish(id); }

    @PostMapping("/quizzes/{id}/questions")
    public AdminQuizDtos.AdminFull addQuestion(@PathVariable Long id,
                                               @Valid @RequestBody AdminQuizDtos.QuestionUpsertRequest req) {
        return service.addQuestion(id, req);
    }

    @PutMapping("/questions/{questionId}")
    public AdminQuizDtos.AdminFull updateQuestion(@PathVariable Long questionId,
                                                  @Valid @RequestBody AdminQuizDtos.QuestionUpsertRequest req) {
        return service.updateQuestion(questionId, req);
    }

    @DeleteMapping("/questions/{questionId}")
    public AdminQuizDtos.AdminFull deleteQuestion(@PathVariable Long questionId) {
        return service.deleteQuestion(questionId);
    }

    @GetMapping("/quizzes/{id}/attempts")
    public Map<String, Object> attempts(@PathVariable Long id) {
        var q = service.get(id);
        var items = service.attemptsForQuiz(id);
        return Map.of(
                "quiz", Map.of("id", q.id, "title", q.title, "passPct", q.passPct, "questionCount", q.questions.size()),
                "items", items
        );
    }
}
