# toon4s-spark: Strategic Alignment with Benchmark Findings & Spark Ecosystem

**Date:** 2025-12-11
**Status:** Strategic Implementation Guide
**Based On:** TOON Generation Benchmark Analysis + 2025 Spark Ecosystem Research

---

## Executive Summary

After analyzing the TOON Generation Benchmark and researching the 2025 Spark ecosystem (Databricks, Iceberg, Delta Lake, DuckDB), we have a **crystal-clear picture** of where toon4s-spark fits:

### ✅ The Perfect Match: TOON's Sweet Spot = Spark's Core Use Case

**TOON wins at:**
- Tabular data
- Uniform arrays
- Shallow nesting
- Large-scale datasets

**Spark excels at:**
- SQL query results
- Transactional data (orders, invoices, logs)
- ETL pipelines
- Large-scale analytics

**This is a PERFECT ALIGNMENT.** toon4s-spark targets exactly where TOON shines.

### ❌ What We Must Avoid

Based on the benchmark's **brutal honesty**:
- Deep hierarchies (0% one-shot accuracy in company case)
- Complex nested structures
- Small datasets (prompt tax > token savings)
- Pretending TOON is a general JSON replacement

---

## Part 1: Benchmark Insights for Spark Integration

### Finding #1: Spark Produces "Aligned" Data

**What Spark Typically Produces:**
```scala
// Example: Typical Spark SQL result
val results = spark.sql("""
  SELECT user_id, name, order_count, total_spent
  FROM users
  JOIN orders ON users.id = orders.user_id
  GROUP BY user_id, name
""")
```

**Data characteristics:**
- ✅ Tabular (flat row structure)
- ✅ Uniform schema (all rows same fields)
- ✅ Shallow nesting (max 1-2 levels for joins)
- ✅ Large scale (hundreds to millions of rows)

**Benchmark verdict:** **90.5% accuracy**, 22% token savings for users case

**Conclusion:** Spark SQL results are **TOON-aligned by default**. This is our core use case.

### Finding #2: The Scaling Hypothesis Validates Spark

**From Benchmark:**
> "TOON's true efficiency potential likely follows a non-linear curve, shining only beyond a specific point where the cumulative syntax savings of large datasets amortize the initial prompt overhead."

**Spark's Reality:**
- Spark queries often return **thousands to millions of rows**
- Typical use: Aggregate analytics, ETL outputs, batch processing
- Output size: **10KB - 100MB+ per query**

**Break-even analysis:**
```
Small dataset (< 1KB): JSON wins (prompt tax too high)
Medium dataset (1-10KB): TOON competitive
Large dataset (> 10KB): TOON wins ✅ ← Spark lives here
```

**Conclusion:** Spark's scale perfectly matches TOON's break-even point.

### Finding #3: Streaming = Uniform Tabular Data

**Databricks Real-Time Streaming (2025):**
- Millisecond-level stream processing with Structured Streaming
- Delta Lake ACID compliance for streaming writes
- DLT pipeline monitoring with Auto Loader

**Stream characteristics:**
- ✅ Fixed schema (Kafka topics, event streams)
- ✅ Uniform structure (all events same shape)
- ✅ High volume (continuous flow)
- ✅ Shallow nesting (event payload typically flat)

**Benchmark verdict:** Aligned domain → TOON works

**Use case:**
```scala
import io.toonformat.toon4s.spark.SparkToonOps._

val stream = spark.readStream
  .format("kafka")
  .option("subscribe", "user_events")
  .load()
  .selectExpr("CAST(value AS STRING)")
  .as[UserEvent]

// Aggregate streaming data, encode to TOON for LLM analysis
stream
  .groupBy(window($"timestamp", "1 hour"))
  .agg(count("*").as("event_count"))
  .toDF()
  .toToon(key = "hourly_metrics") // ✅ TOON-aligned
```

**Conclusion:** Streaming analytics pipelines are TOON-aligned.

