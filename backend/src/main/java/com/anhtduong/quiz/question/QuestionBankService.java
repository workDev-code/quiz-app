package com.anhtduong.quiz.question;

import com.anhtduong.quiz.common.BusinessException;
import com.anhtduong.quiz.quiz.QuizEntity;
import com.anhtduong.quiz.quiz.QuizRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Ngân hàng câu hỏi: câu hỏi tồn tại độc lập, đề thi chỉ "mượn" qua bảng quiz_questions.
 */
@Service
public class QuestionBankService {

    private final QuestionRepository questions;
    private final QuizQuestionRepository quizQuestions;
    private final QuestionTagRepository questionTags;
    private final TagRepository tags;
    private final QuizRepository quizzes;

    public QuestionBankService(QuestionRepository questions,
                               QuizQuestionRepository quizQuestions,
                               QuestionTagRepository questionTags,
                               TagRepository tags,
                               QuizRepository quizzes) {
        this.questions = questions;
        this.quizQuestions = quizQuestions;
        this.questionTags = questionTags;
        this.tags = tags;
        this.quizzes = quizzes;
    }

    @Transactional(readOnly = true)
    public List<QuestionBankDtos.BankItem> searchBank(String search, String difficulty, Long tagId) {
        List<QuestionEntity> qs = questions.searchBank(normalize(search), normalize(difficulty), tagId);
        if (qs.isEmpty()) return List.of();

        List<Long> qids = qs.stream().map(QuestionEntity::getId).toList();

        // 3 query cho N câu, thay vì N query.
        Map<Long, List<QuestionBankDtos.TagDto>> tagsByQid = loadTagsForQuestions(qids);
        Map<Long, Usage> usageByQid = loadUsage(qids);

        List<QuestionBankDtos.BankItem> result = new ArrayList<>(qs.size());
        for (QuestionEntity q : qs) {
            List<QuestionBankDtos.OptionSummary> options = q.getOptions().stream()
                    .sorted(Comparator.comparingInt(AnswerOptionEntity::getPosition))
                    .map(o -> new QuestionBankDtos.OptionSummary(o.getId(), o.getText(), o.isCorrect()))
                    .toList();
            Usage usage = usageByQid.getOrDefault(q.getId(), Usage.EMPTY);
            result.add(new QuestionBankDtos.BankItem(
                    q.getId(),
                    q.getText(),
                    q.getDifficulty(),
                    q.getType(),
                    q.getExplanation(),
                    options,
                    tagsByQid.getOrDefault(q.getId(), List.of()),
                    usage.usageCount,
                    usage.publishedCount > 0));
        }
        return result;
    }

    /** Gộp tag theo từng câu: lấy mapping, rồi lấy tag theo id (tránh N+1). */
    private Map<Long, List<QuestionBankDtos.TagDto>> loadTagsForQuestions(List<Long> qids) {
        if (qids.isEmpty()) return Map.of();

        List<QuestionTagEntity> mappings = questionTags.findByQuestionIds(qids);
        if (mappings.isEmpty()) return Map.of();

        Set<Long> tagIds = new LinkedHashSet<>();
        mappings.forEach(m -> tagIds.add(m.getTagId()));
        Map<Long, String> nameById = new HashMap<>();
        tags.findAllById(tagIds).forEach(t -> nameById.put(t.getId(), t.getName()));

        Map<Long, List<QuestionBankDtos.TagDto>> result = new HashMap<>();
        for (QuestionTagEntity m : mappings) {
            String name = nameById.get(m.getTagId());
            if (name == null) continue;
            result.computeIfAbsent(m.getQuestionId(), k -> new ArrayList<>())
                    .add(new QuestionBankDtos.TagDto(m.getTagId(), name));
        }
        result.values().forEach(l -> l.sort(Comparator.comparing(QuestionBankDtos.TagDto::name)));
        return result;
    }

