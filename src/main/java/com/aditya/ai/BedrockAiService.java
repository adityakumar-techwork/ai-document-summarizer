package com.aditya.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

import java.io.IOException;

/**
 * Service wrapper around Amazon Bedrock (Claude via AWS SDK v2).
 */
public class BedrockAiService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String modelId;
    private final Region region;

    public BedrockAiService(String modelId, String region) {
        this.modelId = modelId;
        this.region = Region.of(region);
    }

    /**
     * Send an arbitrary user prompt to Claude on Bedrock and return the text response.
     */
    public String invokeClaude(String userPrompt) throws IOException {
        String payload = buildClaudeMessagesPayload(userPrompt);

        try (BedrockRuntimeClient client = BedrockRuntimeClient.builder()
                .region(region)
                .build()) {

            InvokeModelRequest request = InvokeModelRequest.builder()
                    .modelId(modelId)
                    .contentType("application/json")
                    .accept("application/json")
                    .body(SdkBytes.fromUtf8String(payload))
                    .build();

            InvokeModelResponse response = client.invokeModel(request);
            String responseJson = response.body().asUtf8String();
            return extractClaudeText(responseJson);
        }
    }

    /**
     * Convenience: build a “3 sentence summary” prompt and send it.
     */
    public String summarizeInThreeSentences(String text) throws IOException {
        String prompt = """
                Summarize the text below in exactly 3 sentences.
                - Preserve key facts, names, and numbers.
                - No bullet points.
                - Output ONLY the 3 sentences.

                TEXT:
                %s
                """.formatted(text);
        return invokeClaude(prompt);
    }

    private String buildClaudeMessagesPayload(String userText) throws IOException {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("anthropic_version", "bedrock-2023-05-31");
        root.put("max_tokens", 300);
        root.put("temperature", 0.2);

        ArrayNode messages = root.putArray("messages");
        ObjectNode userMsg = messages.addObject();
        userMsg.put("role", "user");
        ArrayNode content = userMsg.putArray("content");
        ObjectNode contentItem = content.addObject();
        contentItem.put("type", "text");
        contentItem.put("text", userText);

        return MAPPER.writeValueAsString(root);
    }

    private String extractClaudeText(String responseJson) throws IOException {
        JsonNode root = MAPPER.readTree(responseJson);
        JsonNode content = root.path("content");
        if (content.isArray() && !content.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (JsonNode item : content) {
                if ("text".equals(item.path("type").asText())) {
                    if (sb.length() > 0) sb.append('\n');
                    sb.append(item.path("text").asText());
                }
            }
            if (sb.length() > 0) {
                return sb.toString();
            }
        }
        if (root.hasNonNull("completion")) {
            return root.path("completion").asText();
        }
        return responseJson;
    }
}

