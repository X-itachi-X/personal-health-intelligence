package com.phi.extraction;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ClaudeResponseParserTest {

    private final ClaudeResponseParser parser = new ClaudeResponseParser();

    @Test
    void parsesBiomarkerArrayFromClaudeContent() throws Exception {
        String body = """
                {
                  "stop_reason": "end_turn",
                  "content": [
                    {
                      "type": "text",
                      "text": "[{\\"testName\\":\\"Vitamin D\\",\\"canonical\\":\\"vitamin_d\\",\\"value\\":21.3,\\"unit\\":\\"ng/mL\\",\\"referenceRange\\":\\"30-100\\",\\"confidence\\":0.95}]"
                    }
                  ]
                }
                """;

        var results = parser.parseBiomarkers(body);

        assertEquals(1, results.size());
        assertEquals("vitamin_d", results.get(0).canonical());
        assertEquals(new BigDecimal("21.3"), results.get(0).value());
    }

    @Test
    void parsesBiomarkerArrayFromMarkdownWrappedContent() throws Exception {
        String body = """
                {
                  "stop_reason": "end_turn",
                  "content": [
                    {
                      "type": "text",
                      "text": "```json\\n[{\\"testName\\":\\"ALT\\",\\"canonical\\":\\"alt\\",\\"value\\":22,\\"unit\\":\\"U/L\\",\\"referenceRange\\":\\"0-40\\",\\"confidence\\":0.9}]\\n```"
                    }
                  ]
                }
                """;

        var results = parser.parseBiomarkers(body);

        assertEquals(1, results.size());
        assertEquals("alt", results.get(0).canonical());
    }
}
