package com.anhtduong.quiz.question;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "questions")
public class QuestionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Backward-compat: column kept nullable so old rows keep quiz_id; new code goes through QuizQuestionEntity.
     */
    @Column(name = "quiz_id")
    private Long quizId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String text;

    @Column(columnDefinition = "TEXT")
    private String explanation;

    @Column(name = "position")
    private Integer position;

    /** EASY / MEDIUM / HARD */
    @Column(nullable = false, length = 16)
    private String difficulty = "MEDIUM";

    /** SINGLE_CHOICE / MULTIPLE_CHOICE / TRUE_FALSE */
    @Column(nullable = false, length = 20)
    private String type = "SINGLE_CHOICE";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<AnswerOptionEntity> options = new ArrayList<>();

    @PrePersist
    void onCreate() { this.createdAt = Instant.now(); }

    // getters / setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getQuizId() { return quizId; }
    public void setQuizId(Long quizId) { this.quizId = quizId; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }
    public Integer getPosition() { return position; }
    public void setPosition(Integer position) { this.position = position; }
    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Instant getCreatedAt() { return createdAt; }
    public List<AnswerOptionEntity> getOptions() { return options; }
    public void setOptions(List<AnswerOptionEntity> options) { this.options = options; }
}
