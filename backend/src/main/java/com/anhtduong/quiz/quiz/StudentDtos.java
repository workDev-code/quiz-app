package com.anhtduong.quiz.quiz;

import java.time.Instant;
import java.util.List;

public class StudentDtos {

    public static class Summary {
        public Long id;
        public String title;
        public String description;
        public String subject;
        public Integer questionCount;
        public Integer durationMin;
        public Integer maxAttempts;
        public Integer passPct;
        public Instant closesAt;
        public Integer attemptsUsed;
        public Integer bestPercent;
        public boolean available;
        public Integer attemptsLeft;
    }

    public static class Detail {
        public Summary summary;
        public String unavailableReason;     // null khi có thể bắt đầu
        public boolean canStart;
        public List<PublicQuestion> questions; // chỉ trả khi canStart, KHÔNG có correct/explanation
    }

    public static class PublicQuestion {
        public Long id;
        public String text;
        public List<PublicOption> options;
    }

    public record PublicOption(Long id, String text) {}
}
