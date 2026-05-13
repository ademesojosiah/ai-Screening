package com.hireflow.ai_Screening.restclient.impl;

public class OpenAiChatException extends RuntimeException {
    public OpenAiChatException(String message) {
        super(message);
    }

    public OpenAiChatException(String message, Throwable cause) {
        super(message, cause);
    }
}
