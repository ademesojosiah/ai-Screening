package com.hireflow.ai_Screening.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.hireflow.ai_Screening.event.ApplicationSubmittedEvent;
import com.hireflow.ai_Screening.event.ProjectConsistencyCompletedEvent;
import com.hireflow.ai_Screening.restclient.OpenAiChatClient;
import com.hireflow.ai_Screening.service.ProjectConsistencyScreener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Slf4j
@Primary
@Service
@RequiredArgsConstructor
public class OpenAiProjectConsistencyScreener implements ProjectConsistencyScreener {

    private static final String SYSTEM_PROMPT = """
            You score whether the applicant's resume evidence supports their claimed skills and the job's required stack.
            Use ONLY the resume summary as evidence. Do not request, infer, or evaluate technical question answers — those
            are reserved for human reviewers. Do not reject for missing evidence — flag it as weak and require human review.
            Reply with ONLY a JSON object using exactly these keys:
              "score" (integer 0-100, 100 = resume clearly supports required skills at expected seniority),
              "explanation" (concise candidate-neutral explanation, max 600 chars),
              "review" (internal HR review note, max 600 chars).
            """;

    private final OpenAiChatClient chatClient;
    private final BasicProjectConsistencyScreener fallback;

    @Override
    public ProjectConsistencyCompletedEvent score(ApplicationSubmittedEvent event) {
        try {
            String userPrompt = OpenAiPromptFactory.jobContext(event)
                    + OpenAiPromptFactory.applicantContext(event);

            JsonNode root = chatClient.completeJson(SYSTEM_PROMPT, userPrompt);
            Integer aiScore = OpenAiResponseSupport.clampedScore(root, "score");
            String explanation = OpenAiResponseSupport.requireText(root, "explanation");
            String review = OpenAiResponseSupport.requireText(root, "review");

            return new ProjectConsistencyCompletedEvent(event.getApplicationId(), aiScore, explanation, review);
        } catch (Exception ex) {
            log.warn("Project consistency OpenAI call failed for {} — falling back to deterministic scoring: {}",
                    event.getApplicationId(), ex.getMessage());
            return fallback.score(event);
        }
    }
}
