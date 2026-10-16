package com.anhtduong.quiz.attempt;

import com.anhtduong.quiz.common.BusinessException;
import com.anhtduong.quiz.question.AnswerOptionEntity;
import com.anhtduong.quiz.question.QuestionEntity;
import com.anhtduong.quiz.question.QuestionRepository;
import com.anhtduong.quiz.quiz.QuizEntity;
import com.anhtduong.quiz.quiz.QuizRepository;
import com.anhtduong.quiz.user.CurrentUser;
import com.anhtduong.quiz.user.UserEntity;
import com.anhtduong.quiz.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
public class AttemptService {

    private static final Logger log = LoggerFactory.getLogger(AttemptService.class);
    private static final Duration SUBMIT_GRACE = Duration.ofSeconds(30);

    private final QuizRepository quizzes;
    private final AttemptRepository attempts;
    private final UserRepository users;

    private final QuestionRepository questions;

    public AttemptService(QuizRepository quizzes, AttemptRepository attempts, UserRepository users, QuestionRepository questions) {
        this.quizzes = quizzes;
        this.attempts = attempts;
        this.users = users;
        this.questions = questions;
    }

    /**
     * Học sinh nộp bài. Hàm này chứa tất cả logic chấm điểm phía server:
     * <ol>
     *   <li>Idempotency: nếu (user, clientKey) đã có → trả lại attempt cũ, 200.</li>
     *   <li>Quiz phải PUBLISHED, mở, còn lượt.</li>
     *   <li>Thời gian làm bài: server xác nhận startedAt + durationMin + grace.</li>
     *   <li>Validate câu trả lời (câu phải thuộc quiz, đáp án thuộc câu, không trùng).</li>
     *   <li>Chấm điểm server-side, ghi snapshot, lưu attempt + answers.</li>
     * </ol>
     */
    @Transactional
    public AttemptDtos.Brief submit(Long quizId, AttemptDtos.SubmitRequest req) {
        Long userId = CurrentUser.id();
        if (userId == null) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "Bạn cần đăng nhập để tiếp tục.");
        }
        if (req.clientKey() == null || req.clientKey().isBlank()) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "VALIDATION_FAILED", "Thiếu clientKey.");
        }

        // 1) Idempotency check: trả về attempt cũ nếu đã nộp với clientKey này.
        Optional<AttemptEntity> existing = attempts.findByUserIdAndClientKey(userId, req.clientKey());
        if (existing.isPresent()) {
            return brief(existing.get(), true);
        }

        // 2) Quiz phải tồn tại và PUBLISHED
        QuizEntity q = quizzes.findWithQuestions(quizId)
                .filter(x -> x.getStatus() == QuizEntity.Status.PUBLISHED)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "QUIZ_NOT_FOUND",
                        "Không tìm thấy bài kiểm tra."));
        q.getQuestions().forEach(qq -> qq.getOptions().size());

        // 3) Đã đóng cửa?
        if (q.getClosesAt() != null && q.getClosesAt().isBefore(Instant.now())) {
            throw new BusinessException(HttpStatus.CONFLICT, "QUIZ_CLOSED",
                    "Bài kiểm tra đã đóng, không nhận bài nữa.");
        }

        // 4) Hết lượt?
        long used = attempts.countByUserIdAndQuizId(userId, q.getId());
        if (used >= q.getMaxAttempts()) {
            throw new BusinessException(HttpStatus.CONFLICT, "MAX_ATTEMPTS",
                    "Bạn đã dùng hết số lượt làm bài.");
        }

        // 5) Thời gian làm bài: server xác nhận
        Instant started = req.startedAt() == null ? Instant.now() : req.startedAt();
        Instant deadline = started.plus(Duration.ofMinutes(q.getDurationMin())).plus(SUBMIT_GRACE);
        if (Instant.now().isAfter(deadline)) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "TIME_EXPIRED",
                    "Đã quá thời gian làm bài.");
        }

        // 6) Validate answers
        List<AttemptDtos.AnswerInput> raw = req.answers() == null ? List.of() : req.answers();
        Map<Long, QuestionEntity> byQid = new HashMap<>();
        for (QuestionEntity qe : q.getQuestions()) {
            byQid.put(qe.getId(), qe);
        }
        Set<Long> seen = new HashSet<>();
        Map<Long, AnswerOptionEntity> chosen = new HashMap<>();
        for (AttemptDtos.AnswerInput a : raw) {
            if (a.questionId() == null) {
                throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_QUESTION",
                        "Có câu hỏi không thuộc bài kiểm tra này.");
            }
            QuestionEntity qe = byQid.get(a.questionId());
            if (qe == null) {
                throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_QUESTION",
                        "Có câu hỏi không thuộc bài kiểm tra này.");
            }
            if (!seen.add(qe.getId())) {
                throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "DUPLICATE_ANSWER",
                        "Một câu hỏi bị trả lời hai lần.");
            }
            if (a.optionId() != null) {
                AnswerOptionEntity opt = qe.getOptions().stream()
                        .filter(o -> Objects.equals(o.getId(), a.optionId())).findFirst().orElse(null);
                if (opt == null) {
                    throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "OPTION_NOT_IN_QUESTION",
                            "Đáp án được chọn không thuộc câu hỏi tương ứng.");
                }
                chosen.put(qe.getId(), opt);
            }
        }

        // 7) Build user, attempt, snapshot, chấm điểm
        UserEntity user = users.getReferenceById(userId);
        int totalQuestions = q.getQuestions().size();
        int score = 0;
        int pos = 0;
        AttemptEntity attempt = new AttemptEntity();
        attempt.setUser(user);
        attempt.setQuiz(q);
        attempt.setSeq((int) used + 1);
        attempt.setScore(0);
        attempt.setTotal(totalQuestions);
        attempt.setPercent(0);
        attempt.setPassed(false);
        attempt.setPassPct(q.getPassPct());
        attempt.setClientKey(req.clientKey());
        attempt.setStartedAt(started);
        attempt.setSubmittedAt(Instant.now());

        List<QuestionEntity> orderedQuestions = new ArrayList<>(q.getQuestions());
        orderedQuestions.sort(Comparator.comparing(QuestionEntity::getPosition));
        for (QuestionEntity qe : orderedQuestions) {
            AnswerOptionEntity correctOpt = qe.getOptions().stream().filter(AnswerOptionEntity::isCorrect)
                    .findFirst().orElse(null);
            AnswerOptionEntity chosenOpt = chosen.get(qe.getId());
            boolean isCorrect = chosenOpt != null && correctOpt != null
                    && Objects.equals(chosenOpt.getId(), correctOpt.getId());
            if (isCorrect) score++;

            AttemptAnswerEntity ans = new AttemptAnswerEntity();
            ans.setAttempt(attempt);
            ans.setQuestionIdSnapshot(qe.getId());
            ans.setQuestionText(qe.getText());
            ans.setChosenOptionId(chosenOpt == null ? null : chosenOpt.getId());
            ans.setChosenOptionText(chosenOpt == null ? null : chosenOpt.getText());
            ans.setCorrectOptionId(correctOpt == null ? -1L : correctOpt.getId());
            ans.setCorrectOptionText(correctOpt == null ? "" : correctOpt.getText());
            ans.setExplanationSnapshot(qe.getExplanation());
            ans.setQuestionPosition(qe.getPosition());
            ans.setPosition(pos++);
            attempt.getAnswers().add(ans);
        }

        int percent = totalQuestions == 0 ? 0 : Math.round(100f * score / totalQuestions);
        attempt.setScore(score);
        attempt.setPercent(percent);
        attempt.setPassed(percent >= q.getPassPct());

        try {
            attempt = attempts.saveAndFlush(attempt);
        } catch (DataIntegrityViolationException dup) {
            // Race: người khác (cùng user, cùng clientKey) đã ghi. Lấy attempt cũ.
            log.info("Idempotency race cho user {} clientKey {}", userId, req.clientKey());
            AttemptEntity prev = attempts.findByUserIdAndClientKey(userId, req.clientKey())
                    .orElseThrow(() -> new BusinessException(HttpStatus.CONFLICT, "DATA_CONFLICT",
                            "Dữ liệu bị xung đột."));
            return brief(prev, true);
        }
        return brief(attempt, false);
    }

    @Transactional(readOnly = true)
    public AttemptDtos.Detail get(Long attemptId) {
        Long userId = CurrentUser.id();
        AttemptEntity a = attempts.findById(attemptId).orElseThrow(() ->
                new BusinessException(HttpStatus.NOT_FOUND, "ATTEMPT_NOT_FOUND",
                        "Không tìm thấy bài làm."));
        if (!Objects.equals(a.getUser().getId(), userId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "NOT_YOUR_ATTEMPT",
                    "Bạn chỉ xem được kết quả bài làm của chính mình.");
        }
        AttemptDtos.Detail d = new AttemptDtos.Detail();
        d.id = a.getId();
        d.quizId = a.getQuiz().getId();
        d.quizTitle = a.getQuiz().getTitle();
        d.seq = a.getSeq();
        d.score = a.getScore();
        d.total = a.getTotal();
        d.percent = a.getPercent();
        d.passed = a.isPassed();
        d.passPct = a.getPassPct();
        d.submittedAt = a.getSubmittedAt();
        QuizEntity q = a.getQuiz();
        long used = attempts.countByUserIdAndQuizId(userId, q.getId());
        d.maxAttempts = q.getMaxAttempts();
        d.attemptsLeft = Math.max(0, q.getMaxAttempts() - (int) used);
        boolean open = q.getStatus() == QuizEntity.Status.PUBLISHED
                && (q.getClosesAt() == null || q.getClosesAt().isAfter(Instant.now()));
        d.canRetry = open && d.attemptsLeft > 0;
        // Load questions + options for review (1 truy vấn cho cả attempt)
        java.util.Map<Long, QuestionEntity> qById = questions.findByQuizIdOrderByPositionAsc(q.getId()).stream()
                .collect(java.util.stream.Collectors.toMap(QuestionEntity::getId, x -> x));
        d.items = a.getAnswers().stream()
                .sorted(Comparator.comparingInt(AttemptAnswerEntity::getPosition))
                .map(aa -> toReview(aa, qById))
                .toList();
        return d;
    }

    private AttemptDtos.ReviewQuestion toReview(AttemptAnswerEntity aa, java.util.Map<Long, QuestionEntity> qById) {
        AttemptDtos.ReviewQuestion r = new AttemptDtos.ReviewQuestion();
        r.questionId = aa.getQuestionIdSnapshot();
        r.text = aa.getQuestionText();
        r.chosenOptionId = aa.getChosenOptionId();
        r.correctOptionId = aa.getCorrectOptionId();
        r.explanation = aa.getExplanationSnapshot();
        r.answered = aa.getChosenOptionId() != null;
        r.correct = r.answered && Objects.equals(aa.getChosenOptionId(), aa.getCorrectOptionId());
        // Lấy options từ question (hiện tại - trade-off: nếu giáo viên sửa, review dùng bản mới).
        // TODO: snapshot toàn bộ options vào attempt_answers để bảo toàn lịch sử.
        QuestionEntity q = qById.get(aa.getQuestionIdSnapshot());
        if (q != null) {
            r.options = q.getOptions().stream()
                    .sorted(java.util.Comparator.comparingInt(AnswerOptionEntity::getPosition))
                    .map(o -> {
                        AttemptDtos.ReviewOption ro = new AttemptDtos.ReviewOption();
                        ro.id = o.getId();
                        ro.text = o.getText();
                        ro.correct = o.isCorrect();
                        ro.chosen = aa.getChosenOptionId() != null
                                && Objects.equals(o.getId(), aa.getChosenOptionId());
                        return ro;
                    })
                    .toList();
        }
        return r;
    }

    @Transactional(readOnly = true)
    public AttemptDtos.HistoryPage history(int page, int size, Long quizId) {
        Long userId = CurrentUser.id();
        int safeSize = Math.max(1, Math.min(50, size));
        int safePage = Math.max(0, page);
        Page<AttemptEntity> p = attempts.findHistory(userId, quizId, PageRequest.of(safePage, safeSize));
        Double avg = attempts.avgPercentByUser(userId);
        int totalAttempts = (int) attempts.countByUserId(userId);

        AttemptDtos.HistoryPage out = new AttemptDtos.HistoryPage();
        out.page = p.getNumber();
        out.size = p.getSize();
        out.total = p.getTotalElements();
        out.totalPages = p.getTotalPages();
        out.avgPercent = (avg == null) ? null : (int) Math.round(avg.doubleValue());
        out.totalAttempts = totalAttempts;
        out.items = p.getContent().stream().map(a -> {
            AttemptDtos.HistoryItem it = new AttemptDtos.HistoryItem();
            it.id = a.getId();
            it.quizId = a.getQuiz().getId();
            it.quizTitle = a.getQuiz().getTitle();
            it.seq = a.getSeq();
            it.score = a.getScore();
            it.total = a.getTotal();
            it.percent = a.getPercent();
            it.passed = a.isPassed();
            it.submittedAt = a.getSubmittedAt();
            return it;
        }).toList();
        return out;
    }

    private AttemptDtos.Brief brief(AttemptEntity a, boolean duplicate) {
        AttemptDtos.Brief b = new AttemptDtos.Brief();
        b.id = a.getId();
        b.quizId = a.getQuiz().getId();
        b.quizTitle = a.getQuiz().getTitle();
        b.seq = a.getSeq();
        b.score = a.getScore();
        b.total = a.getTotal();
        b.percent = a.getPercent();
        b.passed = a.isPassed();
        b.submittedAt = a.getSubmittedAt();
        return b;
    }
}
