package com.hireflow.ai_Screening.service.impl;

import com.hireflow.ai_Screening.event.ApplicationSubmittedEvent;

import java.util.List;

final class GeminiPromptFactory {

    private GeminiPromptFactory() {}

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
