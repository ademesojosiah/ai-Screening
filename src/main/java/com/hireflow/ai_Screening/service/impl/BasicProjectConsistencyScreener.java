package com.hireflow.ai_Screening.service.impl;

import com.hireflow.ai_Screening.event.ApplicationSubmittedEvent;
import com.hireflow.ai_Screening.event.ProjectConsistencyCompletedEvent;
import com.hireflow.ai_Screening.service.ProjectConsistencyScreener;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.hireflow.ai_Screening.service.impl.ScreeningAnalysisSupport.evidenceSupportsSkill;
import static com.hireflow.ai_Screening.service.impl.ScreeningAnalysisSupport.isBlank;
import static com.hireflow.ai_Screening.service.impl.ScreeningAnalysisSupport.safeList;

@Service
public class BasicProjectConsistencyScreener implements ProjectConsistencyScreener {

    @Override
    public ProjectConsistencyCompletedEvent score(ApplicationSubmittedEvent event) {
        List<String> jobSkills = safeList(event.getJobSkills());
        String evidence = event.getResumeSummary();

        int score = computeScore(jobSkills, evidence);
        String explanation = explanation(jobSkills, evidence);
        String review = review(score);

        return new ProjectConsistencyCompletedEvent(event.getApplicationId(), score, explanation, review);
    }

    private int computeScore(List<String> jobSkills, String evidence) {
        if (jobSkills.isEmpty() || isBlank(evidence)) {
            return 50;
        }
        long supported = jobSkills.stream().filter(skill -> evidenceSupportsSkill(evidence, skill)).count();
        return (int) Math.round((supported * 100.0) / jobSkills.size());
    }

    private String explanation(List<String> jobSkills, String evidence) {
        if (jobSkills.isEmpty()) {
            return "No required job skills were provided for project consistency comparison.";
        }
        if (isBlank(evidence)) {
            return "No resume summary is available; project consistency is neutral.";
        }
        return "Project consistency is approximated from the resume summary evidence.";
    }

    private String review(int score) {
        if (score >= 70) {
            return "Resume evidence appears aligned with the target role.";
        }
        if (score >= 40) {
            return "Resume evidence needs human review before it affects ranking.";
        }
        return "Resume evidence is weak or missing for several required skills.";
    }
}
