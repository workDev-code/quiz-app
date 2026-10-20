package com.anhtduong.quiz.question;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public class QuestionBankDtos {

    public record TagDto(Long id, String name) {}

    public record CreateTagRequest(@NotBlank @Size(max = 60) String name) {}

    /**
     * Item trong ngân hàng câu hỏi (cho giáo viên browse).
     */
    public record BankItem(
            Long id,
            String text,
            String difficulty,
            String type,
            String explanation,
            List<OptionSummary> options,
            List<TagDto> tags,
            int usageCount,
            boolean usedInPublishedQuiz
    ) {}

    public record OptionSummary(Long id, String text, boolean correct) {}
}
