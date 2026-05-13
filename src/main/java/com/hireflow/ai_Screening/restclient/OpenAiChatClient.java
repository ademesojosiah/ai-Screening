package com.hireflow.ai_Screening.restclient;

import com.fasterxml.jackson.databind.JsonNode;

public interface OpenAiChatClient {

    /**
     * Calls OpenAI's chat completion endpoint expecting a JSON object response,
     * then returns the parsed root node of that JSON object.
     */
    JsonNode completeJson(String systemPrompt, String userPrompt);
}
