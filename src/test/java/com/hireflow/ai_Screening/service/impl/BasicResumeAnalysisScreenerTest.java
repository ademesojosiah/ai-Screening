package com.hireflow.ai_Screening.service.impl;

import com.hireflow.ai_Screening.event.ApplicationSubmittedEvent;
import com.hireflow.ai_Screening.event.ResumeAnalysisCompletedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BasicResumeAnalysisScreenerTest {

    private final BasicResumeAnalysisScreener screener = new BasicResumeAnalysisScreener();

    @Test
    @DisplayName("Should return score 100 when every job skill is found in applicant skills")
    void analyze_allSkillsMatched() {
        ApplicationSubmittedEvent event = event(
                List.of("Java", "Spring Boot"),
                List.of("Java", "Spring Boot", "Docker")
        );

        ResumeAnalysisCompletedEvent result = screener.analyze(event);

        assertThat(result.getApplicationId()).isEqualTo("application-1");
        assertThat(result.getScore()).isEqualTo(100);
        assertThat(result.getExplanation()).contains("Matched 2 of 2");
        assertThat(result.getReview()).contains("align with all required");
    }

    @Test
    @DisplayName("Should compute the correct score for a partial skill match")
    void analyze_partialMatch() {
        // 2 of 3 matched → round(66.67) = 67
        ApplicationSubmittedEvent event = event(
                List.of("Java", "Kafka", "Docker"),
                List.of("Java", "Docker")
        );

        ResumeAnalysisCompletedEvent result = screener.analyze(event);

        assertThat(result.getScore()).isEqualTo(67);
        assertThat(result.getExplanation()).contains("Matched 2 of 3");
        assertThat(result.getReview()).contains("match 2");
    }

    @Test
    @DisplayName("Should return score 0 when none of the job skills appear in applicant skills")
    void analyze_noMatch() {
        ApplicationSubmittedEvent event = event(
                List.of("Java", "Kafka"),
                List.of("React", "TypeScript")
        );

        ResumeAnalysisCompletedEvent result = screener.analyze(event);

        assertThat(result.getScore()).isEqualTo(0);
        assertThat(result.getExplanation()).contains("Matched 0 of 2");
    }

    @Test
    @DisplayName("Should return neutral score 50 when the job has no required skills")
    void analyze_emptyJobSkills() {
        ResumeAnalysisCompletedEvent result = screener.analyze(event(List.of(), List.of("Java")));

        assertThat(result.getScore()).isEqualTo(50);
    }

    @Test
    @DisplayName("Should treat null skill lists as empty and return neutral score 50")
    void analyze_nullSkillLists() {
        ResumeAnalysisCompletedEvent result = screener.analyze(event(null, null));

        assertThat(result.getScore()).isEqualTo(50);
    }

    @Test
    @DisplayName("Should match skills case-insensitively and ignore surrounding whitespace")
    void analyze_caseInsensitiveAndTrimmed() {
        ApplicationSubmittedEvent event = event(
                List.of("Java", "Spring Boot"),
                List.of(" JAVA ", "spring boot")
        );

        ResumeAnalysisCompletedEvent result = screener.analyze(event);

        assertThat(result.getScore()).isEqualTo(100);
    }

    private ApplicationSubmittedEvent event(List<String> jobSkills, List<String> applicantSkills) {
        ApplicationSubmittedEvent event = new ApplicationSubmittedEvent();
        event.setApplicationId("application-1");
        event.setJobSkills(jobSkills);
        event.setApplicantSkills(applicantSkills);
        return event;
    }
}
