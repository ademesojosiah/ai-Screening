package com.hireflow.ai_Screening.service;

import com.hireflow.ai_Screening.event.ApplicationSubmittedEvent;
import com.hireflow.ai_Screening.event.ProjectConsistencyCompletedEvent;

public interface ProjectConsistencyScreener {
    ProjectConsistencyCompletedEvent score(ApplicationSubmittedEvent event);
}
