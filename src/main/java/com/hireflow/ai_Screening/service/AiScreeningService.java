package com.hireflow.ai_Screening.service;

import com.hireflow.ai_Screening.event.ApplicationSubmittedEvent;
import com.hireflow.ai_Screening.event.ScreeningCompletedEvent;

public interface AiScreeningService {
    ScreeningCompletedEvent screen(ApplicationSubmittedEvent event);
}
