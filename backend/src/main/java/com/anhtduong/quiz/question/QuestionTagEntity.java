package com.anhtduong.quiz.question;

import jakarta.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "question_tags")
@IdClass(QuestionTagEntity.PK.class)
public class QuestionTagEntity implements Serializable {
    @Id
    @Column(name = "question_id")
    private Long questionId;
    @Id
    @Column(name = "tag_id")
    private Long tagId;

    public Long getQuestionId() { return questionId; }
    public void setQuestionId(Long v) { this.questionId = v; }
    public Long getTagId() { return tagId; }
    public void setTagId(Long v) { this.tagId = v; }

    public static class PK implements Serializable {
        private Long questionId;
        private Long tagId;
        public PK() {}
        public PK(Long q, Long t) { this.questionId = q; this.tagId = t; }
        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof PK p)) return false;
            return java.util.Objects.equals(questionId, p.questionId) && java.util.Objects.equals(tagId, p.tagId);
        }
        @Override public int hashCode() { return java.util.Objects.hash(questionId, tagId); }
    }
}