    private Map<Long, Usage> loadUsage(List<Long> qids) {
        if (qids.isEmpty()) return Map.of();
        Map<Long, Usage> result = new HashMap<>();
        for (Object[] row : quizQuestions.countUsageByQuestionIds(qids)) {
            Long qid = toLong(row[0]);
            if (qid == null) continue;
            result.put(qid, new Usage(toInt(row[1]), toInt(row[2])));
        }
        return result;
    }

    private static Long toLong(Object o) {
        return o instanceof Number n ? n.longValue() : null;
    }

    private static int toInt(Object o) {
        return o instanceof Number n ? n.intValue() : 0;
    }

    private static String normalize(String v) {
        return (v == null || v.isBlank()) ? null : v.trim();
    }

    private record Usage(int usageCount, int publishedCount) {
        static final Usage EMPTY = new Usage(0, 0);
    }

    /* --------------- Tags --------------- */

    @Transactional(readOnly = true)
    public List<QuestionBankDtos.TagDto> listTags() {
        return tags.findAllByOrderByNameAsc().stream()
                .map(t -> new QuestionBankDtos.TagDto(t.getId(), t.getName()))
                .toList();
    }

    @Transactional
    public QuestionBankDtos.TagDto createTag(String name) {
        String n = name.trim();
        if (tags.existsByName(n)) {
            throw new BusinessException(HttpStatus.CONFLICT, "TAG_EXISTS", "Tag này đã tồn tại.");
        }
        TagEntity t = new TagEntity();
        t.setName(n);
        t = tags.save(t);
        return new QuestionBankDtos.TagDto(t.getId(), t.getName());
    }

    @Transactional
    public void deleteTag(Long tagId) {
        if (!tags.existsById(tagId)) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "TAG_NOT_FOUND", "Không tìm thấy tag.");
        }
        questionTags.deleteByTagId(tagId);
        tags.deleteById(tagId);
    }

    /* --------------- Link câu hỏi vào đề --------------- */

    /**
     * Mượn 1 câu hỏi đã có trong ngân hàng vào 1 đề (thêm vào cuối).
     * Bản thân câu hỏi không bị copy — chỉ tạo dòng trong quiz_questions.
     */
    @Transactional
    public void linkQuestionToQuiz(Long quizId, Long questionId) {
        QuizEntity quiz = quizzes.findById(quizId).orElseThrow(() ->
                new BusinessException(HttpStatus.NOT_FOUND, "QUIZ_NOT_FOUND", "Không tìm thấy bài."));
        if (quiz.getStatus() == QuizEntity.Status.PUBLISHED) {
            throw new BusinessException(HttpStatus.CONFLICT, "QUIZ_PUBLISHED",
                    "Bài đang xuất bản. Hãy gỡ xuất bản trước khi thêm câu hỏi.");
        }
        if (!questions.existsById(questionId)) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "QUESTION_NOT_FOUND",
                    "Câu hỏi không tồn tại trong ngân hàng.");
        }
        if (quizQuestions.existsByQuizIdAndQuestionId(quizId, questionId)) {
            throw new BusinessException(HttpStatus.CONFLICT, "ALREADY_LINKED",
                    "Câu hỏi này đã có trong bài rồi.");
        }
        Integer max = quizQuestions.maxPosition(quizId);
        int pos = (max == null) ? 0 : max + 1;

        QuizQuestionEntity qq = new QuizQuestionEntity();
        qq.setQuizId(quizId);
        qq.setQuestionId(questionId);
        qq.setPosition(pos);
        quizQuestions.save(qq);
    }

    /**
     * Gỡ câu hỏi khỏi đề (không xoá câu hỏi khỏi ngân hàng).
     */
    @Transactional
    public void unlinkQuestionFromQuiz(Long quizId, Long questionId) {
        if (!quizQuestions.existsByQuizIdAndQuestionId(quizId, questionId)) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "LINK_NOT_FOUND",
                    "Câu hỏi này không có trong bài.");
        }
        quizQuestions.unlink(quizId, questionId);
    }
}
