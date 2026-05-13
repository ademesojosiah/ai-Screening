package com.hireflow.ai_Screening.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ScreeningStageFailedEvent {

    private String applicationId;
    // RESUME_ANALYSIS | PROJECT_CONSISTENCY | INCONSISTENCY_REVIEW | MATCH_SUMMARY
    private String stage;
    private String failureReason;
    private Instant failedAt;
}
