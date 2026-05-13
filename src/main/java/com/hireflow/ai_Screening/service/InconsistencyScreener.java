package com.hireflow.ai_Screening.service;

import com.hireflow.ai_Screening.event.ApplicationSubmittedEvent;
import com.hireflow.ai_Screening.event.InconsistencyReviewCompletedEvent;

public interface InconsistencyScreener {
    InconsistencyReviewCompletedEvent detect(ApplicationSubmittedEvent event);
}
