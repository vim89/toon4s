package io.toonformat.toon4s.spark

import io.toonformat.toon4s.JsonValue
import io.toonformat.toon4s.JsonValue._
import munit.FunSuite
import org.apache.spark.sql.{Row, SparkSession}
import org.apache.spark.sql.types._

import scala.collection.immutable.VectorMap

class SparkJsonInteropTest extends FunSuite {

  private var spark: SparkSession = _

  override def beforeAll(): Unit = {
    spark = SparkSession
      .builder()
      .master("local[1]")
      .appName("SparkJsonInteropTest")
      .config("spark.ui.enabled", "false")
      .config("spark.sql.shuffle.partitions", "1")
      .getOrCreate()
  }

  override def afterAll(): Unit = {
    if (spark != null) {
      spark.stop()
    }
  }

  test("rowToJsonValue: convert simple row") {
    val schema = StructType(Seq(
      StructField("id", IntegerType),
      StructField("name", StringType),
      StructField("active", BooleanType)
    ))

    val row = Row(1, "Alice", true)
    val json = SparkJsonInterop.rowToJsonValue(row, schema)

    assertEquals(json, JObj(VectorMap(
      "id" -> JNumber(BigDecimal(1)),
      "name" -> JString("Alice"),
      "active" -> JBool(true)
    )))
  }

  test("rowToJsonValue: handle null values") {
    val schema = StructType(Seq(
      StructField("name", StringType),
      StructField("age", IntegerType)
    ))

    val row = Row("Bob", null)
    val json = SparkJsonInterop.rowToJsonValue(row, schema)

    assertEquals(json, JObj(VectorMap(
      "name" -> JString("Bob"),
      "age" -> JNull
    )))
  }

  test("rowToJsonValue: handle numeric types") {
    val schema = StructType(Seq(
      StructField("int_val", IntegerType),
      StructField("long_val", LongType),
      StructField("double_val", DoubleType),
      StructField("float_val", FloatType)
    ))

    val row = Row(42, 100L, 3.14, 2.71f)
    val json = SparkJsonInterop.rowToJsonValue(row, schema)

    json match {
      case JObj(fields) =>
        assertEquals(fields("int_val"), JNumber(BigDecimal(42)))
        assertEquals(fields("long_val"), JNumber(BigDecimal(100)))
        assert(fields("double_val").isInstanceOf[JNumber])
        assert(fields("float_val").isInstanceOf[JNumber])
      case _ => fail("Expected JObj")
    }
  }

  test("rowToJsonValue: handle nested struct") {
    val innerSchema = StructType(Seq(
      StructField("city", StringType),
      StructField("zip", IntegerType)
    ))

    val schema = StructType(Seq(
      StructField("name", StringType),
      StructField("address", innerSchema)
    ))

    val row = Row("Alice", Row("NYC", 10001))
    val json = SparkJsonInterop.rowToJsonValue(row, schema)

    json match {
      case JObj(fields) =>
        assertEquals(fields("name"), JString("Alice"))
        assert(fields("address").isInstanceOf[JObj])
      case _ => fail("Expected JObj")
    }
  }

  test("rowToJsonValue: handle arrays") {
    val schema = StructType(Seq(
      StructField("tags", ArrayType(StringType))
    ))

    val row = Row(Seq("scala", "spark", "toon"))
    val json = SparkJsonInterop.rowToJsonValue(row, schema)

    json match {
      case JObj(fields) =>
        assertEquals(fields("tags"), JArray(Vector(
          JString("scala"),
          JString("spark"),
          JString("toon")
        )))
      case _ => fail("Expected JObj")
    }
  }

  test("jsonValueToRow: convert simple json") {
    val schema = StructType(Seq(
      StructField("id", IntegerType),
      StructField("name", StringType)
    ))

    val json = JObj(VectorMap(
      "id" -> JNumber(BigDecimal(1)),
      "name" -> JString("Alice")
    ))

    val row = SparkJsonInterop.jsonValueToRow(json, schema)
    assertEquals(row.getInt(0), 1)
    assertEquals(row.getString(1), "Alice")
  }

  test("jsonValueToRow: handle missing fields") {
    val schema = StructType(Seq(
      StructField("id", IntegerType),
      StructField("name", StringType),
      StructField("age", IntegerType)
    ))

    val json = JObj(VectorMap(
      "id" -> JNumber(BigDecimal(1)),
      "name" -> JString("Alice")
    ))

    val row = SparkJsonInterop.jsonValueToRow(json, schema)
    assertEquals(row.getInt(0), 1)
    assertEquals(row.getString(1), "Alice")
    assert(row.isNullAt(2))
  }

  test("rowToJsonValueSafe: handle conversion errors") {
    val schema = StructType(Seq(StructField("id", IntegerType)))
    val row = Row(1)

    val result = SparkJsonInterop.rowToJsonValueSafe(row, schema)
    assert(result.isRight)
  }

  test("jsonValueToRowSafe: handle conversion errors") {
    val schema = StructType(Seq(StructField("id", IntegerType)))
    val json = JObj(VectorMap("id" -> JNumber(BigDecimal(1))))

    val result = SparkJsonInterop.jsonValueToRowSafe(json, schema)
    assert(result.isRight)
  }

  test("jsonValueToRowSafe: fail on non-JObj") {
    val schema = StructType(Seq(StructField("id", IntegerType)))
    val json = JArray(Vector(JNumber(BigDecimal(1))))

    val result = SparkJsonInterop.jsonValueToRowSafe(json, schema)
    assert(result.isLeft)
  }

  test("round-trip: Row -> JsonValue -> Row") {
    val schema = StructType(Seq(
      StructField("id", IntegerType),
      StructField("name", StringType),
      StructField("score", DoubleType)
    ))

    val originalRow = Row(42, "test", 95.5)
    val json = SparkJsonInterop.rowToJsonValue(originalRow, schema)
    val reconstructedRow = SparkJsonInterop.jsonValueToRow(json, schema)

    assertEquals(reconstructedRow.getInt(0), 42)
    assertEquals(reconstructedRow.getString(1), "test")
    assertEquals(reconstructedRow.getDouble(2), 95.5, 0.0001)
  }
}
