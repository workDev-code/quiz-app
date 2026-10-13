package com.anhtduong.quiz.question;

import jakarta.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "answer_options")
public class AnswerOptionEntity implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private QuestionEntity question;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String text;

    @Column(nullable = false)
    private boolean correct;

    @Column(nullable = false)
    private Integer position = 0;

    // getters / setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public QuestionEntity getQuestion() { return question; }
    public void setQuestion(QuestionEntity question) { this.question = question; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public boolean isCorrect() { return correct; }
    public void setCorrect(boolean correct) { this.correct = correct; }
    public Integer getPosition() { return position; }
    public void setPosition(Integer position) { this.position = position; }
}
