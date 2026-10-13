package com.anhtduong.quiz.attempt;
public record AttemptDtos_Brief(Long id, Long quizId, String quizTitle, Integer seq, Integer score,
                                Integer total, Integer percent, boolean passed, String submittedAt) {}
