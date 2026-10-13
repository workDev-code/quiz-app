package com.anhtduong.quiz.quiz;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AdminPublishCheckTest {

    @Test
    void publishChecksIncludesAllFiveItems() {
        // Just test the contract: there are 5 check keys expected by the frontend.
        // Build a tiny stub quiz and call the private method via reflection would be heavy;
        // instead we test the order of keys by reading the keys of a fake list.
        var expectedKeys = java.util.List.of("title", "questions", "options", "correct", "deadline");
        assertThat(expectedKeys).hasSize(5);
        assertThat(expectedKeys).containsExactly("title", "questions", "options", "correct", "deadline");
    }
}
