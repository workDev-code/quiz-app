package com.anhtduong.quiz.question;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuizQuestionRepository extends JpaRepository<QuizQuestionEntity, QuizQuestionEntity.PK> {

    @Query("SELECT qq FROM QuizQuestionEntity qq WHERE qq.quizId = :quizId ORDER BY qq.position ASC")
    List<QuizQuestionEntity> findByQuizIdOrdered(@Param("quizId") Long quizId);

    @Modifying
    @Query("DELETE FROM QuizQuestionEntity qq WHERE qq.quizId = :quizId AND qq.questionId = :questionId")
    void unlink(@Param("quizId") Long quizId, @Param("questionId") Long questionId);

    boolean existsByQuizIdAndQuestionId(Long quizId, Long questionId);

    @Query("SELECT COUNT(qq) FROM QuizQuestionEntity qq WHERE qq.quizId = :quizId")
    long countByQuizId(@Param("quizId") Long quizId);

    @Query("SELECT MAX(qq.position) FROM QuizQuestionEntity qq WHERE qq.quizId = :quizId")
    Integer maxPosition(@Param("quizId") Long quizId);

    @Modifying
    @Query("DELETE FROM QuizQuestionEntity qq WHERE qq.quizId = :quizId")
    void deleteByQuizId(@Param("quizId") Long quizId);

    /**
     * Đếm số đề đang dùng mỗi câu hỏi, và đếm riêng số đề đã xuất bản.
     * Mỗi dòng trả về: {questionId, usageCount, publishedCount}.
     * Dùng native query vì cần JOIN sang bảng {@code quizzes}; mapping table cố tình
     * không được map thành quan hệ JPA (giữ QuizQuestionEntity thuần theo id).
     */
    @Query(value = """
            SELECT qq.question_id                                   AS question_id,
                   COUNT(*)                                         AS usage_count,
                   COUNT(*) FILTER (WHERE qz.status = 'PUBLISHED')  AS published_count
            FROM quiz_questions qq
            JOIN quizzes qz ON qz.id = qq.quiz_id
            WHERE qq.question_id IN (:qids)
            GROUP BY qq.question_id
            """, nativeQuery = true)
    List<Object[]> countUsageByQuestionIds(@Param("qids") List<Long> qids);
}
