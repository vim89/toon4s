package io.toonformat.toon4s.spark

/**
 * Vendor-agnostic LLM client abstraction.
 *
 * ==Design Principles==
 *   - Trait-based abstraction (Strategy pattern)
 *   - Error ADT for typed error handling
 *   - Configuration case class for immutability
 *   - Pure functional interface (no side effects in trait definition)
 *
 * ==Usage==
 * {{{
 * // Implement for your LLM provider
 * class OpenAIClient(apiKey: String, config: LlmConfig) extends LlmClient {
 *   def complete(prompt: String): Either[LlmError, String] = {
 *     // HTTP call to OpenAI API
 *     Try {
 *       val response = http.post("https://api.openai.com/v1/chat/completions")
 *         .header("Authorization", s"Bearer \$apiKey")
 *         .body(buildRequest(prompt))
 *         .asString
 *       parseResponse(response.body)
 *     }.toEither.left.map(ex => LlmError.ApiError(ex.getMessage, 500))
 *   }
 * }
 *
 * // Use in Spark pipeline
 * val client = new OpenAIClient(apiKey, LlmConfig.default)
 *
 * df.toToon() match {
 *   case Right(toonChunks) =>
 *     toonChunks.foreach { toon =>
 *       client.complete(s"Analyze this data:\\n\$toon") match {
 *         case Right(response) => println(response)
 *         case Left(error) => logger.error(s"LLM error: \$error")
 *       }
 *     }
 *   case Left(error) =>
 *     logger.error(s"Encoding error: \${error.message}")
 * }
 * }}}
 */
trait LlmClient {

  /**
   * Send prompt to LLM and receive completion.
   *
   * Pure interface definition. Implementations should handle all I/O, retries, and error recovery
   * internally.
   *
   * @param prompt
   *   Input prompt (may include TOON-encoded data)
   * @return
   *   Either LLM error or completion text
   */
  def complete(prompt: String): Either[LlmError, String]

  /**
   * Send prompt with streaming response.
   *
   * Optional method for streaming responses. Default implementation calls complete.
   *
   * @param prompt
   *   Input prompt
   * @param onChunk
   *   Callback for each response chunk
   * @return
   *   Either error or unit
   */
  def completeStreaming(
    prompt: String
  )(onChunk: String => Unit): Either[LlmError, Unit] = {
    complete(prompt).map(response => onChunk(response))
  }

  /**
   * Get LLM configuration.
   *
   * @return
   *   Current configuration
   */
  def config: LlmConfig
}

/**
 * LLM configuration.
 *
 * Immutable case class for LLM parameters. All fields have sensible defaults.
 *
 * @param model
 *   Model identifier (e.g., "gpt-4", "claude-3-sonnet")
 * @param maxTokens
 *   Maximum tokens in response
 * @param temperature
 *   Sampling temperature (0.0 = deterministic, 1.0 = creative)
 * @param systemPrompt
 *   System prompt to prepend to all requests
 * @param timeoutMillis
 *   Request timeout in milliseconds
 * @param retryAttempts
 *   Number of retry attempts on transient failures
 */
final case class LlmConfig(
  model: String,
  maxTokens: Int = 4096,
  temperature: Double = 0.7,
  systemPrompt: String = "",
  timeoutMillis: Long = 30000,
  retryAttempts: Int = 3
)

object LlmConfig {

  /** Default configuration for general-purpose use. */
  val default: LlmConfig = LlmConfig(
    model = "gpt-4",
    maxTokens = 4096,
    temperature = 0.7
  )

  /** Configuration for deterministic outputs (zero temperature). */
  val deterministic: LlmConfig = default.copy(temperature = 0.0)

  /** Configuration for creative outputs (higher temperature). */
  val creative: LlmConfig = default.copy(temperature = 0.9)

  /** Configuration for code generation (lower temperature, more tokens). */
  val codeGeneration: LlmConfig = LlmConfig(
    model = "gpt-4",
    maxTokens = 8192,
    temperature = 0.2,
    systemPrompt = "You are an expert programmer. Generate clean, idiomatic code."
  )

  /** Configuration for data analysis (structured outputs). */
  val dataAnalysis: LlmConfig = LlmConfig(
    model = "gpt-4",
    maxTokens = 4096,
    temperature = 0.3,
    systemPrompt = "You are a data analyst. Provide structured, factual analysis."
  )
}

/**
 * LLM error types.
 *
 * Sealed trait for exhaustive pattern matching on error cases. Covers common failure modes:
 * API errors, timeouts, rate limits, and validation errors.
 */
sealed trait LlmError {
  def message: String
}

object LlmError {

  /**
   * API error from LLM provider.
   *
   * @param message
   *   Error message from provider
   * @param statusCode
   *   HTTP status code
   */
  final case class ApiError(message: String, statusCode: Int) extends LlmError

  /**
   * Request timeout.
   *
   * @param message
   *   Timeout description
   */
  final case class TimeoutError(message: String) extends LlmError

