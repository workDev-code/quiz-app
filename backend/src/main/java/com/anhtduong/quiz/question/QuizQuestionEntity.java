package com.anhtduong.quiz.question;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.Instant;

/**
 * Bảng liên kết nhiều-nhiều giữa Quiz và Question.
 * Đây là cốt lõi của "ngân hàng câu hỏi": một câu có thể thuộc nhiều quiz,
 * một quiz có thể chứa nhiều câu.
 */
@Entity
@Table(name = "quiz_questions")
@IdClass(QuizQuestionEntity.PK.class)
public class QuizQuestionEntity implements Serializable {

    @Id
    @Column(name = "quiz_id")
    private Long quizId;

    @Id
    @Column(name = "question_id")
    private Long questionId;

    @Column(nullable = false)
    private Integer position;

    @Column(nullable = false, precision = 6, scale = 2)
    private java.math.BigDecimal points = new java.math.BigDecimal("1.00");

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() { this.createdAt = Instant.now(); }

    public Long getQuizId() { return quizId; }
    public void setQuizId(Long quizId) { this.quizId = quizId; }
    public Long getQuestionId() { return questionId; }
    public void setQuestionId(Long questionId) { this.questionId = questionId; }
    public Integer getPosition() { return position; }
    public void setPosition(Integer position) { this.position = position; }
    public java.math.BigDecimal getPoints() { return points; }
    public void setPoints(java.math.BigDecimal points) { this.points = points; }
    public Instant getCreatedAt() { return createdAt; }

    public static class PK implements Serializable {
        private Long quizId;
        private Long questionId;
        public PK() {}
        public PK(Long quizId, Long questionId) { this.quizId = quizId; this.questionId = questionId; }
        public Long getQuizId() { return quizId; }
        public void setQuizId(Long quizId) { this.quizId = quizId; }
        public Long getQuestionId() { return questionId; }
        public void setQuestionId(Long questionId) { this.questionId = questionId; }
        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof PK pk)) return false;
            return java.util.Objects.equals(quizId, pk.quizId) && java.util.Objects.equals(questionId, pk.questionId);
        }
        @Override public int hashCode() { return java.util.Objects.hash(quizId, questionId); }
    }
}
