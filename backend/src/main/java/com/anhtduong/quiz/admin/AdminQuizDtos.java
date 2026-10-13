package com.anhtduong.quiz.admin;

import com.anhtduong.quiz.quiz.QuizEntity;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public class AdminQuizDtos {

    public record QuizUpsertRequest(
            @NotBlank @Size(min = 3, max = 180) String title,
            @Size(max = 2000) String description,
            @NotBlank @Size(max = 80) String subject,
            @NotNull @Min(1) @Max(240) Integer durationMin,
            @NotNull @Min(1) @Max(20) Integer maxAttempts,
            @NotNull @Min(0) @Max(100) Integer passPct,
            Instant closesAt
    ) {}

    public static class AdminOption {
        public Long id;
        public String text;
        public boolean correct;
    }

    public static class AdminQuestion {
        public Long id;
        public String text;
        public String explanation;
        public List<AdminOption> options;
        public Long correctId;
    }

    public record CheckItem(String key, String label, boolean ok, String detail) {}

    public static class AdminFull {
        public Long id;
        public String title;
        public String description;
        public String subject;
        public String status;
        public Integer durationMin;
        public Integer maxAttempts;
        public Integer passPct;
        public Instant closesAt;
        public Instant updatedAt;
        public long attemptCount;
        public boolean locked;
        public List<AdminQuestion> questions;
        public List<CheckItem> checks;
    }

    public static class AdminSummary {
        public Long id;
        public String title;
        public String subject;
        public String status;
        public Integer questionCount;
        public long attemptCount;
        public Integer avgPercent;
        public Instant closesAt;
        public Instant updatedAt;
    }

    public record QuestionUpsertRequest(
            @NotBlank @Size(min = 5, max = 2000) String text,
            @Size(max = 2000) String explanation,
            @NotNull @Size(min = 2, max = 5) @Valid List<OptionInput> options
    ) {}

    public record OptionInput(
            Long id,                         // null = tạo mới
            @NotBlank @Size(max = 1000) String text,
            boolean correct
    ) {}

    public record TeacherStats(long publishedCount, long draftCount, long attemptCount) {}

    public record QuizAttemptsResponse(AdminQuizDtos.AdminSummary quiz, List<Map<String, Object>> items) {}
}
