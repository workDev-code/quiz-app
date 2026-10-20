package com.anhtduong.quiz.question;

import com.anhtduong.quiz.question.QuestionBankDtos.BankItem;
import com.anhtduong.quiz.question.QuestionBankDtos.TagDto;
import com.anhtduong.quiz.test.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Ngân hàng câu hỏi: tìm kiếm / tag / mượn câu hỏi vào đề.
 */
class QuestionBankIntegrationTest extends AbstractIntegrationTest {

    private static final ParameterizedTypeReference<List<BankItem>> BANK_LIST = new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<List<TagDto>> TAG_LIST = new ParameterizedTypeReference<>() {};

    private String teacher() { return login("giaovien@demo.vn", "Demo@1234"); }

    /** Gọi API và tự decode JSON vào đúng kiểu (RestTemplate cần ParameterizedTypeReference cho List). */
    private <T> T req(String method, String path, Object body, ParameterizedTypeReference<T> type, String token) {
        var entity = new HttpEntity<>(body, authHeaders(token));
        return http.exchange(baseUrl() + path, HttpMethod.valueOf(method), entity, type).getBody();
    }

    /**
     * Gọi API có query string. Phải truyền {@link URI} chứ không phải String, vì
     * RestTemplate coi String là URI template và sẽ encode lại lần nữa (tiếng Việt bị hỏng).
     */
    private List<BankItem> bank(String token, String search, String difficulty, Long tagId) {
        var b = UriComponentsBuilder.fromUriString(baseUrl()).path("/api/admin/questions");
        if (search != null) b.queryParam("search", search);
        if (difficulty != null) b.queryParam("difficulty", difficulty);
        if (tagId != null) b.queryParam("tagId", tagId);
        URI uri = b.build().encode().toUri();
        var entity = new HttpEntity<>(authHeaders(token));
        return http.exchange(uri, HttpMethod.GET, entity, BANK_LIST).getBody();
    }

    private List<BankItem> bank(String token) { return bank(token, null, null, null); }

    private List<TagDto> allTags(String token) {
        return req("GET", "/api/admin/tags", null, TAG_LIST, token);
    }

    private TagDto createTag(String token, String name) {
        return req("POST", "/api/admin/tags", Map.of("name", name), new ParameterizedTypeReference<TagDto>() {}, token);
    }

    // ---------- tìm kiếm ----------

    @Test
    void searchBankWithoutFilterReturnsSeededQuestions() {
        var items = bank(teacher());
        assertThat(items).isNotEmpty();
        // mỗi câu phải có đủ options (không lỗi N+1 / lazy loading)
        assertThat(items).allSatisfy(it -> {
            assertThat(it.id()).isNotNull();
            assertThat(it.text()).isNotBlank();
            assertThat(it.difficulty()).isIn("EASY", "MEDIUM", "HARD");
            assertThat(it.options()).hasSizeGreaterThanOrEqualTo(2);
            assertThat(it.options()).anyMatch(o -> o.correct());
        });
    }

    @Test
    void searchBankByTextIsCaseInsensitive() {
        var items = bank(teacher(), "kiểu dữ liệu", null, null);
        assertThat(items).isNotEmpty();
        assertThat(items).allSatisfy(it ->
                assertThat(it.text().toLowerCase()).contains("kiểu dữ liệu"));
    }

    @Test
    void searchBankFiltersByDifficulty() {
        var items = bank(teacher(), null, "EASY", null);
        assertThat(items).isNotEmpty();
        assertThat(items).allSatisfy(it -> assertThat(it.difficulty()).isEqualTo("EASY"));
    }

    @Test
    void searchBankWithNoMatchReturnsEmpty() {
        assertThat(bank(teacher(), "khongcoutenhaycauhoi", null, null)).isEmpty();
    }

    @Test
    void unknownDifficultyReturnsEmptyNotError() {
        assertThat(bank(teacher(), null, "SAO_LA", null)).isEmpty();
    }

