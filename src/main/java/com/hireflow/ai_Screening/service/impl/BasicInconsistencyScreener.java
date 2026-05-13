package com.hireflow.ai_Screening.service.impl;

import com.hireflow.ai_Screening.event.ApplicationSubmittedEvent;
import com.hireflow.ai_Screening.event.InconsistencyReviewCompletedEvent;
import com.hireflow.ai_Screening.service.InconsistencyScreener;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.hireflow.ai_Screening.service.impl.ScreeningAnalysisSupport.evidenceSupportsSkill;
import static com.hireflow.ai_Screening.service.impl.ScreeningAnalysisSupport.isBlank;
import static com.hireflow.ai_Screening.service.impl.ScreeningAnalysisSupport.safeList;

@Service
public class BasicInconsistencyScreener implements InconsistencyScreener {

    @Override
    public InconsistencyReviewCompletedEvent detect(ApplicationSubmittedEvent event) {
        List<String> applicantSkills = safeList(event.getApplicantSkills());

        int score = skillClaimRisk(applicantSkills, event.getResumeSummary());
        String severity = severity(score);
        String explanation = explanation(applicantSkills, event.getResumeSummary());
        String review = review(severity);
        String action = recommendedAction(severity);

        return new InconsistencyReviewCompletedEvent(
                event.getApplicationId(),
                score,
                severity,
                explanation,
                review,
                action
        );
    }

    private int skillClaimRisk(List<String> applicantSkills, String resumeSummary) {
        if (applicantSkills.isEmpty() || isBlank(resumeSummary)) {
            return 0;
        }
        long unsupported = applicantSkills.stream()
                .filter(skill -> !evidenceSupportsSkill(resumeSummary, skill))
                .count();
        return (int) Math.round((unsupported * 100.0) / applicantSkills.size());
    }

    private String severity(int score) {
        if (score >= 70) return "HIGH";
        if (score >= 40) return "MEDIUM";
        return "LOW";
    }

    private String explanation(List<String> applicantSkills, String resumeSummary) {
        if (applicantSkills.isEmpty()) {
            return "No applicant skills were provided for inconsistency comparison.";
        }
        if (isBlank(resumeSummary)) {
            return "Resume summary is missing; no contradictions could be evaluated.";
        }
        return "Compared claimed applicant skills against the resume summary.";
    }

    private String review(String severity) {
        return switch (severity) {
            case "HIGH" -> "High inconsistency risk. HR should review the resume before advancing the applicant.";
            case "MEDIUM" -> "Some skill claims need human review before they influence recommendations.";
            default -> "No major contradictions detected in the available resume evidence.";
        };
    }

    private String recommendedAction(String severity) {
        return switch (severity) {
            case "HIGH" -> "Manually review the unsupported skill claims against the resume and candidate Q&A section.";
            case "MEDIUM" -> "Check the weaker skill claims during assessment or interview.";
            default -> "Proceed with normal screening review.";
        };
    }
}
