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

    List<QuestionEntity> findByQuizIdOrderByPositionAsc(Long quizId);
}