    @Test
    void bankFlagsQuestionsAlreadyUsedInPublishedQuiz() {
        // quiz id 1 trong seed là PUBLISHED -> câu của nó phải bị đánh dấu
        var publishedQids = jdbc.queryForList(
                "SELECT qq.question_id FROM quiz_questions qq "
              + "JOIN quizzes qz ON qz.id = qq.quiz_id WHERE qz.status = 'PUBLISHED'",
                Long.class);

        var items = bank(teacher());
        for (Long qid : publishedQids) {
            var item = items.stream().filter(b -> b.id().equals(qid)).findFirst().orElseThrow();
            assertThat(item.usedInPublishedQuiz())
                    .as("câu %d đang nằm trong đề xuất bản", qid).isTrue();
            assertThat(item.usageCount()).isPositive();
        }
    }

    // ---------- tag ----------

    @Test
    void tagLifecycle_createListDelete() {
        var token = teacher();

        var tag = createTag(token, "  Biến và hệ số  ");
        assertThat(tag.id()).isNotNull();
        assertThat(tag.name()).isEqualTo("Biến và hệ số");   // đã trim

        var tags = allTags(token);
        assertThat(tags).extracting(TagDto::name).contains("Biến và hệ số");
        assertThat(tags).extracting(TagDto::name).isSorted(); // sắp xếp alphabet

        exchange("DELETE", "/api/admin/tags/" + tag.id(), null, String.class, token);
        tags = allTags(token);
        assertThat(tags).extracting(TagDto::name).doesNotContain("Biến và hệ số");
    }

    @Test
    void duplicateTagReturns409() {
        var token = teacher();
        createTag(token, "OOP cơ bản");
        try {
            createTag(token, "OOP cơ bản");
            fail("Phải trả 409 khi tag trùng");
        } catch (HttpClientErrorException ex) {
            assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        }
    }

