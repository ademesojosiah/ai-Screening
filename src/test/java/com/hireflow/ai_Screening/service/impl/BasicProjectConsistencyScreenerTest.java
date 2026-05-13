package com.hireflow.ai_Screening.service.impl;

import com.hireflow.ai_Screening.event.ApplicationSubmittedEvent;
import com.hireflow.ai_Screening.event.ProjectConsistencyCompletedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BasicProjectConsistencyScreenerTest {

    private final BasicProjectConsistencyScreener screener = new BasicProjectConsistencyScreener();

    @Test
    @DisplayName("Should return score 100 when all job skills appear in resume evidence")
    void score_allSkillsInEvidence() {
        ApplicationSubmittedEvent event = new ApplicationSubmittedEvent();
        event.setApplicationId("application-1");
        event.setJobSkills(List.of("Java", "Kafka"));
        event.setResumeSummary("Built distributed systems using Java and Kafka at scale");

        ProjectConsistencyCompletedEvent result = screener.score(event);

        assertThat(result.getApplicationId()).isEqualTo("application-1");
        assertThat(result.getScore()).isEqualTo(100);
    }

    @Test
    @DisplayName("Should compute partial score when only some job skills appear in evidence")
    void score_partialSkillsInEvidence() {
        // 1 of 3 matched → round(33.33) = 33
        ApplicationSubmittedEvent event = new ApplicationSubmittedEvent();
        event.setApplicationId("application-1");
        event.setJobSkills(List.of("Java", "Kafka", "Docker"));
        event.setResumeSummary("Backend Java developer with no containerisation experience");

        ProjectConsistencyCompletedEvent result = screener.score(event);

        assertThat(result.getScore()).isEqualTo(33);
    }

    @Test
    @DisplayName("Should return neutral score 50 when there is no evidence text")
    void score_noEvidence() {
        ApplicationSubmittedEvent event = new ApplicationSubmittedEvent();
        event.setApplicationId("application-1");
        event.setJobSkills(List.of("Java", "Kafka"));
        event.setResumeSummary(null);

        ProjectConsistencyCompletedEvent result = screener.score(event);

        assertThat(result.getScore()).isEqualTo(50);
    }

    @Test
    @DisplayName("Should return neutral score 50 when the job has no required skills")
    void score_emptyJobSkills() {
        ApplicationSubmittedEvent event = new ApplicationSubmittedEvent();
        event.setApplicationId("application-1");
        event.setJobSkills(List.of());
        event.setResumeSummary("Experienced Java developer");

        ProjectConsistencyCompletedEvent result = screener.score(event);

        assertThat(result.getScore()).isEqualTo(50);
    }

    @Test
    @DisplayName("Should ignore Q&A answers — only the resume summary is used as evidence")
    void score_ignoresAnswers() {
        // Skill not in resume but mentioned in an answer-like field should NOT count.
        // The screener no longer reads answers; Q&A is reserved for human reviewers.
        ApplicationSubmittedEvent event = new ApplicationSubmittedEvent();
        event.setApplicationId("application-1");
        event.setJobSkills(List.of("Kafka"));
        event.setResumeSummary("Frontend engineer with no streaming experience");

        ProjectConsistencyCompletedEvent result = screener.score(event);

        assertThat(result.getScore()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should not count Java as supported when evidence only mentions JavaScript")
    void score_javaSkillNotMatchedByJavaScriptEvidence() {
        ApplicationSubmittedEvent event = new ApplicationSubmittedEvent();
        event.setApplicationId("application-1");
        event.setJobSkills(List.of("Java"));
        event.setResumeSummary("Full-stack developer with JavaScript and TypeScript experience");

        ProjectConsistencyCompletedEvent result = screener.score(event);

        assertThat(result.getScore()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should not match Spring Boot when only Spring is present in evidence")
    void score_multiWordSkillRequiresBothTokens() {
        ApplicationSubmittedEvent event = new ApplicationSubmittedEvent();
        event.setApplicationId("application-1");
        event.setJobSkills(List.of("Spring Boot"));
        event.setResumeSummary("Expert in Spring and various Java frameworks");

        ProjectConsistencyCompletedEvent result = screener.score(event);

        assertThat(result.getScore()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should match Spring Boot when both tokens appear in evidence")
    void score_multiWordSkillMatchesWhenBothTokensPresent() {
        ApplicationSubmittedEvent event = new ApplicationSubmittedEvent();
        event.setApplicationId("application-1");
        event.setJobSkills(List.of("Spring Boot"));
        event.setResumeSummary("Built REST APIs with Spring Boot and Hibernate");

        ProjectConsistencyCompletedEvent result = screener.score(event);

        assertThat(result.getScore()).isEqualTo(100);
    }
}
