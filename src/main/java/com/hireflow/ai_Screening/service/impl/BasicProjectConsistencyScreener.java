package com.hireflow.ai_Screening.service.impl;

import com.hireflow.ai_Screening.event.ApplicationSubmittedAnswer;
import com.hireflow.ai_Screening.event.ApplicationSubmittedEvent;
import com.hireflow.ai_Screening.event.ProjectConsistencyCompletedEvent;
import com.hireflow.ai_Screening.service.ProjectConsistencyScreener;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.hireflow.ai_Screening.service.impl.ScreeningAnalysisSupport.combineEvidence;
import static com.hireflow.ai_Screening.service.impl.ScreeningAnalysisSupport.containsNormalized;
import static com.hireflow.ai_Screening.service.impl.ScreeningAnalysisSupport.isBlank;
import static com.hireflow.ai_Screening.service.impl.ScreeningAnalysisSupport.safeList;

@Service
public class BasicProjectConsistencyScreener implements ProjectConsistencyScreener {

    @Override
    public ProjectConsistencyCompletedEvent score(ApplicationSubmittedEvent event) {
        List<String> jobSkills = safeList(event.getJobSkills());
        List<ApplicationSubmittedAnswer> answers = safeList(event.getAnswers());
        String evidence = combineEvidence(event.getResumeSummary(), answers);

        int score = computeScore(jobSkills, evidence);
        String explanation = explanation(jobSkills, evidence, answers);
        String review = review(score);

        return new ProjectConsistencyCompletedEvent(event.getApplicationId(), score, explanation, review);
    }

    private int computeScore(List<String> jobSkills, String evidence) {
        if (jobSkills.isEmpty() || isBlank(evidence)) {
            return 50;
        }
        long supported = jobSkills.stream().filter(skill -> containsNormalized(evidence, skill)).count();
        return (int) Math.round((supported * 100.0) / jobSkills.size());
    }

    private String explanation(List<String> jobSkills, String evidence, List<ApplicationSubmittedAnswer> answers) {
        if (jobSkills.isEmpty()) {
            return "No required job skills were provided for project consistency comparison.";
        }
        if (isBlank(evidence)) {
            return "No structured project data or applicant answers are available yet; project consistency is neutral.";
        }
        if (answers.isEmpty()) {
            return "Project consistency is approximated from resume summary evidence until structured projects are available.";
        }
        return "Project consistency is approximated from resume summary and applicant answer evidence until structured projects are available.";
    }

    private String review(int score) {
        if (score >= 70) {
            return "Project evidence appears aligned with the target role.";
        }
        if (score >= 40) {
            return "Project evidence needs human review before it affects ranking.";
        }
        return "Project evidence is weak or missing for several required skills.";
    }
}
