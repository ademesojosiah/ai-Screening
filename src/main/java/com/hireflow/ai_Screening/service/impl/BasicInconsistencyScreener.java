package com.hireflow.ai_Screening.service.impl;

import com.hireflow.ai_Screening.event.ApplicationSubmittedAnswer;
import com.hireflow.ai_Screening.event.ApplicationSubmittedEvent;
import com.hireflow.ai_Screening.event.InconsistencyReviewCompletedEvent;
import com.hireflow.ai_Screening.service.InconsistencyScreener;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

import static com.hireflow.ai_Screening.service.impl.ScreeningAnalysisSupport.combineEvidence;
import static com.hireflow.ai_Screening.service.impl.ScreeningAnalysisSupport.evidenceSupportsSkill;
import static com.hireflow.ai_Screening.service.impl.ScreeningAnalysisSupport.isBlank;
import static com.hireflow.ai_Screening.service.impl.ScreeningAnalysisSupport.normalize;
import static com.hireflow.ai_Screening.service.impl.ScreeningAnalysisSupport.safeList;
import static com.hireflow.ai_Screening.service.impl.ScreeningAnalysisSupport.significantTokens;

@Service
public class BasicInconsistencyScreener implements InconsistencyScreener {

    @Override
    public InconsistencyReviewCompletedEvent detect(ApplicationSubmittedEvent event) {
        List<String> applicantSkills = safeList(event.getApplicantSkills());
        List<ApplicationSubmittedAnswer> answers = safeList(event.getAnswers());

        int skillRisk = skillClaimRisk(applicantSkills, event.getResumeSummary(), answers);
        int answerRisk = answerRisk(answers);
        int score = combine(skillRisk, answerRisk, answers.isEmpty());

        String severity = severity(score);
        String explanation = explanation(applicantSkills, event.getResumeSummary(), answers);
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

    private int skillClaimRisk(List<String> applicantSkills, String resumeSummary, List<ApplicationSubmittedAnswer> answers) {
        if (applicantSkills.isEmpty()) {
            return 0;
        }
        String evidence = combineEvidence(resumeSummary, answers);
        if (isBlank(evidence)) {
            return 0;
        }
        long unsupported = applicantSkills.stream().filter(skill -> !evidenceSupportsSkill(evidence, skill)).count();
        return (int) Math.round((unsupported * 100.0) / applicantSkills.size());
    }

    private int answerRisk(List<ApplicationSubmittedAnswer> answers) {
        if (answers.isEmpty()) {
            return 0;
        }
        long weak = answers.stream().filter(this::isWeakAnswer).count();
        return (int) Math.round((weak * 100.0) / answers.size());
    }

    private int combine(int skillRisk, int answerRisk, boolean answersAbsent) {
        if (answersAbsent) {
            return skillRisk;
        }
        return Math.min(100, (int) Math.round((skillRisk * 0.6) + (answerRisk * 0.4)));
    }

    private boolean isWeakAnswer(ApplicationSubmittedAnswer answer) {
        if (answer == null) {
            return true;
        }
        String applicant = normalize(answer.getApplicantAnswer());
        if (applicant.isEmpty() || applicant.length() < 20) {
            return true;
        }
        String guide = normalize(answer.getExpectedAnswerGuide());
        if (guide.isEmpty()) {
            return false;
        }
        Set<String> guideTokens = significantTokens(guide);
        if (guideTokens.isEmpty()) {
            return false;
        }
        Set<String> applicantTokens = significantTokens(applicant);
        long overlap = guideTokens.stream().filter(applicantTokens::contains).count();
        return ((double) overlap / guideTokens.size()) < 0.2;
    }

    private String severity(int score) {
        if (score >= 70) return "HIGH";
        if (score >= 40) return "MEDIUM";
        return "LOW";
    }

    private String explanation(List<String> applicantSkills, String resumeSummary, List<ApplicationSubmittedAnswer> answers) {
        if (applicantSkills.isEmpty() && answers.isEmpty()) {
            return "No applicant skills or answers were provided for inconsistency comparison.";
        }
        if (isBlank(resumeSummary) && answers.isEmpty()) {
            return "Resume summary is missing and no answers were submitted, so no contradiction was detected.";
        }
        if (answers.isEmpty()) {
            return "Compared claimed applicant skills against resume summary evidence.";
        }
        return "Compared claimed applicant skills and technical answers against resume summary and answer guides.";
    }

    private String review(String severity) {
        return switch (severity) {
            case "HIGH" -> "High inconsistency risk. HR should review the evidence before advancing the applicant.";
            case "MEDIUM" -> "Some claims need human review before they influence recommendations.";
            default -> "No major contradictions detected in the available resume evidence.";
        };
    }

    private String recommendedAction(String severity) {
        return switch (severity) {
            case "HIGH" -> "Manually review unsupported skill claims, weak technical answers, and project evidence.";
            case "MEDIUM" -> "Check the weaker claims or answers during assessment or interview.";
            default -> "Proceed with normal screening review.";
        };
    }
}
