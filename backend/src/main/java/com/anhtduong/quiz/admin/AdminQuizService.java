package com.anhtduong.quiz.admin;

import com.anhtduong.quiz.attempt.AttemptEntity;
import com.anhtduong.quiz.attempt.AttemptRepository;
import com.anhtduong.quiz.common.BusinessException;
import com.anhtduong.quiz.question.AnswerOptionEntity;
import com.anhtduong.quiz.question.QuestionEntity;
import com.anhtduong.quiz.question.QuestionRepository;
import com.anhtduong.quiz.quiz.QuizEntity;
import com.anhtduong.quiz.quiz.QuizRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
public class AdminQuizService {

    private final QuizRepository quizzes;
    private final QuestionRepository questions;
    private final AttemptRepository attempts;

    public AdminQuizService(QuizRepository quizzes, QuestionRepository questions, AttemptRepository attempts) {
        this.quizzes = quizzes;
        this.questions = questions;
        this.attempts = attempts;
    }

    @Transactional(readOnly = true)
    public List<AdminQuizDtos.AdminSummary> list() {
        return quizzes.findAll().stream().sorted(Comparator.comparing(QuizEntity::getUpdatedAt).reversed())
                .map(this::toSummary).toList();
    }

    @Transactional(readOnly = true)
    public AdminQuizDtos.AdminFull get(Long id) {
        QuizEntity q = quizzes.findWithQuestions(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "QUIZ_NOT_FOUND",
                        "Không tìm thấy bài kiểm tra."));
        return toFull(q);
    }

    @Transactional
    public AdminQuizDtos.AdminFull create(AdminQuizDtos.QuizUpsertRequest req) {
        validateBasics(req);
        QuizEntity q = new QuizEntity();
        apply(q, req);
        q = quizzes.save(q);
        return toFull(q);
    }

    @Transactional
    public AdminQuizDtos.AdminFull update(Long id, AdminQuizDtos.QuizUpsertRequest req) {
        validateBasics(req);
        QuizEntity q = quizzes.findWithQuestions(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "QUIZ_NOT_FOUND",
                        "Không tìm thấy bài kiểm tra."));
        apply(q, req);
        return toFull(q);
    }

    @Transactional
    public AdminQuizDtos.AdminFull publish(Long id) {
        QuizEntity q = quizzes.findWithQuestions(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "QUIZ_NOT_FOUND",
                        "Không tìm thấy bài kiểm tra."));
        List<AdminQuizDtos.CheckItem> checks = publishChecks(q);
        List<AdminQuizDtos.CheckItem> fails = checks.stream().filter(c -> !c.ok()).toList();
        if (!fails.isEmpty()) {
            List<Map<String, String>> details = fails.stream()
                    .map(f -> Map.of("label", f.label(), "detail", f.detail() == null ? "" : f.detail()))
                    .toList();
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "QUIZ_NOT_PUBLISHABLE",
                    "Bài chưa đủ điều kiện để xuất bản.", details);
        }
        q.setStatus(QuizEntity.Status.PUBLISHED);
        return toFull(q);
    }

    @Transactional
    public AdminQuizDtos.AdminFull unpublish(Long id) {
        QuizEntity q = quizzes.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "QUIZ_NOT_FOUND",
                        "Không tìm thấy bài kiểm tra."));
        q.setStatus(QuizEntity.Status.DRAFT);
        return toFull(q);
    }

    @Transactional
    public AdminQuizDtos.AdminFull addQuestion(Long quizId, AdminQuizDtos.QuestionUpsertRequest req) {
        QuizEntity q = quizzes.findWithQuestions(quizId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "QUIZ_NOT_FOUND",
                        "Không tìm thấy bài kiểm tra."));
        validateQuestion(req, q);
        QuestionEntity qe = new QuestionEntity();
        qe.setQuiz(q);
        qe.setText(req.text().trim());
        qe.setExplanation(req.explanation() == null ? null : req.explanation().trim());
        qe.setPosition(q.getQuestions().size());
        int i = 0;
        for (AdminQuizDtos.OptionInput o : req.options()) {
            AnswerOptionEntity opt = new AnswerOptionEntity();
            opt.setQuestion(qe);
            opt.setText(o.text().trim());
            opt.setCorrect(o.correct());
            opt.setPosition(i++);
            qe.getOptions().add(opt);
        }
        q.getQuestions().add(qe);
        return toFull(q);
    }

    @Transactional
    public AdminQuizDtos.AdminFull updateQuestion(Long questionId, AdminQuizDtos.QuestionUpsertRequest req) {
        QuestionEntity qe = questions.findFullById(questionId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "QUESTION_NOT_FOUND",
                        "Không tìm thấy câu hỏi."));
        qe.getOptions().size();
        QuizEntity q = qe.getQuiz();
        boolean locked = attempts.existsByQuizId(q.getId());
        validateQuestion(req, q);

        if (locked) {
            Set<Long> currentIds = new HashSet<>();
            for (AnswerOptionEntity o : qe.getOptions()) currentIds.add(o.getId());
            Set<Long> nextIds = new HashSet<>();
            for (AdminQuizDtos.OptionInput o : req.options()) {
                if (o.id() != null) nextIds.add(o.id());
            }
            if (!currentIds.equals(nextIds)) {
                throw new BusinessException(HttpStatus.CONFLICT, "QUIZ_HAS_ATTEMPTS",
                        "Đã có học sinh làm bài nên không được xóa đáp án.");
            }
            Long currentCorrect = qe.getOptions().stream().filter(AnswerOptionEntity::isCorrect)
                    .map(AnswerOptionEntity::getId).findFirst().orElse(null);
            Long newCorrect = req.options().stream().filter(AdminQuizDtos.OptionInput::correct)
                    .map(AdminQuizDtos.OptionInput::id).findFirst().orElse(null);
            if (!Objects.equals(currentCorrect, newCorrect)) {
                throw new BusinessException(HttpStatus.CONFLICT, "QUIZ_HAS_ATTEMPTS",
                        "Đã có học sinh làm bài nên không được đổi đáp án đúng.");
            }
        }

        qe.setText(req.text().trim());
        qe.setExplanation(req.explanation() == null ? null : req.explanation().trim());

        // Đồng bộ options: nếu option có id tồn tại -> cập nhật text; nếu không -> tạo mới.
        Map<Long, AnswerOptionEntity> existing = new HashMap<>();
        for (AnswerOptionEntity o : qe.getOptions()) existing.put(o.getId(), o);
        Set<Long> keep = new HashSet<>();
        int pos = 0;
        for (AdminQuizDtos.OptionInput in : req.options()) {
            AnswerOptionEntity o;
            if (in.id() != null && existing.containsKey(in.id())) {
                o = existing.get(in.id());
            } else {
                o = new AnswerOptionEntity();
                o.setQuestion(qe);
                qe.getOptions().add(o);
            }
            o.setText(in.text().trim());
            o.setCorrect(in.correct());
            o.setPosition(pos++);
            keep.add(o.getId() == null ? -1L : o.getId());
        }
        // Xoá options cũ không còn (chỉ khi không bị lock)
        if (!locked) {
            qe.getOptions().removeIf(o -> o.getId() != null && !keep.contains(o.getId()));
        }
        return toFull(q);
    }

    @Transactional
    public AdminQuizDtos.AdminFull deleteQuestion(Long questionId) {
        QuestionEntity qe = questions.findFullById(questionId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "QUESTION_NOT_FOUND",
                        "Không tìm thấy câu hỏi."));
        qe.getOptions().size();
        QuizEntity q = qe.getQuiz();
        if (attempts.existsByQuizId(q.getId())) {
            throw new BusinessException(HttpStatus.CONFLICT, "QUIZ_HAS_ATTEMPTS",
                    "Đã có học sinh làm bài nên không được xóa câu hỏi.");
        }
        if (q.getStatus() == QuizEntity.Status.PUBLISHED && q.getQuestions().size() <= 1) {
            throw new BusinessException(HttpStatus.CONFLICT, "LAST_QUESTION",
                    "Bài đang xuất bản cần ít nhất 1 câu hỏi. Hãy gỡ xuất bản trước.");
        }
        q.getQuestions().remove(qe);
        // re-number positions
        int i = 0;
        for (QuestionEntity x : q.getQuestions()) x.setPosition(i++);
        return toFull(q);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> attemptsForQuiz(Long quizId) {
        QuizEntity q = quizzes.findById(quizId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "QUIZ_NOT_FOUND",
                        "Không tìm thấy bài kiểm tra."));
        List<AttemptEntity> list = attempts.findByQuizOrdered(quizId);
        List<Map<String, Object>> out = new ArrayList<>();
        for (AttemptEntity a : list) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", a.getId());
            m.put("studentName", a.getUser().getName());
            m.put("studentEmail", a.getUser().getEmail());
            m.put("seq", a.getSeq());
            m.put("score", a.getScore());
            m.put("total", a.getTotal());
            m.put("percent", a.getPercent());
            m.put("passed", a.isPassed());
            m.put("submittedAt", a.getSubmittedAt());
            out.add(m);
        }
        return out;
    }

    @Transactional(readOnly = true)
    public AdminQuizDtos.TeacherStats stats() {
        long pub = quizzes.findAllByStatusOrdered(QuizEntity.Status.PUBLISHED).size();
        long draft = quizzes.findAllByStatusOrdered(QuizEntity.Status.DRAFT).size();
        long att = 0;
        for (QuizEntity q : quizzes.findAll()) att += attempts.countByQuizId(q.getId());
        return new AdminQuizDtos.TeacherStats(pub, draft, att);
    }

    /* ---------------- helpers ---------------- */

    private void apply(QuizEntity q, AdminQuizDtos.QuizUpsertRequest req) {
        q.setTitle(req.title().trim());
        q.setDescription(req.description() == null ? "" : req.description().trim());
        q.setSubject(req.subject().trim());
        q.setDurationMin(req.durationMin());
        q.setMaxAttempts(req.maxAttempts());
        q.setPassPct(req.passPct());
        q.setClosesAt(req.closesAt());
    }

    private void validateBasics(AdminQuizDtos.QuizUpsertRequest req) {
        if (req.closesAt() != null && req.closesAt().isBefore(Instant.now())) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "VALIDATION_FAILED",
                    "Ngày đóng không hợp lệ.",
                    Map.of("closesAt", "Ngày đóng phải ở tương lai."));
        }
    }

    private void validateQuestion(AdminQuizDtos.QuestionUpsertRequest req, QuizEntity q) {
        long correct = req.options().stream().filter(AdminQuizDtos.OptionInput::correct).count();
        if (q.getStatus() == QuizEntity.Status.PUBLISHED && correct != 1) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "VALIDATION_FAILED",
                    "Câu hỏi chưa hợp lệ.",
                    Map.of("correct", "Bài đang xuất bản nên câu hỏi phải có đúng 1 đáp án đúng."));
        }
    }

    private List<AdminQuizDtos.CheckItem> publishChecks(QuizEntity q) {
        List<AdminQuizDtos.CheckItem> out = new ArrayList<>();
        out.add(new AdminQuizDtos.CheckItem("title", "Bài có tên (từ 3 ký tự)",
                q.getTitle() != null && q.getTitle().trim().length() >= 3, null));
        out.add(new AdminQuizDtos.CheckItem("questions", "Có ít nhất 1 câu hỏi",
                !q.getQuestions().isEmpty(), null));
        List<Integer> few = new ArrayList<>();
        List<Integer> noc = new ArrayList<>();
        int i = 1;
        for (QuestionEntity x : q.getQuestions()) {
            if (x.getOptions().size() < 2) few.add(i);
            long correct = x.getOptions().stream().filter(AnswerOptionEntity::isCorrect).count();
            if (correct != 1) noc.add(i);
            i++;
        }
        out.add(new AdminQuizDtos.CheckItem("options", "Mỗi câu có ít nhất 2 đáp án",
                few.isEmpty(), listDetail(few)));
        out.add(new AdminQuizDtos.CheckItem("correct", "Mỗi câu có đúng 1 đáp án đúng",
                noc.isEmpty(), listDetail(noc)));
        out.add(new AdminQuizDtos.CheckItem("deadline", "Hạn đóng bài chưa qua",
                q.getClosesAt() == null || q.getClosesAt().isAfter(Instant.now()), null));
        return out;
    }

    private String listDetail(List<Integer> list) {
        if (list.isEmpty()) return "";
        StringBuilder sb = new StringBuilder("Câu ");
        for (int j = 0; j < list.size(); j++) {
            if (j > 0) sb.append(", ");
            sb.append(list.get(j));
        }
        sb.append(".");
        return sb.toString();
    }

    private AdminQuizDtos.AdminSummary toSummary(QuizEntity q) {
        AdminQuizDtos.AdminSummary s = new AdminQuizDtos.AdminSummary();
        s.id = q.getId();
        s.title = q.getTitle();
        s.subject = q.getSubject();
        s.status = q.getStatus().name();
        s.questionCount = q.getQuestions().size();
        s.attemptCount = attempts.countByQuizId(q.getId());
        Double avg = attempts.avgPercentByQuiz(q.getId());
        s.avgPercent = avg == null ? null : (int) Math.round(avg);
        s.closesAt = q.getClosesAt();
        s.updatedAt = q.getUpdatedAt();
        return s;
    }

    private AdminQuizDtos.AdminFull toFull(QuizEntity q) {
        AdminQuizDtos.AdminFull f = new AdminQuizDtos.AdminFull();
        f.id = q.getId();
        f.title = q.getTitle();
        f.description = q.getDescription();
        f.subject = q.getSubject();
        f.status = q.getStatus().name();
        f.durationMin = q.getDurationMin();
        f.maxAttempts = q.getMaxAttempts();
        f.passPct = q.getPassPct();
        f.closesAt = q.getClosesAt();
        f.updatedAt = q.getUpdatedAt();
        f.attemptCount = attempts.countByQuizId(q.getId());
        f.locked = f.attemptCount > 0;
        f.questions = new ArrayList<>();
        for (QuestionEntity qe : q.getQuestions()) {
            AdminQuizDtos.AdminQuestion aq = new AdminQuizDtos.AdminQuestion();
            aq.id = qe.getId();
            aq.text = qe.getText();
            aq.explanation = qe.getExplanation();
            aq.options = new ArrayList<>();
            for (AnswerOptionEntity o : qe.getOptions()) {
                AdminQuizDtos.AdminOption ao = new AdminQuizDtos.AdminOption();
                ao.id = o.getId();
                ao.text = o.getText();
                ao.correct = o.isCorrect();
                aq.options.add(ao);
            }
            aq.correctId = qe.getOptions().stream().filter(AnswerOptionEntity::isCorrect)
                    .map(AnswerOptionEntity::getId).findFirst().orElse(null);
            f.questions.add(aq);
        }
        f.checks = publishChecks(q);
        return f;
    }
}
