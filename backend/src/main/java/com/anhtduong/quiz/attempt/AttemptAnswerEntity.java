package com.anhtduong.quiz.attempt;

import jakarta.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "attempt_answers")
public class AttemptAnswerEntity implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attempt_id", nullable = false)
    private AttemptEntity attempt;

    @Column(name = "question_id_snapshot", nullable = false)
    private Long questionIdSnapshot;

    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;

    @Column(name = "chosen_option_id")
    private Long chosenOptionId;

    @Column(name = "chosen_option_text", columnDefinition = "TEXT")
    private String chosenOptionText;

    @Column(name = "correct_option_id", nullable = false)
    private Long correctOptionId;

    @Column(name = "correct_option_text", nullable = false, columnDefinition = "TEXT")
    private String correctOptionText;

    @Column(name = "explanation_snapshot", columnDefinition = "TEXT")
    private String explanationSnapshot;

    @Column(name = "question_position", nullable = false)
    private Integer questionPosition;

    @Column(nullable = false)
    private Integer position;

    // getters / setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public AttemptEntity getAttempt() { return attempt; }
    public void setAttempt(AttemptEntity attempt) { this.attempt = attempt; }
    public Long getQuestionIdSnapshot() { return questionIdSnapshot; }
    public void setQuestionIdSnapshot(Long questionIdSnapshot) { this.questionIdSnapshot = questionIdSnapshot; }
    public String getQuestionText() { return questionText; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }
    public Long getChosenOptionId() { return chosenOptionId; }
    public void setChosenOptionId(Long chosenOptionId) { this.chosenOptionId = chosenOptionId; }
    public String getChosenOptionText() { return chosenOptionText; }
    public void setChosenOptionText(String chosenOptionText) { this.chosenOptionText = chosenOptionText; }
    public Long getCorrectOptionId() { return correctOptionId; }
    public void setCorrectOptionId(Long correctOptionId) { this.correctOptionId = correctOptionId; }
    public String getCorrectOptionText() { return correctOptionText; }
    public void setCorrectOptionText(String correctOptionText) { this.correctOptionText = correctOptionText; }
    public String getExplanationSnapshot() { return explanationSnapshot; }
    public void setExplanationSnapshot(String explanationSnapshot) { this.explanationSnapshot = explanationSnapshot; }
    public Integer getQuestionPosition() { return questionPosition; }
    public void setQuestionPosition(Integer questionPosition) { this.questionPosition = questionPosition; }
    public Integer getPosition() { return position; }
    public void setPosition(Integer position) { this.position = position; }
}
