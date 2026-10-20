package com.anhtduong.quiz.question;

import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class QuestionBankController {

    private final QuestionBankService service;

    public QuestionBankController(QuestionBankService service) {
        this.service = service;
    }

    /**
     * Tìm câu hỏi trong ngân hàng. Chỉ giáo viên.
     */
    @GetMapping("/questions")
    @PreAuthorize("hasRole('TEACHER')")
    public List<QuestionBankDtos.BankItem> searchBank(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) Long tagId) {
        return service.searchBank(search, difficulty, tagId);
    }

    /**
     * Liên kết 1 câu hỏi trong ngân hàng vào quiz cụ thể.
     */
    @PostMapping("/quizzes/{quizId}/questions/{questionId}")
    @PreAuthorize("hasRole('TEACHER')")
    public void linkToQuiz(@PathVariable Long quizId, @PathVariable Long questionId) {
        service.linkQuestionToQuiz(quizId, questionId);
    }

    @DeleteMapping("/quizzes/{quizId}/questions/{questionId}")
    @PreAuthorize("hasRole('TEACHER')")
    public void unlinkFromQuiz(@PathVariable Long quizId, @PathVariable Long questionId) {
        service.unlinkQuestionFromQuiz(quizId, questionId);
    }

    /* --------------- Tags --------------- */

    @GetMapping("/tags")
    @PreAuthorize("hasRole('TEACHER')")
    public List<QuestionBankDtos.TagDto> listTags() {
        return service.listTags();
    }

    @PostMapping("/tags")
    @PreAuthorize("hasRole('TEACHER')")
    public QuestionBankDtos.TagDto createTag(@Valid @RequestBody QuestionBankDtos.CreateTagRequest req) {
        return service.createTag(req.name());
    }

    @DeleteMapping("/tags/{tagId}")
    @PreAuthorize("hasRole('TEACHER')")
    public void deleteTag(@PathVariable Long tagId) {
        service.deleteTag(tagId);
    }
}
