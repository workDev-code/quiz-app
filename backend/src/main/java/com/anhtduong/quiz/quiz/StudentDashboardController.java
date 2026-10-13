package com.anhtduong.quiz.quiz;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StudentDashboardController {

    private final StudentDashboardService service;

    public StudentDashboardController(StudentDashboardService service) { this.service = service; }

    @GetMapping("/api/me/dashboard")
    public StudentDashboardService.Dash dashboard() { return service.get(); }
}
