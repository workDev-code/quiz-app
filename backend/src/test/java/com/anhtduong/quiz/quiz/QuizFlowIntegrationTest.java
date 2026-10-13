package com.anhtduong.quiz.quiz;

import com.anhtduong.quiz.attempt.AttemptDtos_Submit;
import com.anhtduong.quiz.attempt.AttemptDtos_Brief;
import com.anhtduong.quiz.test.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

class QuizFlowIntegrationTest extends AbstractIntegrationTest {

    record QuizSummary(Long id, String title, String status, int questionCount, int attemptsUsed, int maxAttempts, boolean available) {}
    record QuizDetail(QuizSummary summary, String unavailableReason, boolean canStart, List<PublicQuestion> questions) {}
    record PublicQuestion(Long id, String text, List<PublicOption> options) {}
    record PublicOption(Long id, String text) {}

    @Test
    void studentCannotSeeCorrectOptionOrExplanationInQuizJson() {
        var token = login("hocsinh@demo.vn", "Demo@1234");
        var detail = (QuizDetail) exchange("GET", "/api/quizzes/1", null, QuizDetail.class, token);
        assertThat(detail.canStart).isTrue();
        assertThat(detail.questions).isNotEmpty();
        // raw JSON must not contain "correct" or "explanation"
        var raw = exchange("GET", "/api/quizzes/1", null, String.class, token);
        assertThat(raw).doesNotContain("\"correct\"");
        assertThat(raw).doesNotContain("\"explanation\"");
    }

    @Test
    void draftQuizReturns404ToStudent() {
        var token = login("hocsinh@demo.vn", "Demo@1234");
        try {
            exchange("GET", "/api/quizzes/4", null, String.class, token);
            fail("Phải trả 404");
        } catch (org.springframework.web.client.HttpClientErrorException ex) {
            assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Test
    void studentCannotAccessAdminEndpoint() {
        var token = login("hocsinh@demo.vn", "Demo@1234");
        try {
            exchange("GET", "/api/admin/quizzes", null, String.class, token);
            fail("Phải trả 403");
        } catch (org.springframework.web.client.HttpClientErrorException ex) {
            assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }
    }

    @Test
    void allCorrectAttemptScoresFull() {
        var token = login("hocsinh@demo.vn", "Demo@1234");
        var detail = (QuizDetail) exchange("GET", "/api/quizzes/1", null, QuizDetail.class, token);
        // Chọn option thứ 2 của mỗi câu (theo seed) — 5/6 đúng
        List<Map<String, Object>> answers = detail.questions.stream()
                .map(q -> Map.<String, Object>of("questionId", q.id, "optionId", q.options.get(1).id()))
                .toList();
        var body = new AttemptDtos_Submit("ck-test-" + System.nanoTime(), java.time.Instant.now().toString(), answers);
        AttemptDtos_Brief r = (AttemptDtos_Brief) exchange("POST", "/api/quizzes/1/attempts", body, AttemptDtos_Brief.class, token);
        assertThat(r.score()).isEqualTo(5);
        assertThat(r.total()).isEqualTo(6);
        assertThat(r.percent()).isEqualTo(83);
        assertThat(r.passed()).isTrue();
    }

    @Test
    void duplicateClientKeyReturnsSameAttempt() {
        var token = login("hocsinh@demo.vn", "Demo@1234");
        var detail = (QuizDetail) exchange("GET", "/api/quizzes/1", null, QuizDetail.class, token);
        List<Map<String, Object>> answers = detail.questions.stream()
                .map(q -> Map.<String, Object>of("questionId", q.id, "optionId", q.options.get(1).id()))
                .toList();
        var ck = "ck-dup-" + System.nanoTime();
        var body = new AttemptDtos_Submit(ck, java.time.Instant.now().toString(), answers);
        AttemptDtos_Brief first = (AttemptDtos_Brief) exchange("POST", "/api/quizzes/1/attempts", body, AttemptDtos_Brief.class, token);
        AttemptDtos_Brief second = (AttemptDtos_Brief) exchange("POST", "/api/quizzes/1/attempts", body, AttemptDtos_Brief.class, token);
        assertThat(second.id()).isEqualTo(first.id());
        assertThat(second.seq()).isEqualTo(first.seq());
    }

    @Test
    void invalidOptionFromAnotherQuestionReturns422() {
        var token = login("hocsinh@demo.vn", "Demo@1234");
        var detail = (QuizDetail) exchange("GET", "/api/quizzes/1", null, QuizDetail.class, token);
        // lấy option thuộc câu 2, gửi cho câu 1
        var wrongOpt = detail.questions.get(1).options.get(0).id();
        var bad = List.of(Map.<String, Object>of("questionId", detail.questions.get(0).id(), "optionId", wrongOpt));
        var body = new AttemptDtos_Submit("ck-bad-" + System.nanoTime(), java.time.Instant.now().toString(), bad);
        try {
            exchange("POST", "/api/quizzes/1/attempts", body, String.class, token);
            fail("Phải trả 422");
        } catch (org.springframework.web.client.HttpClientErrorException ex) {
            assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
            assertThat(ex.getResponseBodyAsString()).contains("OPTION_NOT_IN_QUESTION");
        }
    }
}
