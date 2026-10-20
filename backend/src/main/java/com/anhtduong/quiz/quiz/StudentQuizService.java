package com.anhtduong.quiz.quiz;

import com.anhtduong.quiz.attempt.AttemptEntity;
import com.anhtduong.quiz.attempt.AttemptRepository;
import com.anhtduong.quiz.common.BusinessException;
import com.anhtduong.quiz.question.AnswerOptionEntity;
import com.anhtduong.quiz.question.QuestionEntity;
import com.anhtduong.quiz.question.QuestionRepository;
import com.anhtduong.quiz.question.QuizQuestionEntity;
import com.anhtduong.quiz.question.QuizQuestionRepository;
import com.anhtduong.quiz.user.CurrentUser;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class StudentQuizService {
    private final QuizRepository quizzes;
    private final AttemptRepository attempts;
    private final QuestionRepository questions;
    private final QuizQuestionRepository quizQuestions;

    public StudentQuizService(QuizRepository quizzes,
                              AttemptRepository attempts,
                              QuestionRepository questions,
                              QuizQuestionRepository quizQuestions) {
        this.quizzes = quizzes;
        this.attempts = attempts;
        this.questions = questions;
        this.quizQuestions = quizQuestions;
    }

    @Transactional(readOnly = true)
    public List<StudentDtos.Summary> listPublished() {
        Long userId = CurrentUser.id();
        return quizzes.findAllByStatusOrdered(QuizEntity.Status.PUBLISHED).stream()
                .map(q -> toSummary(q, userId)).toList();
    }

    @Transactional(readOnly = true)
    public StudentDtos.Detail detail(Long quizId) {
        Long userId = CurrentUser.id();
        QuizEntity q = quizzes.findById(quizId)
                .filter(x -> x.getStatus() == QuizEntity.Status.PUBLISHED)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "QUIZ_NOT_FOUND",
                        "Không tìm thấy bài kiểm tra."));
        StudentDtos.Summary sum = toSummary(q, userId);
        // Load questions via ngân hàng mapping
        List<QuestionEntity> qs = loadQuestionsForQuiz(q.getId());
        boolean hasQuestions = !qs.isEmpty();
        boolean canStart = hasQuestions && sum.available && sum.attemptsUsed < sum.maxAttempts;

        String unavailableReason = null;
        if (!hasQuestions) {
            unavailableReason = "Bài kiểm tra chưa có câu hỏi.";
        } else if (!sum.available) {
            unavailableReason = "Bài kiểm tra đã đóng.";
        } else if (sum.attemptsUsed >= sum.maxAttempts) {
            unavailableReason = "Bạn đã dùng hết " + sum.maxAttempts + " lượt làm bài.";
        }

        StudentDtos.Detail d = new StudentDtos.Detail();
        d.summary = sum;
        d.unavailableReason = unavailableReason;
        d.canStart = canStart;
        if (canStart) {
            d.questions = qs.stream().map(this::toPublicQuestion).toList();
        }
        return d;
    }

    private List<QuestionEntity> loadQuestionsForQuiz(Long quizId) {
        // 1. Lấy IDs qua mapping
        List<QuizQuestionEntity> mappings = quizQuestions.findByQuizIdOrdered(quizId);
        if (mappings.isEmpty()) return List.of();
        List<Long> qids = mappings.stream().map(QuizQuestionEntity::getQuestionId).toList();
        // 2. Load questions + options (1 truy vấn)
        Map<Long, QuestionEntity> byId = questions.findAllById(qids).stream()
                .collect(Collectors.toMap(QuestionEntity::getId, qe -> qe));
        // 3. Trả về theo đúng thứ tự position
        List<QuestionEntity> ordered = new ArrayList<>();
        for (QuizQuestionEntity m : mappings) {
            QuestionEntity qe = byId.get(m.getQuestionId());
            if (qe != null) ordered.add(qe);
        }
        // Force load options
        ordered.forEach(qq -> qq.getOptions().size());
        return ordered;
    }

    private StudentDtos.PublicQuestion toPublicQuestion(QuestionEntity q) {
        StudentDtos.PublicQuestion pq = new StudentDtos.PublicQuestion();
        pq.id = q.getId();
        pq.text = q.getText();
        pq.options = q.getOptions().stream()
                .map(o -> new StudentDtos.PublicOption(o.getId(), o.getText()))
                .toList();
        return pq;
    }

    private StudentDtos.Summary toSummary(QuizEntity q, Long userId) {
        long used = (userId == null) ? 0 : attempts.countByUserIdAndQuizId(userId, q.getId());
        Integer best = null;
        if (userId != null && used > 0) {
            List<AttemptEntity> last = attempts.findHistory(userId, q.getId(),
                    PageRequest.of(0, 100)).getContent();
            best = last.stream().map(AttemptEntity::getPercent).max(Integer::compareTo).orElse(null);
        }
        boolean open = q.getStatus() == QuizEntity.Status.PUBLISHED
                && (q.getClosesAt() == null || q.getClosesAt().isAfter(Instant.now()));
        // Đếm số câu qua mapping
        long qCount = quizQuestions.countByQuizId(q.getId());

        StudentDtos.Summary s = new StudentDtos.Summary();
        s.id = q.getId();
        s.title = q.getTitle();
        s.description = q.getDescription();
        s.subject = q.getSubject();
        s.questionCount = (int) qCount;
        s.durationMin = q.getDurationMin();
        s.maxAttempts = q.getMaxAttempts();
        s.passPct = q.getPassPct();
        s.closesAt = q.getClosesAt();
        s.attemptsUsed = (int) used;
        s.bestPercent = best;
        s.available = open;
        s.attemptsLeft = Math.max(0, q.getMaxAttempts() - (int) used);
        return s;
    }
}
