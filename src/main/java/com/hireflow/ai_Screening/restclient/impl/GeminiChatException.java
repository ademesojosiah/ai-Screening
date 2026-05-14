package com.hireflow.ai_Screening.restclient.impl;

public class GeminiChatException extends RuntimeException {
    public GeminiChatException(String message) {
        super(message);
    }

    public GeminiChatException(String message, Throwable cause) {
        super(message, cause);
    }
}
