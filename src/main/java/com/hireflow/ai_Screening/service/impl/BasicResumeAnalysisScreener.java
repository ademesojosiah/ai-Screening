package com.hireflow.ai_Screening.service.impl;

import com.hireflow.ai_Screening.event.ApplicationSubmittedEvent;
import com.hireflow.ai_Screening.event.ResumeAnalysisCompletedEvent;
import com.hireflow.ai_Screening.service.ResumeAnalysisScreener;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

import static com.hireflow.ai_Screening.service.impl.ScreeningAnalysisSupport.normalize;
import static com.hireflow.ai_Screening.service.impl.ScreeningAnalysisSupport.safeList;

@Service
public class BasicResumeAnalysisScreener implements ResumeAnalysisScreener {

    @Override
    public ResumeAnalysisCompletedEvent analyze(ApplicationSubmittedEvent event) {
        List<String> jobSkills = safeList(event.getJobSkills());
        Set<String> applicantSkills = normalize(safeList(event.getApplicantSkills()));

        long matched = jobSkills.stream()
                .filter(skill -> applicantSkills.contains(normalize(skill)))
                .count();
        long missed = jobSkills.size() - matched;

        int score = jobSkills.isEmpty() ? 50 : (int) Math.round((matched * 100.0) / jobSkills.size());
        String explanation = "Matched " + matched + " of " + jobSkills.size() + " required job skills.";
        String review = resumeReview(jobSkills, matched, missed);

        return new ResumeAnalysisCompletedEvent(event.getApplicationId(), score, explanation, review);
    }

    private String resumeReview(List<String> jobSkills, long matched, long missed) {
        if (jobSkills.isEmpty()) {
            return "No required job skills were provided, so the resume analysis stayed neutral.";
        }
        if (missed == 0) {
            return "Resume/profile skills align with all required job skills.";
        }
        return "Resume/profile skills match " + matched + " required skills and miss " + missed + ".";
    }
}
