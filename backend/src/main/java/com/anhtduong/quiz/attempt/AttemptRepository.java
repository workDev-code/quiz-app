package com.anhtduong.quiz.attempt;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AttemptRepository extends JpaRepository<AttemptEntity, Long> {

    Optional<AttemptEntity> findByUserIdAndClientKey(Long userId, String clientKey);

    long countByUserIdAndQuizId(Long userId, Long quizId);

    @Query("SELECT COUNT(a) FROM AttemptEntity a WHERE a.quiz.id = :quizId")
    long countByQuizId(@Param("quizId") Long quizId);

    @Query("SELECT COUNT(a) FROM AttemptEntity a WHERE a.user.id = :userId")
    long countByUserId(@Param("userId") Long userId);

    boolean existsByQuizId(Long quizId);

    @Query("SELECT a FROM AttemptEntity a WHERE a.user.id = :userId ORDER BY a.submittedAt DESC")
    List<AttemptEntity> findAllByUserOrdered(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT a FROM AttemptEntity a WHERE a.user.id = :userId AND (:quizId IS NULL OR a.quiz.id = :quizId) ORDER BY a.submittedAt DESC")
    Page<AttemptEntity> findHistory(@Param("userId") Long userId, @Param("quizId") Long quizId, Pageable pageable);

    @Query("SELECT a FROM AttemptEntity a WHERE a.quiz.id = :quizId ORDER BY a.submittedAt DESC")
    List<AttemptEntity> findByQuizOrdered(@Param("quizId") Long quizId);

    @Query("SELECT AVG(a.percent) FROM AttemptEntity a WHERE a.user.id = :userId")
    Double avgPercentByUser(@Param("userId") Long userId);

    @Query("SELECT AVG(a.percent) FROM AttemptEntity a WHERE a.quiz.id = :quizId")
    Double avgPercentByQuiz(@Param("quizId") Long quizId);
}
