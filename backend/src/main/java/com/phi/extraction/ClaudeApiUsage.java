package com.phi.extraction;

public record ClaudeApiUsage(int inputTokens, int outputTokens, String model) {

    public static final ClaudeApiUsage EMPTY = new ClaudeApiUsage(0, 0, null);

    public int totalTokens() {
        return inputTokens + outputTokens;
    }
}
