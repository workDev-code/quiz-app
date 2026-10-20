package com.anhtduong.quiz.quiz;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuizRepository extends JpaRepository<QuizEntity, Long> {

    @Query("SELECT q FROM QuizEntity q WHERE q.status = :status ORDER BY q.updatedAt DESC")
    List<QuizEntity> findAllByStatusOrdered(@Param("status") QuizEntity.Status status);

    @Query("SELECT COUNT(a) FROM AttemptEntity a WHERE a.quiz.id = :quizId")
    long countAttempts(@Param("quizId") Long quizId);
}
