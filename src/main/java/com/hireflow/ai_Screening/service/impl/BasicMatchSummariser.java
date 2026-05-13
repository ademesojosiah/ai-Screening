package com.hireflow.ai_Screening.service.impl;

import com.hireflow.ai_Screening.event.ApplicationSubmittedEvent;
import com.hireflow.ai_Screening.event.ScreeningCompletedEvent;
import com.hireflow.ai_Screening.service.MatchSummariser;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

import static com.hireflow.ai_Screening.service.impl.ScreeningAnalysisSupport.normalize;
import static com.hireflow.ai_Screening.service.impl.ScreeningAnalysisSupport.safeList;

@Service
public class BasicMatchSummariser implements MatchSummariser {

    @Override
    public ScreeningCompletedEvent summarise(ApplicationSubmittedEvent event) {
        List<String> jobSkills = safeList(event.getJobSkills());
        Set<String> applicantSkills = normalize(safeList(event.getApplicantSkills()));

        List<String> matched = jobSkills.stream()
                .filter(skill -> applicantSkills.contains(normalize(skill)))
                .toList();
        List<String> unmatched = jobSkills.stream()
                .filter(skill -> !applicantSkills.contains(normalize(skill)))
                .toList();

        int matchPercentage = jobSkills.isEmpty()
                ? 50
                : (int) Math.round((matched.size() * 100.0) / jobSkills.size());
        String summary = "Matched " + matched.size() + " of " + jobSkills.size() + " required job skills.";

        return new ScreeningCompletedEvent(
                event.getApplicationId(),
                matchPercentage,
                matched,
                unmatched,
                summary
        );
    }
}
