package com.anhtduong.quiz.question;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface QuestionRepository extends JpaRepository<QuestionEntity, Long>,
        JpaSpecificationExecutor<QuestionEntity> {

    @Query("SELECT q FROM QuestionEntity q LEFT JOIN FETCH q.options WHERE q.id = :id")
    Optional<QuestionEntity> findFullById(@Param("id") Long id);

    /**
     * Backward-compat: dùng bởi AttemptService để lấy options theo từng câu khi chấm.
     */
    @Query("SELECT q FROM QuestionEntity q LEFT JOIN FETCH q.options WHERE q.quizId = :quizId ORDER BY q.position ASC")
    List<QuestionEntity> findByQuizIdOrderByPositionAsc(@Param("quizId") Long quizId);

    /**
     * Lấy luôn options trong cùng 1 câu lệnh để tránh N+1 khi dựng danh sách ngân hàng.
     */
    @Override
    @EntityGraph(attributePaths = "options")
    List<QuestionEntity> findAll(@Nullable Specification<QuestionEntity> spec);

    /**
     * Tìm trong ngân hàng câu hỏi — câu hỏi độc lập, không phụ thuộc đề nào.
     * Bộ lọc nào không dùng thì bỏ qua, nên câu lệnh SQL luôn hợp lệ.
     */
    default List<QuestionEntity> searchBank(String search, String difficulty, Long tagId) {
        List<QuestionEntity> found = findAll(Specification.allOf(
                QuestionSpecs.textContains(search),
                QuestionSpecs.difficultyIs(difficulty),
                QuestionSpecs.hasTag(tagId)));

        // EntityGraph nối bảng answer_options nên 1 câu có thể lặp nhiều dòng -> gộp lại theo id.
        Map<Long, QuestionEntity> byId = new LinkedHashMap<>();
        for (QuestionEntity q : found) byId.putIfAbsent(q.getId(), q);

        return byId.values().stream()
                .sorted((a, b) -> Long.compare(b.getId(), a.getId()))   // mới nhất trước
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }
}
