package com.phi.extraction;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class ClaudeUsageParser {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public ClaudeApiUsage parse(String responseBody, String model) {
        try {
            JsonNode usage = objectMapper.readTree(responseBody).path("usage");
            int input = usage.path("input_tokens").asInt(0);
            int output = usage.path("output_tokens").asInt(0);
            return new ClaudeApiUsage(input, output, model);
        } catch (Exception e) {
            return ClaudeApiUsage.EMPTY;
        }
    }
}
