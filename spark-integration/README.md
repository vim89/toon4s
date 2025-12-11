# toon4s-spark

Apache Spark integration for TOON format - encode DataFrames to token-efficient TOON format for LLM processing.

## Features

- **DataFrame ↔ TOON conversion**: Pure functional API with Either error handling
- **Extension methods**: Fluent `.toToon()` and `.toonMetrics()` on DataFrames
- **Token metrics**: Compare JSON vs TOON token counts and cost savings
- **SQL UDFs**: Register TOON functions for use in Spark SQL queries
- **Chunking support**: Handle large DataFrames with automatic chunking
- **LLM client abstraction**: Vendor-agnostic trait for LLM integration
- **Type-safe**: Comprehensive Scala type safety with ADT error handling

## Installation

Add to your `build.sbt`:

```scala
libraryDependencies += "com.vitthalmirji" %% "toon4s-spark" % "0.1.0"
```

For Spark applications, use `Provided` scope since Spark is typically provided by the cluster:

```scala
libraryDependencies ++= Seq(
  "com.vitthalmirji" %% "toon4s-spark" % "0.1.0",
  "org.apache.spark" %% "spark-sql" % "3.5.0" % Provided
)
```

## Quick Start

### DataFrame to TOON

```scala
import io.toonformat.toon4s.spark.SparkToonOps._
import org.apache.spark.sql.SparkSession

val spark = SparkSession.builder()
  .appName("TOON Example")
  .getOrCreate()

import spark.implicits._

val df = Seq(
  (1, "Alice", 25),
  (2, "Bob", 30)
).toDF("id", "name", "age")

// Encode DataFrame to TOON
df.toToon(key = "users") match {
  case Right(toonChunks) =>
    toonChunks.foreach { toon =>
      println(s"TOON: $toon")
      // Send to LLM or save to storage
    }
  case Left(error) =>
    println(s"Error: ${error.message}")
}
```

### Token Metrics

```scala
// Compare JSON vs TOON token efficiency
df.toonMetrics(key = "data") match {
  case Right(metrics) =>
    println(metrics.summary)
    // Output:
    // Token Metrics:
    //   JSON tokens: 150
    //   TOON tokens: 90
    //   Savings: 60 tokens (40.0%)
    //   Rows: 2, Columns: 3

    val costSavings = metrics.estimatedCostSavings(costPer1kTokens = 0.002)
    println(f"Est. cost savings: $$${costSavings}%.4f")

  case Left(error) =>
    println(s"Error: ${error.message}")
}
```

### SQL UDFs

```scala
import io.toonformat.toon4s.spark.ToonUDFs

// Register TOON functions for SQL
ToonUDFs.register(spark)

spark.sql("""
  SELECT
    id,
    name,
    toon_encode_row(struct(id, name, age)) as toon_data,
    toon_estimate_tokens(struct(id, name, age)) as token_count
  FROM users
""").show()
```

### Round-Trip Conversion

```scala
implicit val sparkSession: SparkSession = spark

val originalDf = df.select("id", "name", "age")
val schema = originalDf.schema

val result = for {
  toonChunks <- originalDf.toToon(key = "data")
  decodedDf <- SparkToonOps.fromToon(toonChunks, schema)
} yield decodedDf

result match {
  case Right(reconstructed) =>
    reconstructed.show()
  case Left(error) =>
    println(s"Error: ${error.message}")
}
```

### Chunking Large DataFrames

```scala
// Handle large datasets with automatic chunking
val largeDf = spark.read.parquet("large_dataset.parquet")

largeDf.toToon(
  key = "data",
  maxRowsPerChunk = 1000  // Process 1000 rows per chunk
) match {
  case Right(chunks) =>
    println(s"Created ${chunks.size} chunks")
    chunks.zipWithIndex.foreach { case (toon, idx) =>
      // Process each chunk independently
      saveToon(s"chunk_$idx.toon", toon)
    }
  case Left(error) =>
    println(s"Error: ${error.message}")
}
```

### LLM Integration

```scala
import io.toonformat.toon4s.spark.{LlmClient, LlmConfig, LlmError, MockLlmClient}

// Use mock client for testing
val client = MockLlmClient(Map(
  "prompt1" -> "response1"
))

// Or implement for your LLM provider
class OpenAIClient(apiKey: String) extends LlmClient {
  val config = LlmConfig.default

  def complete(prompt: String): Either[LlmError, String] = {
    // HTTP call to OpenAI API
    ???
  }
}

// Send TOON data to LLM
df.toToon(key = "analytics_data") match {
  case Right(toonChunks) =>
    toonChunks.foreach { toon =>
      val prompt = s"Analyze this data:\\n$toon"
      client.complete(prompt) match {
        case Right(response) =>
          println(s"LLM Response: $response")
        case Left(error) =>
          println(s"LLM Error: ${error.message}")
      }
    }
  case Left(error) =>
    println(s"Encoding error: ${error.message}")
}
```

