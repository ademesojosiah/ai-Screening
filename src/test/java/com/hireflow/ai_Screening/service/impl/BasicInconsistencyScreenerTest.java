package com.hireflow.ai_Screening.service.impl;

import com.hireflow.ai_Screening.event.ApplicationSubmittedEvent;
import com.hireflow.ai_Screening.event.InconsistencyReviewCompletedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BasicInconsistencyScreenerTest {

    private final BasicInconsistencyScreener screener = new BasicInconsistencyScreener();

    @Test
    @DisplayName("Should return score 0 and LOW severity when all claimed skills are supported by the resume")
    void detect_allSkillsSupportedByResume() {
        ApplicationSubmittedEvent event = new ApplicationSubmittedEvent();
        event.setApplicationId("application-1");
        event.setApplicantSkills(List.of("Java", "Kafka"));
        event.setResumeSummary("Backend engineer with Java microservices and Kafka streaming experience");

        InconsistencyReviewCompletedEvent result = screener.detect(event);

        assertThat(result.getScore()).isEqualTo(0);
        assertThat(result.getSeverity()).isEqualTo("LOW");
    }

    @Test
    @DisplayName("Should return HIGH severity when most claimed skills are absent from the resume")
    void detect_mostSkillsUnsupportedReturnsHigh() {
        // 3 of 4 unsupported → round(75) = 75 → HIGH
        ApplicationSubmittedEvent event = new ApplicationSubmittedEvent();
        event.setApplicationId("application-1");
        event.setApplicantSkills(List.of("Java", "Kubernetes", "Terraform", "AWS"));
        event.setResumeSummary("Junior Java developer");

        InconsistencyReviewCompletedEvent result = screener.detect(event);

        assertThat(result.getScore()).isEqualTo(75);
        assertThat(result.getSeverity()).isEqualTo("HIGH");
    }

    @Test
    @DisplayName("Should return MEDIUM severity when half of claimed skills are absent from the resume")
    void detect_halfUnsupportedSkillsIsMedium() {
        // 1 of 2 unsupported → 50 → MEDIUM
        ApplicationSubmittedEvent event = new ApplicationSubmittedEvent();
        event.setApplicationId("application-1");
        event.setApplicantSkills(List.of("Java", "Kubernetes"));
        event.setResumeSummary("Java developer with no cloud infrastructure experience");

        InconsistencyReviewCompletedEvent result = screener.detect(event);

        assertThat(result.getScore()).isEqualTo(50);
        assertThat(result.getSeverity()).isEqualTo("MEDIUM");
    }

    @Test
    @DisplayName("Should return score 0 and LOW severity when there are no claimed skills")
    void detect_noSkillsReturnsLow() {
        ApplicationSubmittedEvent event = new ApplicationSubmittedEvent();
        event.setApplicationId("application-1");
        event.setApplicantSkills(List.of());
        event.setResumeSummary("Some resume text");

        InconsistencyReviewCompletedEvent result = screener.detect(event);

        assertThat(result.getScore()).isEqualTo(0);
        assertThat(result.getSeverity()).isEqualTo("LOW");
    }

    @Test
    @DisplayName("Should return score 0 and LOW severity when the resume is missing — no claim can be challenged")
    void detect_missingResumeReturnsLow() {
        ApplicationSubmittedEvent event = new ApplicationSubmittedEvent();
        event.setApplicationId("application-1");
        event.setApplicantSkills(List.of("Java", "Kafka"));
        event.setResumeSummary(null);

        InconsistencyReviewCompletedEvent result = screener.detect(event);

        assertThat(result.getScore()).isEqualTo(0);
        assertThat(result.getSeverity()).isEqualTo("LOW");
    }

    @Test
    @DisplayName("Should not count Java as supported when the resume only mentions JavaScript")
    void detect_javaSkillNotSupportedByJavaScriptResume() {
        ApplicationSubmittedEvent event = new ApplicationSubmittedEvent();
        event.setApplicationId("application-1");
        event.setApplicantSkills(List.of("Java"));
        event.setResumeSummary("Full-stack developer with JavaScript and TypeScript");

        InconsistencyReviewCompletedEvent result = screener.detect(event);

        assertThat(result.getScore()).isEqualTo(100);
        assertThat(result.getSeverity()).isEqualTo("HIGH");
    }
}
