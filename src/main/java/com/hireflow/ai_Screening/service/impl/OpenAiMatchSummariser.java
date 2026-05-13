package com.hireflow.ai_Screening.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.hireflow.ai_Screening.event.ApplicationSubmittedEvent;
import com.hireflow.ai_Screening.event.ScreeningCompletedEvent;
import com.hireflow.ai_Screening.restclient.OpenAiChatClient;
import com.hireflow.ai_Screening.service.MatchSummariser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Primary
@Service
@RequiredArgsConstructor
public class OpenAiMatchSummariser implements MatchSummariser {

    private static final String SYSTEM_PROMPT = """
            You are summarising the overall match between an applicant and the job's required skills.
            Only matched/unmatched skills and matchPercentage drive automatic stage decisions, so be careful and conservative.
            Reply with ONLY a JSON object using exactly these keys:
              "matchPercentage" (integer 0-100, share of required skills the applicant credibly demonstrates),
              "matchedSkills" (array of strings, subset of the job's required skills the applicant demonstrably has),
              "unmatchedSkills" (array of strings, required skills not demonstrated),
              "aiNarrativeSummary" (concise candidate-neutral summary, max 600 chars).
            """;

    private final OpenAiChatClient chatClient;
    private final BasicMatchSummariser fallback;

    @Override
    public ScreeningCompletedEvent summarise(ApplicationSubmittedEvent event) {
        try {
            String userPrompt = OpenAiPromptFactory.jobContext(event)
                    + OpenAiPromptFactory.applicantContext(event)
                    + OpenAiPromptFactory.answersBlock(event);

            JsonNode root = chatClient.completeJson(SYSTEM_PROMPT, userPrompt);
            Integer matchPercentage = OpenAiResponseSupport.clampedScore(root, "matchPercentage");
            List<String> matched = OpenAiResponseSupport.stringArray(root, "matchedSkills");
            List<String> unmatched = OpenAiResponseSupport.stringArray(root, "unmatchedSkills");
            String summary = OpenAiResponseSupport.requireText(root, "aiNarrativeSummary");

            return new ScreeningCompletedEvent(
                    event.getApplicationId(),
                    matchPercentage,
                    matched,
                    unmatched,
                    summary
            );
        } catch (Exception ex) {
            log.warn("Match summarisation OpenAI call failed for {} — falling back to deterministic scoring: {}",
                    event.getApplicationId(), ex.getMessage());
            return fallback.summarise(event);
        }
    }
}
