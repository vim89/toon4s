package io.toonformat.toon4s.spark

import munit.FunSuite

class LlmClientTest extends FunSuite {

  test("LlmConfig.default: has sensible defaults") {
    val config = LlmConfig.default

    assertEquals(config.model, "gpt-4")
    assertEquals(config.maxTokens, 4096)
    assertEquals(config.temperature, 0.7)
    assertEquals(config.systemPrompt, "")
    assertEquals(config.timeoutMillis, 30000L)
    assertEquals(config.retryAttempts, 3)
  }

  test("LlmConfig.deterministic: zero temperature") {
    val config = LlmConfig.deterministic

    assertEquals(config.temperature, 0.0)
  }

  test("LlmConfig.creative: higher temperature") {
    val config = LlmConfig.creative

    assertEquals(config.temperature, 0.9)
  }

  test("LlmConfig.codeGeneration: appropriate settings") {
    val config = LlmConfig.codeGeneration

    assertEquals(config.maxTokens, 8192)
    assertEquals(config.temperature, 0.2)
    assert(config.systemPrompt.contains("programmer"))
  }

  test("LlmConfig.dataAnalysis: appropriate settings") {
    val config = LlmConfig.dataAnalysis

    assertEquals(config.temperature, 0.3)
    assert(config.systemPrompt.contains("data analyst"))
  }

  test("MockLlmClient: return predefined response") {
    val responses = Map("test prompt" -> "test response")
    val client = MockLlmClient(responses)

    val result = client.complete("test prompt")

    assert(result.isRight)
    assertEquals(result.getOrElse(""), "test response")
  }

  test("MockLlmClient: return default response for unknown prompt") {
    val client = MockLlmClient()

    val result = client.complete("unknown prompt")

    assert(result.isRight)
    assert(result.getOrElse("").contains("Mock response"))
  }

  test("MockLlmClient.alwaysSucceeds: always return success") {
    val client = MockLlmClient.alwaysSucceeds

    val result = client.complete("any prompt")

    assert(result.isRight)
  }

  test("MockLlmClient.alwaysFails: always return error") {
    val client = MockLlmClient.alwaysFails

    val result = client.complete("any prompt")

    assert(result.isLeft)
  }

  test("LlmError.ApiError: has message and status code") {
    val error = LlmError.ApiError("Not found", 404)

    assertEquals(error.message, "Not found")
    assertEquals(error.statusCode, 404)
  }

  test("LlmError.TimeoutError: has message") {
    val error = LlmError.TimeoutError("Request timed out")

    assertEquals(error.message, "Request timed out")
  }

  test("LlmError.RateLimitError: format message with retry time") {
    val error = LlmError.RateLimitError(60)

    assert(error.message.contains("60"))
    assert(error.message.contains("seconds"))
  }

  test("LlmError.ValidationError: has message") {
    val error = LlmError.ValidationError("Prompt too long")

    assertEquals(error.message, "Prompt too long")
  }

  test("LlmError.AuthenticationError: has message") {
    val error = LlmError.AuthenticationError("Invalid API key")

    assertEquals(error.message, "Invalid API key")
  }

  test("LlmError.UnknownError: has message and optional cause") {
    val cause = new RuntimeException("test")
    val error = LlmError.UnknownError("Something went wrong", Some(cause))

    assertEquals(error.message, "Something went wrong")
    assertEquals(error.cause, Some(cause))
  }

  test("LlmClientHelpers.validatePromptLength: pass validation") {
    val prompt = "short prompt"
    val result = LlmClientHelpers.validatePromptLength(prompt, 1000)

    assert(result.isRight)
  }

  test("LlmClientHelpers.validatePromptLength: fail validation") {
    val prompt = "a" * 10000 // Very long prompt
    val result = LlmClientHelpers.validatePromptLength(prompt, 100)

    assert(result.isLeft)
    result.left.foreach {
      case LlmError.ValidationError(msg) =>
        assert(msg.contains("too long"))
      case _ => fail("Expected ValidationError")
    }
  }

  test("LlmClientHelpers.buildPrompt: with system prompt") {
    val system = "You are a helpful assistant."
    val user = "What is 2+2?"

    val prompt = LlmClientHelpers.buildPrompt(system, user)

    assert(prompt.contains(system))
    assert(prompt.contains(user))
  }

  test("LlmClientHelpers.buildPrompt: without system prompt") {
    val user = "What is 2+2?"

    val prompt = LlmClientHelpers.buildPrompt("", user)

    assertEquals(prompt, user)
  }

  test("LlmClientHelpers.retry: succeed on first attempt") {
    var attempts = 0
    val operation: Either[LlmError, String] = {
      attempts += 1
      Right("success")
    }

    val result = LlmClientHelpers.retry(maxAttempts = 3, baseDelayMillis = 10) {
      operation
    }

    assert(result.isRight)
    assertEquals(attempts, 1)
  }

  test("LlmClientHelpers.retry: exhaust retries") {
    var attempts = 0
    def operation: Either[LlmError, String] = {
      attempts += 1
      Left(LlmError.ApiError("Server error", 500))
    }

    val result = LlmClientHelpers.retry(maxAttempts = 3, baseDelayMillis = 1) {
      operation
    }

    assert(result.isLeft)
    assertEquals(attempts, 3)
  }

  test("LlmClientHelpers.retry: don't retry client errors") {
    var attempts = 0
    def operation: Either[LlmError, String] = {
      attempts += 1
      Left(LlmError.ApiError("Bad request", 400))
    }

    val result = LlmClientHelpers.retry(maxAttempts = 3, baseDelayMillis = 1) {
      operation
    }

    assert(result.isLeft)
    assertEquals(attempts, 1) // Should not retry 4xx errors
  }

  test("MockLlmClient: has config") {
    val config = LlmConfig(model = "test-model")
    val client = new MockLlmClient(config)

    assertEquals(client.config.model, "test-model")
  }

  test("MockLlmClient.completeStreaming: calls callback") {
    val client = MockLlmClient(Map("test" -> "response"))
    var receivedChunk: Option[String] = None

    val result = client.completeStreaming("test") { chunk =>
      receivedChunk = Some(chunk)
    }

    assert(result.isRight)
    assertEquals(receivedChunk, Some("response"))
  }
}
