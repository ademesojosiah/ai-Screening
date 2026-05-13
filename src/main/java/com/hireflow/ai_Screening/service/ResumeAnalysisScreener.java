package com.hireflow.ai_Screening.service;

import com.hireflow.ai_Screening.event.ApplicationSubmittedEvent;
import com.hireflow.ai_Screening.event.ResumeAnalysisCompletedEvent;

public interface ResumeAnalysisScreener {
    ResumeAnalysisCompletedEvent analyze(ApplicationSubmittedEvent event);
}