### Finding #4: Avoid Complex Aggregations with Deep Nesting

**From Benchmark:**
> "For data representing complex state trees, DOM-like structures, or any deeply nested configuration, TOON is NOT in any way production-ready." (Page 9)

**Spark anti-patterns for TOON:**
```scala
// ❌ DON'T: Deep nested aggregations
val complexNested = spark.sql("""
  SELECT
    company.id,
    struct(
      company.name,
      array_agg(struct(
        dept.name,
        array_agg(struct(
          emp.name,
          array_agg(projects) as projects
        )) as employees
      )) as departments
    ) as org_tree
  FROM companies, departments, employees, projects
  GROUP BY company.id
""")

complexNested.toToon() // ❌ WILL FAIL (0% accuracy like company case)
```

**Spark best practices for TOON:**
```scala
// ✅ DO: Flat aggregations
val flatResults = spark.sql("""
  SELECT
    company_id,
    dept_name,
    COUNT(emp_id) as employee_count,
    SUM(salary) as total_payroll
  FROM employees
  JOIN departments ON emp.dept_id = dept.id
  GROUP BY company_id, dept_name
""")

flatResults.toToon() // ✅ WILL WORK (aligned domain)
```

**Guideline:** Keep Spark aggregations **1-2 levels deep max** for TOON.

---

## Part 2: Spark Ecosystem Integration (2025 Research)

### Databricks Lakehouse Architecture

**Key Components:**
1. **Delta Lake** - ACID transactional table format
2. **Structured Streaming** - Real-time data processing
3. **Unity Catalog** - Centralized metadata & governance
4. **Liquid Clustering** - Adaptive data layout for high cardinality

**Integration Strategy for toon4s-spark:**

#### Delta Lake Integration
```scala
import io.toonformat.toon4s.spark.SparkToonOps._

// Read from Delta table
val deltaTable = spark.read.format("delta").load("/mnt/delta/users")

// Encode to TOON for LLM analysis
deltaTable.toToon(key = "delta_users") match {
  case Right(toonChunks) =>
    toonChunks.foreach(chunk => sendToLLM(chunk))
  case Left(error) =>
    logger.error(s"TOON encoding failed: ${error.message}")
}
```

**Why This Works:**
- Delta tables are tabular by nature ✅
- Time travel enables historical TOON snapshots
- ACID guarantees ensure consistent encoding
- Liquid clustering optimizes for TOON chunking

