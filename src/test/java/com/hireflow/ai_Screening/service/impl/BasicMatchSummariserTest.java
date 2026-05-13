package com.hireflow.ai_Screening.service.impl;

import com.hireflow.ai_Screening.event.ApplicationSubmittedEvent;
import com.hireflow.ai_Screening.event.ScreeningCompletedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BasicMatchSummariserTest {

    private final BasicMatchSummariser summariser = new BasicMatchSummariser();

    @Test
    @DisplayName("Should split matched and unmatched skills case-insensitively and compute match percentage")
    void summarise_partialMatch() {
        ApplicationSubmittedEvent event = event(
                List.of("Java", "Kafka", "Spring Boot"),
                List.of(" java ", "SPRING BOOT", "React")
        );

        ScreeningCompletedEvent result = summariser.summarise(event);

        assertThat(result.getApplicationId()).isEqualTo("application-1");
        assertThat(result.getMatchPercentage()).isEqualTo(67);
        assertThat(result.getMatchedSkills()).containsExactly("Java", "Spring Boot");
        assertThat(result.getUnmatchedSkills()).containsExactly("Kafka");
        assertThat(result.getAiNarrativeSummary()).isEqualTo("Matched 2 of 3 required job skills.");
    }

    @Test
    @DisplayName("Should return neutral score when the job has no required skills")
    void summarise_noJobSkills() {
        ScreeningCompletedEvent result = summariser.summarise(event(List.of(), List.of("Java")));

        assertThat(result.getMatchPercentage()).isEqualTo(50);
        assertThat(result.getMatchedSkills()).isEmpty();
        assertThat(result.getUnmatchedSkills()).isEmpty();
    }

    @Test
    @DisplayName("Should treat null skill lists as empty lists")
    void summarise_nullSkillLists() {
        ScreeningCompletedEvent result = summariser.summarise(event(null, null));

        assertThat(result.getMatchPercentage()).isEqualTo(50);
        assertThat(result.getMatchedSkills()).isEmpty();
        assertThat(result.getUnmatchedSkills()).isEmpty();
    }

    private ApplicationSubmittedEvent event(List<String> jobSkills, List<String> applicantSkills) {
        ApplicationSubmittedEvent event = new ApplicationSubmittedEvent();
        event.setApplicationId("application-1");
        event.setJobSkills(jobSkills);
        event.setApplicantSkills(applicantSkills);
        event.setResumeSummary("Backend engineer with Java experience");
        return event;
    }
}
