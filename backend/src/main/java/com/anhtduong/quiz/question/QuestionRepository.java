package com.anhtduong.quiz.question;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuestionRepository extends JpaRepository<QuestionEntity, Long> {

    @Query("SELECT q FROM QuestionEntity q LEFT JOIN FETCH q.options WHERE q.id = :id")
    Optional<QuestionEntity> findFullById(@Param("id") Long id);

    /**
     * Backward-compat: dùng bởi AttemptService để lấy options theo từng câu khi chấm.
     */
    @Query("SELECT q FROM QuestionEntity q LEFT JOIN FETCH q.options WHERE q.quizId = :quizId ORDER BY q.position ASC")
    List<QuestionEntity> findByQuizIdOrderByPositionAsc(@Param("quizId") Long quizId);


    /**
     * Tìm trong ngân hàng câu hỏi: tất cả câu hỏi (không phụ thuộc 1 quiz cụ thể),
     * có lọc theo text + difficulty + tags.
     */
    @Query("""
        SELECT DISTINCT q FROM QuestionEntity q
        LEFT JOIN q.options o
        WHERE (:search IS NULL OR LOWER(q.text) LIKE LOWER(CONCAT('%', :search, '%')))
          AND (:difficulty IS NULL OR q.difficulty = :difficulty)
          AND (:tagId IS NULL OR EXISTS (
            SELECT 1 FROM QuestionTagEntity qt WHERE qt.questionId = q.id AND qt.tagId = :tagId
          ))
        ORDER BY q.id DESC
        """)
    List<QuestionEntity> searchBank(@Param("search") String search,
                                    @Param("difficulty") String difficulty,
                                    @Param("tagId") Long tagId);
}
