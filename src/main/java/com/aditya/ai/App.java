package com.aditya.ai;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class App {
    private static final String DEFAULT_REGION = "us-east-1";
    private static final String DEFAULT_MODEL_ID = "anthropic.claude-3-haiku-20240307-v1:0";

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            printUsageAndExit();
        }

        String filePath = args[0];
        String modelId = args.length >= 2 ? args[1] : DEFAULT_MODEL_ID;
        String region = args.length >= 3 ? args[2] : DEFAULT_REGION;

        String inputText = Files.readString(Path.of(filePath), StandardCharsets.UTF_8);
        BedrockAiService service = new BedrockAiService(modelId, region);
        String summary = service.summarizeInThreeSentences(inputText);
        System.out.println(summary.trim());
    }

    private static void printUsageAndExit() {
        System.err.println("Usage: mvn -q exec:java -Dexec.args=\"<filePath> [modelId] [region]\"");
        System.err.println("Example: mvn -q exec:java -Dexec.args=\"./notes.txt\"");
        System.err.println("Example: mvn -q exec:java -Dexec.args=\"./notes.txt anthropic.claude-3-5-sonnet-20241022-v2:0 us-west-2\"");
        System.exit(2);
    }
}
