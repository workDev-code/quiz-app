package com.anhtduong.quiz.attempt;
import java.util.List;
import java.util.Map;
public record AttemptDtos_Submit(String clientKey, String startedAt, List<Map<String, Object>> answers) {}
