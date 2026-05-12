package com.hireflow.ai_Screening.service;

import com.hireflow.ai_Screening.event.ApplicationSubmittedEvent;
import com.hireflow.ai_Screening.event.ScreeningCompletedEvent;
import com.hireflow.ai_Screening.service.impl.BasicAiScreeningServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BasicAiScreeningServiceImplTest {

    private final BasicAiScreeningServiceImpl aiScreeningService = new BasicAiScreeningServiceImpl();

    @Test
    @DisplayName("Should calculate match score and split matched and unmatched skills case-insensitively")
    void screen_partialMatch() {
        ApplicationSubmittedEvent event = event(
                List.of("Java", "Kafka", "Spring Boot"),
                List.of(" java ", "SPRING BOOT", "React")
        );

        ScreeningCompletedEvent result = aiScreeningService.screen(event);

        assertThat(result.getApplicationId()).isEqualTo("application-1");
        assertThat(result.getMatchPercentage()).isEqualTo(67);
        assertThat(result.getMatchedSkills()).containsExactly("Java", "Spring Boot");
        assertThat(result.getUnmatchedSkills()).containsExactly("Kafka");
        assertThat(result.getAiNarrativeSummary()).isEqualTo("Matched 2 of 3 required job skills.");
    }

    @Test
    @DisplayName("Should return neutral score when the job has no required skills")
    void screen_noJobSkills() {
        ApplicationSubmittedEvent event = event(List.of(), List.of("Java"));

        ScreeningCompletedEvent result = aiScreeningService.screen(event);

        assertThat(result.getMatchPercentage()).isEqualTo(50);
        assertThat(result.getMatchedSkills()).isEmpty();
        assertThat(result.getUnmatchedSkills()).isEmpty();
        assertThat(result.getAiNarrativeSummary()).isEqualTo("Matched 0 of 0 required job skills.");
    }

    @Test
    @DisplayName("Should handle null skill lists as empty lists")
    void screen_nullSkillLists() {
        ApplicationSubmittedEvent event = event(null, null);

        ScreeningCompletedEvent result = aiScreeningService.screen(event);

        assertThat(result.getMatchPercentage()).isEqualTo(50);
        assertThat(result.getMatchedSkills()).isEmpty();
        assertThat(result.getUnmatchedSkills()).isEmpty();
    }

    @Test
    @DisplayName("Should score zero when none of the required skills match")
    void screen_noMatches() {
        ApplicationSubmittedEvent event = event(List.of("Kafka", "MySQL"), List.of("React", "Figma"));

        ScreeningCompletedEvent result = aiScreeningService.screen(event);

        assertThat(result.getMatchPercentage()).isZero();
        assertThat(result.getMatchedSkills()).isEmpty();
        assertThat(result.getUnmatchedSkills()).containsExactly("Kafka", "MySQL");
    }

    private ApplicationSubmittedEvent event(List<String> jobSkills, List<String> applicantSkills) {
        return new ApplicationSubmittedEvent(
                "application-1",
                "job-1",
                "Backend Engineer",
                "Build APIs",
                "Java and Kafka",
                "Cloud experience",
                "applicant-1",
                "ada@example.com",
                "Backend engineer",
                "https://cdn.example.com/resume.pdf",
                jobSkills,
                applicantSkills,
                40,
                75
        );
    }
}
