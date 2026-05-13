package com.hireflow.ai_Screening.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InconsistencyReviewCompletedEvent {

    private String applicationId;
    private Integer score;
    private String severity;
    private String explanation;
    private String review;
    private String recommendedHumanReviewAction;
}