    @Test
    void blankTagNameReturns422() {
        try {
            createTag(teacher(), "   ");
            fail("Phải trả 422 khi tên tag trống");
        } catch (HttpClientErrorException ex) {
            assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    @Test
    void filteringByTagReturnsOnlyTaggedQuestions() {
        var token = teacher();
        var tag = createTag(token, "Toán học " + System.nanoTime());

        var target = bank(token).get(0);
        jdbc.update("INSERT INTO question_tags(question_id, tag_id) VALUES (?,?)", target.id(), tag.id());

        var filtered = bank(token, null, null, tag.id());
        assertThat(filtered).hasSize(1);
        assertThat(filtered.get(0).id()).isEqualTo(target.id());
        // câu đã gắn tag phải hiện tag đó
        assertThat(filtered.get(0).tags()).extracting(TagDto::name).contains(tag.name());
    }

    // ---------- mượn câu hỏi vào đề ----------

    @Test
    void linkQuestionFromBankIntoQuiz() {
        var token = teacher();
        int bankSizeBefore = bank(token).size();
        var allBankIds = bank(token).stream().map(BankItem::id).toList();

        // tạo đề mới rỗng
        var quiz = (Map<String, Object>) exchange("POST", "/api/admin/quizzes",
                Map.of("title", "Đề gom từ ngân hàng",
                        "description", "", "subject", "Lập trình",
                        "durationMin", 15, "maxAttempts", 3, "passPct", 60),
                java.util.Map.class, token);
        Long quizId = ((Number) quiz.get("id")).longValue();

        // chọn 1 câu bất kỳ trong bank
        var pick = bank(token).stream()
                .filter(b -> !b.usedInPublishedQuiz())
                .findFirst().orElseThrow();

        exchange("POST", "/api/admin/quizzes/" + quizId + "/questions/" + pick.id(),
                null, String.class, token);

        var after = (AdminFull) exchange("GET", "/api/admin/quizzes/" + quizId,
                null, AdminFull.class, token);
        assertThat(after.questions).extracting("id").contains(pick.id());

        // câu hỏi gốc KHÔNG bị nhân bản — số câu trong bank không đổi
        assertThat(bank(token)).hasSize(bankSizeBefore);
        assertThat(bank(token)).extracting(BankItem::id).containsExactlyInAnyOrderElementsOf(allBankIds);
    }

    @Test
    void usageCountRisesAfterLinking() {
        var token = teacher();
        var before = bank(token).get(0);
        long usageBefore = before.usageCount();

        var quizId = createEmptyQuiz(token, "Đề để đo usage");
        exchange("POST", "/api/admin/quizzes/" + quizId + "/questions/" + before.id(),
                null, String.class, token);

        var after = bank(token);
        var same = after.stream().filter(b -> b.id().equals(before.id())).findFirst().orElseThrow();
        assertThat(same.usageCount()).isEqualTo((int) usageBefore + 1);
    }

    @Test
    void linkingTwiceReturns409() {
        var token = teacher();
        var quizId = createEmptyQuiz(token, "Đề link trùng");
        var pick = bank(token).get(0);

        exchange("POST", "/api/admin/quizzes/" + quizId + "/questions/" + pick.id(),
                null, String.class, token);
        try {
            exchange("POST", "/api/admin/quizzes/" + quizId + "/questions/" + pick.id(),
                    null, String.class, token);
            fail("Phải trả 409 khi link trùng");
        } catch (HttpClientErrorException ex) {
            assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        }
    }

    @Test
    void unlinkRemovesQuestionFromQuizButKeepsItInBank() {
        var token = teacher();
        var quizId = createEmptyQuiz(token, "Đề sẽ gỡ câu");
        var pick = bank(token).get(0);

        exchange("POST", "/api/admin/quizzes/" + quizId + "/questions/" + pick.id(),
                null, String.class, token);
        exchange("DELETE", "/api/admin/quizzes/" + quizId + "/questions/" + pick.id(),
                null, String.class, token);

        var after = (AdminFull) exchange("GET", "/api/admin/quizzes/" + quizId,
                null, AdminFull.class, token);
        assertThat(after.questions).extracting("id").doesNotContain(pick.id());

        // câu hỏi vẫn còn trong ngân hàng
        assertThat(bank(token)).extracting(BankItem::id).contains(pick.id());
    }

    @Test
    void unlinkQuestionNotInQuizReturns404() {
        try {
            exchange("DELETE", "/api/admin/quizzes/999999/questions/999999", null, String.class, teacher());
            fail("Phải trả 404");
        } catch (HttpClientErrorException ex) {
            assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Test
    void studentCannotReachQuestionBank() {
        var student = login("hocsinh@demo.vn", "Demo@1234");
        try {
            exchange("GET", "/api/admin/questions", null, String.class, student);
            fail("Học sinh phải bị chặn 403");
        } catch (HttpClientErrorException ex) {
            assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }
    }

    @Test
    void linkSameQuestionIntoTwoQuizzesSharesOneRow() {
        var token = teacher();
        var pick = bank(token).get(0);
        int rowsBefore = mappingRowsFor(pick.id());
        long q1 = createEmptyQuiz(token, "Đề A dùng chung");
        long q2 = createEmptyQuiz(token, "Đề B dùng chung");

        exchange("POST", "/api/admin/quizzes/" + q1 + "/questions/" + pick.id(), null, String.class, token);
        exchange("POST", "/api/admin/quizzes/" + q2 + "/questions/" + pick.id(), null, String.class, token);

        // 1 câu hỏi dùng chung cho 2 đề -> thêm đúng 2 dòng mapping, không copy câu hỏi
        assertThat(mappingRowsFor(pick.id())).isEqualTo(rowsBefore + 2);

        var found = bank(token).stream().filter(b -> b.id().equals(pick.id())).findFirst().orElseThrow();
        assertThat(found.usageCount()).isEqualTo(rowsBefore + 2);
    }

    @Test
    void publishedQuizRefusesNewQuestions() {
        var token = teacher();
        // quiz id 1 trong seed là PUBLISHED
        var pick = bank(token).get(0);
        try {
            exchange("POST", "/api/admin/quizzes/1/questions/" + pick.id(), null, String.class, token);
            fail("Đề đang xuất bản không được thêm câu");
        } catch (HttpClientErrorException ex) {
            assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        }
    }

    /* ---------- helpers ---------- */

    private int mappingRowsFor(Long questionId) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM quiz_questions WHERE question_id = ?", Integer.class, questionId);
    }

    private long createEmptyQuiz(String token, String title) {
        var quiz = (Map<String, Object>) exchange("POST", "/api/admin/quizzes",
                Map.of("title", title, "description", "", "subject", "Lập trình",
                        "durationMin", 15, "maxAttempts", 3, "passPct", 60),
                java.util.Map.class, token);
        return ((Number) quiz.get("id")).longValue();
    }

    public static class AdminFull {
        public Long id;
        public String title;
        public List<AdminQuestion> questions;
    }

    public static class AdminQuestion {
        public Long id;
        public String text;
    }
}
