package com.anhtduong.quiz.question;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionTagRepository extends JpaRepository<QuestionTagEntity, QuestionTagEntity.PK> {

    @Query("SELECT qt FROM QuestionTagEntity qt WHERE qt.questionId IN :qids")
    List<QuestionTagEntity> findByQuestionIds(@Param("qids") List<Long> qids);

    @Query("SELECT qt.questionId FROM QuestionTagEntity qt WHERE qt.tagId = :tagId")
    List<Long> findQuestionIdsByTagId(@Param("tagId") Long tagId);

    @Modifying
    @Query("DELETE FROM QuestionTagEntity qt WHERE qt.tagId = :tagId")
    void deleteByTagId(@Param("tagId") Long tagId);
}
