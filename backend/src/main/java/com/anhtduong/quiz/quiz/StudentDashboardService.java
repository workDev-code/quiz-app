package com.anhtduong.quiz.quiz;

import com.anhtduong.quiz.attempt.AttemptEntity;
import com.anhtduong.quiz.attempt.AttemptRepository;
import com.anhtduong.quiz.question.QuizQuestionRepository;
import com.anhtduong.quiz.user.CurrentUser;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class StudentDashboardService {
    private final QuizRepository quizzes;
    private final AttemptRepository attempts;
    private final QuizQuestionRepository quizQuestions;

    public StudentDashboardService(QuizRepository quizzes, AttemptRepository attempts, QuizQuestionRepository quizQuestions) {
        this.quizzes = quizzes;
        this.attempts = attempts;
        this.quizQuestions = quizQuestions;
    }

    @Transactional(readOnly = true)
    public Dash get() {
        Long userId = CurrentUser.id();
        List<QuizEntity> all = quizzes.findAllByStatusOrdered(QuizEntity.Status.PUBLISHED);
        int available = 0;
        List<StudentDtos.Summary> top = new ArrayList<>();
        for (QuizEntity q : all) {
            long used = attempts.countByUserIdAndQuizId(userId, q.getId());
            int left = q.getMaxAttempts() - (int) used;
            boolean open = q.getClosesAt() == null || q.getClosesAt().isAfter(Instant.now());
            long qCount = quizQuestions.countByQuizId(q.getId());
            if (open && left > 0 && qCount > 0) {
                available++;
                if (top.size() < 3) {
                    StudentDtos.Summary s = new StudentDtos.Summary();
                    s.id = q.getId();
                    s.title = q.getTitle();
                    s.subject = q.getSubject();
                    s.questionCount = (int) qCount;
                    s.durationMin = q.getDurationMin();
                    s.maxAttempts = q.getMaxAttempts();
                    s.passPct = q.getPassPct();
                    s.closesAt = q.getClosesAt();
                    s.attemptsUsed = (int) used;
                    s.attemptsLeft = left;
                    s.available = open;
                    top.add(s);
                }
            }
        }
        Long totalAttempts = attempts.countByUserId(userId);
        Double avg = attempts.avgPercentByUser(userId);
        Integer avgPercent = avg == null ? null : (int) Math.round(avg.doubleValue());

        List<AttemptEntity> recent = attempts.findAllByUserOrdered(userId, PageRequest.of(0, 5));
        List<Recent> recentItems = new ArrayList<>();
        for (AttemptEntity a : recent) {
            Recent r = new Recent();
            r.id = a.getId();
            r.quizId = a.getQuiz().getId();
            r.quizTitle = a.getQuiz().getTitle();
            r.seq = a.getSeq();
            r.score = a.getScore();
            r.total = a.getTotal();
            r.percent = a.getPercent();
            r.passed = a.isPassed();
            r.submittedAt = a.getSubmittedAt();
            recentItems.add(r);
        }

        Dash d = new Dash();
        d.availableCount = available;
        d.totalAttempts = totalAttempts.intValue();
        d.avgPercent = avgPercent;
        d.quizzes = top;
        d.recent = recentItems;
        return d;
    }

    public static class Dash {
        public int availableCount;
        public int totalAttempts;
        public Integer avgPercent;
        public List<StudentDtos.Summary> quizzes;
        public List<Recent> recent;
    }

    public static class Recent {
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
}