  /**
   * Rate limit exceeded.
   *
   * @param retryAfterSeconds
   *   Seconds until retry is allowed
   */
  final case class RateLimitError(retryAfterSeconds: Int) extends LlmError {
    def message: String = s"Rate limit exceeded. Retry after $retryAfterSeconds seconds."
  }

  /**
   * Validation error (e.g., prompt too long).
   *
   * @param message
   *   Validation failure description
   */
  final case class ValidationError(message: String) extends LlmError

  /**
   * Authentication error.
   *
   * @param message
   *   Auth failure description
   */
  final case class AuthenticationError(message: String) extends LlmError

  /**
   * Unexpected error.
   *
   * @param message
   *   Error description
   * @param cause
   *   Optional underlying exception
   */
  final case class UnknownError(message: String, cause: Option[Throwable] = None)
      extends LlmError
}

/**
 * Helper functions for LLM client implementations.
 */
object LlmClientHelpers {

  /**
   * Retry logic with exponential backoff.
   *
   * Pure functional retry combinator. Useful for implementing retry logic in client
   * implementations.
   *
   * @param maxAttempts
   *   Maximum retry attempts
   * @param baseDelayMillis
   *   Base delay for exponential backoff
   * @param operation
   *   Operation to retry
   * @return
   *   Either final error or success value
   */
  def retry[A](
    maxAttempts: Int,
    baseDelayMillis: Long = 1000
  )(operation: => Either[LlmError, A]): Either[LlmError, A] = {

    @scala.annotation.tailrec
    def attempt(attemptsLeft: Int, lastError: Option[LlmError]): Either[LlmError, A] = {
      if (attemptsLeft <= 0) {
        Left(lastError.getOrElse(LlmError.UnknownError("Retry exhausted with no error")))
      } else {
        operation match {
          case Right(value) => Right(value)
          case Left(error @ LlmError.RateLimitError(retryAfter)) =>
            // Don't retry rate limit errors immediately
            Thread.sleep(retryAfter * 1000L)
            attempt(attemptsLeft - 1, Some(error))
          case Left(error @ LlmError.TimeoutError(_)) =>
            // Retry transient errors with exponential backoff
            val delay = baseDelayMillis * (maxAttempts - attemptsLeft + 1)
            Thread.sleep(delay)
            attempt(attemptsLeft - 1, Some(error))
          case Left(error @ LlmError.ApiError(_, code)) if code >= 500 =>
            // Retry transient errors with exponential backoff
            val delay = baseDelayMillis * (maxAttempts - attemptsLeft + 1)
            Thread.sleep(delay)
            attempt(attemptsLeft - 1, Some(error))
          case Left(error) =>
            // Don't retry non-transient errors
            Left(error)
        }
      }
    }

    attempt(maxAttempts, None)
  }

  /**
   * Validate prompt length against token limit.
   *
   * @param prompt
   *   Input prompt
   * @param maxTokens
   *   Maximum allowed tokens
   * @return
   *   Either validation error or unit
   */
  def validatePromptLength(prompt: String, maxTokens: Int): Either[LlmError, Unit] = {
    val estimatedTokens = ToonMetrics.estimateTokens(prompt)
    if (estimatedTokens > maxTokens) {
      Left(
        LlmError.ValidationError(
          s"Prompt too long: $estimatedTokens tokens (max: $maxTokens)"
        )
      )
    } else {
      Right(())
    }
  }

  /**
   * Build prompt with system message and user content.
   *
   * @param systemPrompt
   *   System prompt
   * @param userContent
   *   User content (may include TOON data)
   * @return
   *   Formatted prompt
   */
  def buildPrompt(systemPrompt: String, userContent: String): String = {
    if (systemPrompt.isEmpty) userContent
    else s"$systemPrompt\n\n$userContent"
  }
}

/**
 * Mock LLM client for testing.
 *
 * Returns predefined responses for testing without making actual API calls.
 */
class MockLlmClient(val config: LlmConfig, responses: Map[String, String] = Map.empty)
    extends LlmClient {

  def complete(prompt: String): Either[LlmError, String] = {
    responses.get(prompt) match {
      case Some(response) => Right(response)
      case None =>
        Right(s"Mock response for prompt: ${prompt.take(50)}...")
    }
  }
}

object MockLlmClient {

  /** Create mock client with default config. */
  def apply(responses: Map[String, String] = Map.empty): MockLlmClient = {
    new MockLlmClient(LlmConfig.default, responses)
  }

  /** Create mock client that always succeeds. */
  val alwaysSucceeds: MockLlmClient = new MockLlmClient(
    LlmConfig.default,
    Map.empty
  )

  /** Create mock client that always fails. */
  val alwaysFails: LlmClient = new LlmClient {
    def config: LlmConfig = LlmConfig.default
    def complete(prompt: String): Either[LlmError, String] = {
      Left(LlmError.ApiError("Mock failure", 500))
    }
  }
}
