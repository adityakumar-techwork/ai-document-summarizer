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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class App {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String DEFAULT_REGION = "us-east-1";
    private static final String DEFAULT_MODEL_ID = "anthropic.claude-3-haiku-20240307-v1:0";

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            printUsageAndExit();
        }

        String filePath = args[0];
        String modelId = args.length >= 2 ? args[1] : DEFAULT_MODEL_ID;
        String region = args.length >= 3 ? args[2] : DEFAULT_REGION;

        String inputText = readFileText(filePath);
        String prompt = buildThreeSentenceSummaryPrompt(inputText);

        String summary = summarizeWithClaudeOnBedrock(prompt, modelId, region);
        System.out.println(summary.trim());
    }

    private static void printUsageAndExit() {
        System.err.println("Usage: mvn -q exec:java -Dexec.args=\"<filePath> [modelId] [region]\"");
        System.err.println("Example: mvn -q exec:java -Dexec.args=\"./notes.txt\"");
        System.err.println("Example: mvn -q exec:java -Dexec.args=\"./notes.txt anthropic.claude-3-5-sonnet-20241022-v2:0 us-west-2\"");
        System.exit(2);
    }

    private static String readFileText(String filePath) throws IOException {
        return Files.readString(Path.of(filePath), StandardCharsets.UTF_8);
    }

    private static String buildThreeSentenceSummaryPrompt(String inputText) {
        return """
               Summarize the text below in exactly 3 sentences.
               - Preserve key facts, names, and numbers.
               - No bullet points.
               - Output ONLY the 3 sentences.

               TEXT:
               %s
               """.formatted(inputText);
    }

    private static String summarizeWithClaudeOnBedrock(String userText, String modelId, String region) throws IOException {
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

        String payload = MAPPER.writeValueAsString(root);

        try (BedrockRuntimeClient client = BedrockRuntimeClient.builder()
                .region(Region.of(region))
                .build()) {

            InvokeModelRequest req = InvokeModelRequest.builder()
                    .modelId(modelId)
                    .contentType("application/json")
                    .accept("application/json")
                    .body(SdkBytes.fromUtf8String(payload))
                    .build();

            InvokeModelResponse resp = client.invokeModel(req);
            String respJson = resp.body().asUtf8String();
            return extractClaudeText(respJson);
        }
    }

    private static String extractClaudeText(String responseJson) throws IOException {
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
            if (sb.length() > 0) return sb.toString();
        }
        if (root.hasNonNull("completion")) {
            return root.path("completion").asText();
        }
        return responseJson;
    }
}
