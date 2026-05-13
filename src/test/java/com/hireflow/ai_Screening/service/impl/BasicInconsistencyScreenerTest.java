package com.hireflow.ai_Screening.service.impl;

import com.hireflow.ai_Screening.event.ApplicationSubmittedAnswer;
import com.hireflow.ai_Screening.event.ApplicationSubmittedEvent;
import com.hireflow.ai_Screening.event.InconsistencyReviewCompletedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BasicInconsistencyScreenerTest {

    private final BasicInconsistencyScreener screener = new BasicInconsistencyScreener();

    @Test
    @DisplayName("Should return score 0 and LOW severity when all claimed skills are supported by evidence")
    void detect_allSkillsSupportedByEvidence() {
        ApplicationSubmittedEvent event = new ApplicationSubmittedEvent();
        event.setApplicationId("application-1");
        event.setApplicantSkills(List.of("Java", "Kafka"));
        event.setResumeSummary("Backend engineer with Java microservices and Kafka streaming experience");

        InconsistencyReviewCompletedEvent result = screener.detect(event);

        assertThat(result.getScore()).isEqualTo(0);
        assertThat(result.getSeverity()).isEqualTo("LOW");
    }

    @Test
    @DisplayName("Should return HIGH severity when most claimed skills are absent from evidence")
    void detect_unsupportedSkillsRaiseScoreToHigh() {
        // 3 of 4 unsupported → skillRisk = round(75) = 75 → HIGH
        ApplicationSubmittedEvent event = new ApplicationSubmittedEvent();
        event.setApplicationId("application-1");
        event.setApplicantSkills(List.of("Java", "Kubernetes", "Terraform", "AWS"));
        event.setResumeSummary("Junior Java developer");

        InconsistencyReviewCompletedEvent result = screener.detect(event);

        assertThat(result.getScore()).isEqualTo(75);
        assertThat(result.getSeverity()).isEqualTo("HIGH");
    }

    @Test
    @DisplayName("Should return MEDIUM severity when half of claimed skills are absent from evidence")
    void detect_halfUnsupportedSkillsIsMedium() {
        // 1 of 2 unsupported → skillRisk = 50, no answers → score = 50 → MEDIUM
        ApplicationSubmittedEvent event = new ApplicationSubmittedEvent();
        event.setApplicationId("application-1");
        event.setApplicantSkills(List.of("Java", "Kubernetes"));
        event.setResumeSummary("Java developer with no cloud infrastructure experience");

        InconsistencyReviewCompletedEvent result = screener.detect(event);

        assertThat(result.getScore()).isEqualTo(50);
        assertThat(result.getSeverity()).isEqualTo("MEDIUM");
    }

    @Test
    @DisplayName("Should return score 0 and LOW severity when there are no skills or answers")
    void detect_noInputsReturnsLow() {
        ApplicationSubmittedEvent event = new ApplicationSubmittedEvent();
        event.setApplicationId("application-1");
        event.setApplicantSkills(List.of());
        event.setResumeSummary(null);
        event.setAnswers(List.of());

        InconsistencyReviewCompletedEvent result = screener.detect(event);

        assertThat(result.getScore()).isEqualTo(0);
        assertThat(result.getSeverity()).isEqualTo("LOW");
    }

    @Test
    @DisplayName("Should flag a short answer (under 20 chars) as weak and raise risk score")
    void detect_shortAnswerIsWeak() {
        // skillRisk=0 (no skills), answerRisk=100 (1 of 1 weak), combined = 0*0.6 + 100*0.4 = 40 → MEDIUM
        ApplicationSubmittedAnswer shortAnswer = new ApplicationSubmittedAnswer(
                "q1", "Explain your Java experience", "Describe production use", "Yes I used it"
        );
        ApplicationSubmittedEvent event = new ApplicationSubmittedEvent();
        event.setApplicationId("application-1");
        event.setApplicantSkills(List.of());
        event.setResumeSummary(null);
        event.setAnswers(List.of(shortAnswer));

        InconsistencyReviewCompletedEvent result = screener.detect(event);

        assertThat(result.getScore()).isEqualTo(40);
        assertThat(result.getSeverity()).isEqualTo("MEDIUM");
    }

    @Test
    @DisplayName("Should flag an answer whose token overlap with the guide is below 20%")
    void detect_lowTokenOverlapAnswerIsWeak() {
        // Guide has technical tokens not mentioned by applicant → < 20% overlap → weak
        ApplicationSubmittedAnswer weakAnswer = new ApplicationSubmittedAnswer(
                "q1", "Explain concurrency in Java",
                "Mention synchronized, locks, thread pools, executor service, volatile",
                "I know about concurrency and parallelism from my university course on programming"
        );
        ApplicationSubmittedEvent event = new ApplicationSubmittedEvent();
        event.setApplicationId("application-1");
        event.setApplicantSkills(List.of());
        event.setResumeSummary(null);
        event.setAnswers(List.of(weakAnswer));

        InconsistencyReviewCompletedEvent result = screener.detect(event);

        assertThat(result.getSeverity()).isNotEqualTo("LOW");
    }

    @Test
    @DisplayName("Should accept a strong answer whose token overlap with the guide is at least 20%")
    void detect_strongAnswerPassesThreshold() {
        // Guide tokens: {mention, thread, pools, executor, service} — applicant covers thread, pools, executor, service
        // overlap = 4/5 = 80% → not weak → answerRisk = 0 → score = 0
        ApplicationSubmittedAnswer strongAnswer = new ApplicationSubmittedAnswer(
                "q1", "Explain concurrency in Java",
                "Mention thread pools and executor service",
                "In Java I use thread pools via the executor service framework to manage concurrent tasks safely"
        );
        ApplicationSubmittedEvent event = new ApplicationSubmittedEvent();
        event.setApplicationId("application-1");
        event.setApplicantSkills(List.of());
        event.setResumeSummary(null);
        event.setAnswers(List.of(strongAnswer));

        InconsistencyReviewCompletedEvent result = screener.detect(event);

        assertThat(result.getScore()).isEqualTo(0);
        assertThat(result.getSeverity()).isEqualTo("LOW");
    }

    @Test
    @DisplayName("Should not count Java as supported when evidence only mentions JavaScript")
    void detect_javaSkillNotSupportedByJavaScriptEvidence() {
        // With token matching: "java" token is not in evidence tokens → unsupported → skillRisk = 100
        ApplicationSubmittedEvent event = new ApplicationSubmittedEvent();
        event.setApplicationId("application-1");
        event.setApplicantSkills(List.of("Java"));
        event.setResumeSummary("Full-stack developer with JavaScript and TypeScript");

        InconsistencyReviewCompletedEvent result = screener.detect(event);

        assertThat(result.getScore()).isEqualTo(100);
        assertThat(result.getSeverity()).isEqualTo("HIGH");
    }

    @Test
    @DisplayName("Should combine skill risk and answer risk with 60/40 weighting")
    void detect_combinedScoringFormula() {
        // skillRisk = 100 (Java not in evidence), answerRisk = 100 (short answer)
        // combined = round(100*0.6 + 100*0.4) = 100 → HIGH
        ApplicationSubmittedAnswer shortAnswer = new ApplicationSubmittedAnswer(
                "q1", "Describe a project", "Detail a real system", "Did a project"
        );
        ApplicationSubmittedEvent event = new ApplicationSubmittedEvent();
        event.setApplicationId("application-1");
        event.setApplicantSkills(List.of("Java"));
        event.setResumeSummary("JavaScript frontend developer");
        event.setAnswers(List.of(shortAnswer));

        InconsistencyReviewCompletedEvent result = screener.detect(event);

        assertThat(result.getScore()).isEqualTo(100);
        assertThat(result.getSeverity()).isEqualTo("HIGH");
    }
}