**Source:** [Databricks Best Practices 2025](https://docs.databricks.com/aws/en/lakehouse-architecture/performance-efficiency/best-practices)

#### Structured Streaming Integration
```scala
import io.toonformat.toon4s.spark.SparkDatasetOps._

case class SensorReading(sensor_id: Int, temp: Double, timestamp: Long)

val stream = spark.readStream
  .format("delta")
  .option("readChangeFeed", "true")
  .load("/mnt/delta/sensors")
  .as[SensorReading]

// Micro-batch aggregation + TOON encoding
stream
  .groupBy(window($"timestamp", "5 minutes"), $"sensor_id")
  .agg(avg("temp").as("avg_temp"))
  .writeStream
  .foreachBatch { (batchDF, batchId) =>
    batchDF.toToon(key = s"batch_$batchId") match {
      case Right(toon) => logAnalytics(toon)
      case Left(err) => // Handle
    }
  }
  .start()
```

**Why This Works:**
- Micro-batches are **uniform tabular data** ✅
- Real-time mode = millisecond latency (Databricks RT 16.4+)
- Aligned with TOON's sweet spot

**Source:** [Databricks Real-Time Streaming 2025](https://prolifics.com/usa/resource-center/news/databricks-real-time-streaming-apache-spark)

### Apache Iceberg Integration

**Iceberg Characteristics (2025):**
- Vendor-neutral table format
- Multi-engine support (Spark, Trino, Flink, Presto)
- Time travel & schema evolution
- Partition evolution

**toon4s-spark Integration:**

```scala
import io.toonformat.toon4s.spark.SparkToonOps._

// Read Iceberg table
val icebergTable = spark.read
  .format("iceberg")
  .load("db.analytics.user_metrics")

// TOON encoding for cross-engine analytics
icebergTable.toToon(key = "iceberg_metrics") match {
  case Right(toonChunks) =>
    // Send to LLM for cross-platform analysis
    // LLM can process data from Iceberg (via Spark) + Trino + Flink
    toonChunks.foreach(chunk => analyzeWithLLM(chunk))
  case Left(error) =>
    logger.error(s"Encoding failed: ${error.message}")
}
```

**Strategic Value:**
- **Iceberg's multi-engine support** + **TOON's LLM format** = **Universal Analytics Language**
- Query with Spark → Encode to TOON → Analyze with LLM → Share results across engines
- Time travel enables historical TOON snapshots for trend analysis

**Source:** [Apache Iceberg vs Delta Lake 2025](https://www.kai-waehner.de/blog/2025/11/19/data-streaming-meets-lakehouse-apache-iceberg-for-unified-real-time-and-batch-analytics/)

### DuckDB Integration

**DuckDB Characteristics (2025):**
- Embedded OLAP database (no server)
- Vectorized execution (sub-second queries)
- Native Delta Lake support
- Parquet + Arrow integration

**Why DuckDB + TOON is Powerful:**

```scala
// DuckDB for local analytics, Spark for distributed
import io.toonformat.toon4s.spark.SparkToonOps._

// Spark: Extract large dataset
val sparkResults = spark.sql("""
  SELECT user_id, SUM(revenue) as total_revenue
  FROM transactions
  GROUP BY user_id
  ORDER BY total_revenue DESC
  LIMIT 10000
""")

// Encode to TOON for sharing
sparkResults.toToon(key = "top_users") match {
  case Right(toonChunks) =>
    // Save TOON for DuckDB local analysis
    saveToonForDuckDB(toonChunks)

    // Send same TOON to LLM for insights
    toonChunks.foreach(sendToLLM)
  case Left(err) => // Handle
}
```

**Strategic Value:**
- Spark handles **distributed scale** (TB+)
- DuckDB handles **local analytics** (GB)
- **TOON = portable format** between both
- Benchmark shows TOON works for **tabular aggregations** (users case: 90.5% accuracy)

**Source:** [DuckDB vs Spark 2025](https://medium.com/@mamidipaka2003/duckdb-vs-apache-spark-the-fall-of-the-cluster-c28bea3e4d38)

---

## Part 3: Recommended toon4s-spark Enhancements

### Enhancement #1: Automatic "TOON-Aligned" Detection

**Problem:** Users may try to encode non-aligned data (deep hierarchies)

**Solution:** Add schema analyzer to warn users

```scala
package io.toonformat.toon4s.spark

import org.apache.spark.sql.types._

object ToonAlignmentAnalyzer {

  case class AlignmentScore(
    score: Double, // 0.0 - 1.0
    aligned: Boolean,
    warnings: List[String],
    recommendation: String
  )

  /**
   * Analyze DataFrame schema for TOON alignment.
   *
   * Based on TOON Generation Benchmark findings:
   * - Aligned: Tabular, uniform arrays, shallow nesting (< 3 levels)
   * - Non-aligned: Deep hierarchies, recursive structures
   *
   * @param schema DataFrame schema to analyze
   * @return Alignment score and recommendations
   */
  def analyzeSchema(schema: StructType): AlignmentScore = {
    val maxDepth = calculateMaxDepth(schema)
    val hasArrays = containsArrays(schema)
    val hasComplexNesting = hasDeepStructs(schema)

    val score = calculateScore(maxDepth, hasArrays, hasComplexNesting)
    val aligned = score >= 0.7

    val warnings = List.newBuilder[String]
    if (maxDepth > 3) warnings += s"Deep nesting detected ($maxDepth levels). TOON works best with ≤2 levels."
    if (hasComplexNesting) warnings += "Complex nested structures detected. Consider flattening for better TOON generation."

    val recommendation = if (aligned) {
      "✅ Schema is TOON-aligned. Expected accuracy: 75-90%"
    } else if (score >= 0.5) {
      "⚠️ Schema is partially aligned. Consider flattening or use JSON for complex parts."
    } else {
      "❌ Schema is NOT TOON-aligned. Recommend using JSON instead."
    }

    AlignmentScore(score, aligned, warnings.result(), recommendation)
  }

  private def calculateMaxDepth(dataType: DataType, currentDepth: Int = 0): Int = dataType match {
    case StructType(fields) =>
      if (fields.isEmpty) currentDepth
      else fields.map(f => calculateMaxDepth(f.dataType, currentDepth + 1)).max
    case ArrayType(elementType, _) =>
      calculateMaxDepth(elementType, currentDepth + 1)
    case MapType(_, valueType, _) =>
      calculateMaxDepth(valueType, currentDepth + 1)
    case _ => currentDepth
  }

  private def containsArrays(schema: StructType): Boolean = {
    schema.fields.exists {
      case StructField(_, ArrayType(_, _), _, _) => true
      case _ => false
    }
  }

  private def hasDeepStructs(schema: StructType): Boolean = {
    calculateMaxDepth(schema) > 2
  }

  private def calculateScore(maxDepth: Int, hasArrays: Boolean, hasComplexNesting: Boolean): Double = {
    var score = 1.0

    // Penalize deep nesting (benchmark: company case failed at 0%)
    if (maxDepth > 3) score -= 0.5
    else if (maxDepth > 2) score -= 0.2

    // Penalize complex nesting
    if (hasComplexNesting) score -= 0.3

    // Slight penalty for arrays (can cause count mismatches)
    if (hasArrays) score -= 0.1

    math.max(0.0, score)
  }
}
```

**Usage:**
```scala
import io.toonformat.toon4s.spark.ToonAlignmentAnalyzer._

val df = spark.read.parquet("complex_data.parquet")

val alignment = analyzeSchema(df.schema)
println(alignment.recommendation)

alignment.warnings.foreach(w => logger.warn(w))

if (alignment.aligned) {
  df.toToon() // Safe to use TOON
} else {
  df.toJSON.collect() // Fall back to JSON
}
```

### Enhancement #2: Adaptive Chunking for Prompt Tax Optimization

**Problem:** Benchmark shows prompt tax hurts small datasets

**Solution:** Intelligent chunking based on dataset size

```scala
package io.toonformat.toon4s.spark

object AdaptiveChunking {

  /**
   * Calculate optimal chunk size to amortize TOON prompt tax.
   *
   * Based on benchmark findings:
   * - Small chunks (< 1KB): Prompt tax > savings → BAD
   * - Large chunks (> 10KB): Savings > prompt tax → GOOD
   *
   * @param totalRows Total number of rows
   * @param avgRowSize Average row size in bytes
   * @return Optimal chunk size
   */
  def calculateOptimalChunkSize(totalRows: Long, avgRowSize: Int): Int = {
    val totalDataSize = totalRows * avgRowSize

    // Benchmark break-even: ~10KB per chunk
    val targetChunkSize = 10 * 1024 // 10KB
    val rowsPerChunk = math.max(1, targetChunkSize / avgRowSize)

    if (totalDataSize < 1024) {
      // Very small dataset: Use JSON instead
      logger.warn("Dataset too small for TOON efficiency. Consider JSON.")
      Int.MaxValue // Single chunk (fall back to JSON)
    } else if (totalDataSize < 10 * 1024) {
      // Small dataset: Large chunks to amortize prompt tax
      math.max(100, rowsPerChunk)
    } else {
      // Large dataset: Standard chunking
      math.min(1000, rowsPerChunk)
    }
  }
}
```

### Enhancement #3: Integration with Delta Lake Change Data Feed

**Use Case:** Real-time TOON streaming from Delta Lake CDC

```scala
package io.toonformat.toon4s.spark.integrations

import io.toonformat.toon4s.spark.SparkToonOps._

object DeltaLakeCDC {

  /**
   * Stream Delta Lake changes as TOON-encoded batches.
   *
   * Optimized for Databricks Delta Lake Change Data Feed (CDF).
   * CDF provides row-level changes (inserts, updates, deletes).
   *
   * @param tablePath Delta Lake table path
   * @param startVersion Starting version for CDC
   */
  def streamChangesAsToon(
    tablePath: String,
    startVersion: Long
  )(implicit spark: SparkSession): Unit = {

    spark.readStream
      .format("delta")
      .option("readChangeFeed", "true")
      .option("startingVersion", startVersion)
      .load(tablePath)
      .writeStream
      .foreachBatch { (batchDF, batchId) =>
        // Encode each CDC batch to TOON
        batchDF.toToon(key = s"cdc_batch_$batchId") match {
          case Right(toonChunks) =>
            // Send to LLM for real-time insights
            toonChunks.foreach { chunk =>
              sendToLLMStream(chunk)
            }
          case Left(error) =>
            logger.error(s"CDC TOON encoding failed: ${error.message}")
        }
      }
      .start()
      .awaitTermination()
  }
}
```

**Why This Works:**
- CDC batches are **uniform tabular data** (same schema) ✅
- Row-level changes = **shallow structure** ✅
- Real-time streaming = **continuous TOON generation** ✅
- Aligned with benchmark's "aligned domain"

**Source:** [Databricks Structured Streaming](https://docs.databricks.com/aws/en/structured-streaming/)

### Enhancement #4: Iceberg Time Travel + TOON Snapshots

**Use Case:** Historical analytics with LLM

```scala
package io.toonformat.toon4s.spark.integrations

import io.toonformat.toon4s.spark.SparkToonOps._

object IcebergTimeTravel {

  /**
   * Generate TOON snapshots from Iceberg table history.
   *
   * Enables LLM to analyze trends across time periods.
   */
  def generateHistoricalToonSnapshots(
    tableName: String,
    timestamps: Seq[Long]
  )(implicit spark: SparkSession): Map[Long, Either[SparkToonError, Vector[String]]] = {

    timestamps.map { ts =>
      val snapshot = spark.read
        .format("iceberg")
        .option("as-of-timestamp", ts)
        .load(tableName)

      val toonResult = snapshot.toToon(key = s"snapshot_$ts")

      ts -> toonResult
    }.toMap
  }

  /**
   * Compare two Iceberg snapshots via TOON.
   *
   * Sends both snapshots to LLM for diff analysis.
   */
  def compareToonSnapshots(
    tableName: String,
    timestamp1: Long,
    timestamp2: Long
  )(implicit spark: SparkSession): String = {

    val snapshots = generateHistoricalToonSnapshots(tableName, Seq(timestamp1, timestamp2))

    (snapshots(timestamp1), snapshots(timestamp2)) match {
      case (Right(toon1), Right(toon2)) =>
        // Send both to LLM for comparison
        s"""
        |Analyze the difference between these two snapshots:
        |
        |=== Snapshot 1 (${timestamp1}) ===
        |${toon1.mkString("\n")}
        |
        |=== Snapshot 2 (${timestamp2}) ===
        |${toon2.mkString("\n")}
        |
        |Provide insights on what changed.
        """.stripMargin
      case _ =>
        "Snapshot encoding failed"
    }
  }
}
```

**Strategic Value:**
- Iceberg's time travel + TOON = **historical LLM analytics**
- Compare snapshots via TOON for **trend detection**
- Aligned with TOON's tabular sweet spot

**Source:** [2025 Apache Iceberg Guide](https://blog.datalakehouse.help/posts/2025-01-2025-comprehensive-apache-iceberg-guide/)

---

## Part 4: Documentation Updates

### Update README with Benchmark Data

**Add Section: "When to Use TOON vs JSON"**

```markdown
## When to Use TOON vs JSON

Based on rigorous benchmarking ([TOON Generation Benchmark](https://github.com/vetertann/TOON-generation-benchmark)):

### ✅ Use TOON When:

1. **Tabular Data** (SQL query results, analytics aggregations)
   - Expected accuracy: 90.5%
   - Token savings: 22% vs JSON
   - Example: `SELECT user_id, name, order_count FROM users`

2. **Uniform Arrays** (event streams, log aggregations)
   - Expected accuracy: 75-90%
   - Aligned with Structured Streaming use cases

3. **Large Datasets** (> 10KB output)
   - TOON's cumulative savings amortize prompt overhead
   - Break-even: ~10KB dataset size

4. **Shallow Nesting** (≤ 2 levels deep)
   - Joins with 1-2 tables
   - Simple nested structures

### ❌ Use JSON When:

1. **Deep Hierarchies** (> 3 levels nesting)
   - TOON one-shot accuracy: 0%
   - Repair loops expensive

2. **Complex Nested Structures** (recursive trees, DOM-like data)
   - Not production-ready for TOON generation

3. **Small Datasets** (< 1KB output)
   - TOON prompt tax > token savings

4. **Unknown Schema** (dynamic or evolving structures)
   - TOON requires stable, predictable schemas

### 🎯 Consider JSON-SO (Constrained Decoding) When:

1. **Simple Tabular Data** with **strong model**
   - JSON-SO beats TOON for simple structures
   - Example: users case (556 tokens vs TOON's 840)

2. **Weak Models** needing **guardrails**
   - Constrained decoding prevents nonsense generation

**See**: [Benchmark Analysis](docs/internals/TOON_GENERATION_BENCHMARK_ANALYSIS.md)
```

### Add Example: Spark Streaming + TOON

```markdown
### Real-Time Analytics with Structured Streaming

```scala
import io.toonformat.toon4s.spark.SparkDatasetOps._

case class UserEvent(user_id: Int, event_type: String, timestamp: Long)

val stream = spark.readStream
  .format("kafka")
  .option("subscribe", "user_events")
  .load()
  .selectExpr("CAST(value AS STRING) as json")
  .select(from_json($"json", schema).as("data"))
  .select("data.*")
  .as[UserEvent]

// Micro-batch aggregation + TOON encoding
stream
  .groupBy(window($"timestamp", "5 minutes"), $"event_type")
  .agg(count("*").as("event_count"))
  .writeStream
  .foreachBatch { (batchDF, batchId) =>
    batchDF.toToon(key = s"events_batch_$batchId") match {
      case Right(toonChunks) =>
        // Send to LLM for real-time insights
        toonChunks.foreach(chunk => analyzeWithLLM(chunk))
      case Left(error) =>
        logger.error(s"TOON encoding failed: ${error.message}")
    }
  }
  .start()
```
```

---

## Part 5: Critical Production Guidelines

### Guideline #1: Schema Validation Before Encoding

**Based on benchmark finding:** TOON fails on non-aligned schemas

```scala
import io.toonformat.toon4s.spark.ToonAlignmentAnalyzer._

// ALWAYS validate schema first
val df = spark.sql("SELECT * FROM complex_table")

val alignment = analyzeSchema(df.schema)

if (!alignment.aligned) {
  logger.warn(s"Schema not TOON-aligned: ${alignment.recommendation}")
  // Use JSON instead
  df.toJSON.collect()
} else {
  // Safe to use TOON
  df.toToon()
}
```

### Guideline #2: Monitor Generation Accuracy

**Based on benchmark finding:** Repair loops are expensive

```scala
// Track TOON generation success rate
case class ToonMetrics(
  totalEncodes: Long,
  successfulEncodes: Long,
  failedEncodes: Long,
  avgTokens: Double
)

def trackToonUsage(result: Either[SparkToonError, Vector[String]]): Unit = {
  result match {
    case Right(toon) =>
      metrics.increment("toon.success")
      metrics.gauge("toon.tokens", toon.map(_.length).sum)
    case Left(error) =>
      metrics.increment("toon.failure")
      logger.error(s"TOON encoding failed: ${error.message}")
  }
}
```

### Guideline #3: Hybrid Approach for Mixed Schemas

**Best Practice:** Use TOON for aligned parts, JSON for complex parts

```scala
val df = spark.sql("""
  SELECT
    user_id,
    name,
    email,
    TO_JSON(complex_metadata) as metadata_json
  FROM users
""")

// Flatten complex field to JSON string, rest as TOON
df.toToon(key = "users_hybrid") // ✅ Works because complex part is pre-serialized
```

---

## Conclusion: The Strategic Positioning

### What We Learned

1. **TOON is NOT a general JSON replacement** (benchmark proved this)
2. **TOON has a specific sweet spot** (tabular, uniform, shallow, large-scale)
3. **Spark's core use cases PERFECTLY MATCH TOON's sweet spot**
4. **2025 Spark ecosystem** (Databricks, Iceberg, Delta Lake, DuckDB) **all produce tabular data**

### What We Must Do

1. **Be Honest in Documentation**
   - Show benchmark data
   - Explain TOON's limitations
   - Guide users to right format choice

2. **Build Guardrails**
   - Schema alignment detection
   - Adaptive chunking
   - Monitoring & metrics

3. **Focus on Core Strengths**
   - SQL query results → TOON
   - Streaming analytics → TOON
   - ETL pipelines → TOON
   - Complex hierarchies → JSON

4. **Integrate with Ecosystem**
   - Delta Lake CDC → TOON streams
   - Iceberg time travel → TOON snapshots
   - DuckDB interop → TOON portability

### The Opportunity

**toon4s-spark is positioned at the intersection of:**
- ✅ Spark's tabular data strength
- ✅ TOON's tabular efficiency
- ✅ LLM's need for token-efficient context
- ✅ 2025 lakehouse architecture (Delta, Iceberg)

This is **not** about replacing JSON everywhere.

This is about **owning the tabular analytics → LLM pipeline**.

**That's a big enough market.**

---

## References

### Benchmark
- [TOON Generation Benchmark](https://github.com/vetertann/TOON-generation-benchmark) by Ivan Matveev
- [Benchmark Analysis](docs/internals/TOON_GENERATION_BENCHMARK_ANALYSIS.md)

### Spark Ecosystem 2025
- [Databricks Best Practices](https://docs.databricks.com/aws/en/lakehouse-architecture/performance-efficiency/best-practices)
- [Databricks Real-Time Streaming](https://prolifics.com/usa/resource-center/news/databricks-real-time-streaming-apache-spark)
- [Apache Iceberg vs Delta Lake 2025](https://www.kai-waehner.de/blog/2025/11/19/data-streaming-meets-lakehouse-apache-iceberg-for-unified-real-time-and-batch-analytics/)
- [2025 Apache Iceberg Guide](https://blog.datalakehouse.help/posts/2025-01-2025-comprehensive-apache-iceberg-guide/)
- [DuckDB vs Spark 2025](https://medium.com/@mamidipaka2003/duckdb-vs-apache-spark-the-fall-of-the-cluster-c28bea3e4d38)
- [DuckDB + Delta Lake](https://gurditsingh.github.io/blog/2024/06/30/duckdb_deltalake.html)

---

**This strategy document will guide all future toon4s-spark development.**

**We know our sweet spot. We own it.**
