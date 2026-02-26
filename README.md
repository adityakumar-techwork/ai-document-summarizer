# AI Document Summarizer
A Java-based tool using AWS Bedrock to summarize text.

## Run

Prereqs:
- **AWS credentials** available to the AWS SDK v2 (for example via `~/.aws/credentials`, `AWS_PROFILE`, SSO, or env vars).
- **Bedrock model access** enabled in your AWS account for the model you use.

Build/tests:

```bash
mvn test
```

Summarize a local text file (defaults: model `anthropic.claude-3-haiku-20240307-v1:0`, region `us-east-1`):

```bash
mvn -q exec:java -Dexec.args="./path/to/file.txt"
```

Override model id + region:

```bash
mvn -q exec:java -Dexec.args="./path/to/file.txt anthropic.claude-3-5-sonnet-20241022-v2:0 us-west-2"
```

## How the Bedrock call works (and how to inspect it in Cursor)

Entry point: `src/main/java/com/aditya/ai/App.java`
- Creates `BedrockRuntimeClient`
- Sends an `InvokeModelRequest` with Claude “messages” JSON
- Extracts the returned text from the response JSON

To understand the AWS SDK v2 pieces using Cursor indexing:
- **Jump to definitions** on `BedrockRuntimeClient`, `InvokeModelRequest`, and `InvokeModelResponse` to see the generated SDK types.
- **Search for symbols/usages** like `invokeModel`, `modelId`, and `SdkBytes.fromUtf8String` to trace how requests are built and serialized.
- **Search in dependencies** (Cursor setting) for `InvokeModelRequest.Builder` or `BedrockRuntimeClientBuilder` to see the underlying patterns used across AWS SDK v2 services.