## API Reference

### Extension Methods on DataFrame

**`toToon(key: String, maxRowsPerChunk: Int, options: EncodeOptions): Either[SparkToonError, Vector[String]]`**

Encode DataFrame to TOON format with chunking support.

- `key`: Top-level key for TOON document (default: "data")
- `maxRowsPerChunk`: Maximum rows per chunk (default: 1000)
- `options`: TOON encoding options (default: EncodeOptions())

**`toonMetrics(key: String, options: EncodeOptions): Either[SparkToonError, ToonMetrics]`**

Compute token metrics comparing JSON vs TOON efficiency.

**`showToonSample(n: Int): Unit`**

Print a sample of TOON-encoded data for debugging (default: 5 rows).

### Static Methods

**`SparkToonOps.fromToon(toonDocuments: Vector[String], schema: StructType, options: DecodeOptions)(implicit spark: SparkSession): Either[SparkToonError, DataFrame]`**

Decode TOON strings back to DataFrame.

### SQL UDFs

Register with `ToonUDFs.register(spark)`:

- `toon_encode_row(struct)`: Encode struct to TOON
- `toon_decode_row(string)`: Decode TOON to struct
- `toon_encode_string(string)`: Encode string value
- `toon_decode_string(string)`: Decode TOON string
- `toon_estimate_tokens(string)`: Estimate token count

### ToonMetrics

```scala
case class ToonMetrics(
  jsonTokenCount: Int,
  toonTokenCount: Int,
  savingsPercent: Double,
  rowCount: Int,
  columnCount: Int
)
```

Methods:
- `absoluteSavings: Int` - Token count difference
- `compressionRatio: Double` - TOON/JSON ratio
- `estimatedCostSavings(costPer1kTokens: Double): Double` - Cost savings estimate
- `hasMeaningfulSavings(threshold: Double): Boolean` - Check if savings exceed threshold
- `summary: String` - Formatted summary

### Error Handling

All operations return `Either[SparkToonError, A]` for explicit error handling:

```scala
sealed trait SparkToonError {
  def message: String
  def cause: Option[Throwable]
}

object SparkToonError {
  case class ConversionError(message: String, cause: Option[Throwable] = None)
  case class EncodingError(toonError: EncodeError, cause: Option[Throwable] = None)
  case class DecodingError(toonError: DecodeError, cause: Option[Throwable] = None)
  case class SchemaMismatch(expected: String, actual: String, cause: Option[Throwable] = None)
  case class CollectionError(message: String, cause: Option[Throwable] = None)
  case class UnsupportedDataType(dataType: String, cause: Option[Throwable] = None)
}
```

## Configuration

### Java 17+ Compatibility

For Java 17 or later, add these JVM options:

```scala
// build.sbt
Test / javaOptions ++= Seq(
  "--add-opens=java.base/sun.nio.ch=ALL-UNNAMED",
  "--add-opens=java.base/sun.nio.cs=ALL-UNNAMED"
)
```

### Spark Configuration

```scala
val spark = SparkSession.builder()
  .appName("TOON App")
  .config("spark.sql.shuffle.partitions", "100")
  .config("spark.executor.memory", "4g")
  .getOrCreate()
```

## Performance Tips

1. **Chunking**: Use `maxRowsPerChunk` to control memory usage for large DataFrames
2. **Caching**: Cache DataFrames before multiple TOON operations
3. **Partitioning**: Repartition for parallelism before encoding
4. **Token estimation**: Use `toonMetrics()` to verify savings before committing to TOON

```scala
val largeDf = spark.read.parquet("data.parquet")
  .repartition(100)  // Parallelize
  .cache()           // Cache for reuse

// Check if TOON provides meaningful savings
largeDf.toonMetrics() match {
  case Right(metrics) if metrics.hasMeaningfulSavings(threshold = 15.0) =>
    // Proceed with TOON encoding
    largeDf.toToon(maxRowsPerChunk = 1000)
  case Right(metrics) =>
    // Savings too small, use JSON
    println(s"TOON savings only ${metrics.savingsPercent}%, using JSON")
  case Left(error) =>
    println(s"Error: ${error.message}")
}
```

## Requirements

- Scala 2.12 or 2.13
- Apache Spark 3.5.0+
- Java 11+ (Java 17+ recommended)

## License

MIT License - See LICENSE file for details.
