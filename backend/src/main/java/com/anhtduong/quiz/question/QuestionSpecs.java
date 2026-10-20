package com.anhtduong.quiz.question;

import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.util.Locale;

/**
 * Các điều kiện lọc cho ngân hàng câu hỏi.
 *
 * <p>Mỗi điều kiện trả về {@code null} khi không có bộ lọc tương ứng — Spring Data tự
 * bỏ qua {@code null}. Nhờ vậy SQL chỉ chứa đúng những điều kiện thật sự cần dùng.
 *
 * <p>Không dùng {@code :param IS NULL} trong JPQL: Hibernate 6 không suy luận được kiểu
 * của tham số null trong biểu thức đó, Postgres hiểu là {@code bytea} và báo lỗi
 * ({@code lower(bytea)} / {@code cannot cast bytea to bigint}).
 */
public final class QuestionSpecs {

    private QuestionSpecs() {}

    /** Lọc theo từ khoá trong nội dung câu hỏi, không phân biệt hoa thường. */
    public static Specification<QuestionEntity> textContains(String search) {
        if (isBlank(search)) return null;
        String like = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get("text")), like);
    }

    /** Lọc theo độ khó (EASY / MEDIUM / HARD). */
    public static Specification<QuestionEntity> difficultyIs(String difficulty) {
        if (isBlank(difficulty)) return null;
        String d = difficulty.trim().toUpperCase(Locale.ROOT);
        return (root, query, cb) -> cb.equal(root.get("difficulty"), d);
    }

    /** Chỉ lấy câu hỏi có gắn đúng tag này. */
    public static Specification<QuestionEntity> hasTag(Long tagId) {
        if (tagId == null) return null;
        return (root, query, cb) -> {
            Subquery<Integer> sub = query.subquery(Integer.class);
            Root<QuestionTagEntity> qt = sub.from(QuestionTagEntity.class);
            sub.select(cb.literal(1))
               .where(cb.equal(qt.get("questionId"), root.get("id")),
                      cb.equal(qt.get("tagId"), tagId));
            return cb.exists(sub);
        };
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
