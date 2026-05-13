package com.hireflow.ai_Screening.service.impl;

import com.hireflow.ai_Screening.event.ApplicationSubmittedAnswer;
import com.hireflow.ai_Screening.event.ApplicationSubmittedEvent;

import java.util.List;

final class OpenAiPromptFactory {

    private OpenAiPromptFactory() {}

    static String jobContext(ApplicationSubmittedEvent event) {
        StringBuilder builder = new StringBuilder();
        builder.append("JOB TITLE: ").append(nullSafe(event.getJobTitle())).append('\n');
        builder.append("JOB SUMMARY: ").append(nullSafe(event.getJobSummary())).append('\n');
        builder.append("REQUIRED QUALIFICATIONS: ").append(nullSafe(event.getRequiredQualifications())).append('\n');
        builder.append("PREFERRED QUALIFICATIONS: ").append(nullSafe(event.getPreferredQualifications())).append('\n');
        builder.append("REQUIRED SKILLS: ").append(joinSkills(event.getJobSkills())).append('\n');
        return builder.toString();
    }

    static String applicantContext(ApplicationSubmittedEvent event) {
        StringBuilder builder = new StringBuilder();
        builder.append("APPLICANT SKILLS: ").append(joinSkills(event.getApplicantSkills())).append('\n');
        builder.append("RESUME SUMMARY: ").append(nullSafe(event.getResumeSummary())).append('\n');
        return builder.toString();
    }

    static String answersBlock(ApplicationSubmittedEvent event) {
        List<ApplicationSubmittedAnswer> answers = event.getAnswers();
        if (answers == null || answers.isEmpty()) {
            return "TECHNICAL ANSWERS: (none submitted)\n";
        }
        StringBuilder builder = new StringBuilder();
        builder.append("TECHNICAL ANSWERS:\n");
        int index = 1;
        for (ApplicationSubmittedAnswer answer : answers) {
            if (answer == null) continue;
            builder.append(index++).append(". Q: ").append(nullSafe(answer.getQuestion())).append('\n');
            builder.append("   EXPECTED GUIDE: ").append(nullSafe(answer.getExpectedAnswerGuide())).append('\n');
            builder.append("   APPLICANT ANSWER: ").append(nullSafe(answer.getApplicantAnswer())).append('\n');
        }
        return builder.toString();
    }

    private static String nullSafe(String value) {
        return value == null ? "" : value;
    }

    private static String joinSkills(List<String> skills) {
        if (skills == null || skills.isEmpty()) {
            return "(none)";
        }
        return String.join(", ", skills);
    }
}
