package com.hireflow.ai_Screening.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProjectConsistencyCompletedEvent {

    private String applicationId;
    private Integer score;
    private String explanation;
    private String review;
}
