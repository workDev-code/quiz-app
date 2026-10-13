package com.anhtduong.quiz.attempt;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public class AttemptDtos {

    public record AnswerInput(@NotNull Long questionId, Long optionId) {}

    public record SubmitRequest(
            @NotBlank @Size(min = 4, max = 80) String clientKey,
            Instant startedAt,
            @Valid List<AnswerInput> answers
    ) {}

    public static class Brief {
        public Long id;
        public Long quizId;
        public String quizTitle;
        public Integer seq;
        public Integer score;
        public Integer total;
        public Integer percent;
        public boolean passed;
        public Instant submittedAt;
    }

    public static class ReviewOption {
        public Long id;
        public String text;
        public boolean correct;
        public boolean chosen;
    }

    public static class ReviewQuestion {
        public Long questionId;
        public String text;
        public Long chosenOptionId;
        public Long correctOptionId;
        public String explanation;
        public boolean answered;
        public boolean correct;
        public List<ReviewOption> options;
    }

    public static class Detail {
        public Long id;
        public Long quizId;
        public String quizTitle;
        public Integer seq;
        public Integer score;
        public Integer total;
        public Integer percent;
        public boolean passed;
        public Integer passPct;
        public Integer maxAttempts;
        public Integer attemptsLeft;
        public boolean canRetry;
        public Instant submittedAt;
        public List<ReviewQuestion> items;
    }

    public static class HistoryItem {
        public Long id;
        public Long quizId;
        public String quizTitle;
        public Integer seq;
        public Integer score;
        public Integer total;
        public Integer percent;
        public boolean passed;
        public Instant submittedAt;
    }

    public static class HistoryPage {
        public List<HistoryItem> items;
        public int page;
        public int size;
        public long total;
        public int totalPages;
        public Integer avgPercent;     // tổng quan: % trung bình toàn bộ attempts
        public int totalAttempts;
    }
}
