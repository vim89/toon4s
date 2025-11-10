package io.toonformat.toon4s.visitor

import scala.collection.immutable.VectorMap

/**
 * Visitor that constructs arbitrary tree structures from TOON/JsonValue.
 *
 * This is the reverse of TreeWalker: '''decode TOON directly to Jackson JsonNode, Circe Json, or
 * any tree without JsonValue intermediate'''.
 *
 * ==Why this matters==
 * Standard flow creates 2 trees:
 *   1. TOON string → JsonValue (toon4s AST)
 *   2. JsonValue → JsonNode (Jackson AST)
 *
 * TreeConstructionVisitor eliminates step 1:
 *   - '''Direct decode''': TOON string → Visitor[JsonNode] → Jackson JsonNode
 *   - '''Zero overhead''': No JsonValue allocation
 *
 * ==Real-world use case==
 * {{{
 * // You need Jackson JsonNode for REST API
 * import com.fasterxml.jackson.databind.ObjectMapper
 * import com.fasterxml.jackson.databind.node.{ObjectNode, ArrayNode, JsonNodeFactory}
 *
 * val toonString = """
 * user:
 *   name: Ada
 *   tags[2]: reading,gaming
 * """
 *
 * // Decode directly to Jackson without JsonValue
 * val factory = JsonNodeFactory.instance
 * val visitor = TreeConstructionVisitor.forJackson(factory)
 * val decoded = Toon.decode(toonString).map(Dispatch(_, visitor))
 * // Result: Jackson JsonNode ready for objectMapper.writeValue()
 * }}}
 *
 * @tparam Tree
 *   The tree type to construct (JsonNode, io.circe.Json, etc.)
 */
abstract class TreeConstructionVisitor[Tree] extends Visitor[Tree] {

  /**
   * Construct a string node.
   *
   * @param value
   *   String value
   * @return
   *   Tree node representing string
   */
  def makeString(value: String): Tree

  /**
   * Construct a number node.
   *
   * @param value
   *   Number value
   * @return
   *   Tree node representing number
   */
  def makeNumber(value: BigDecimal): Tree

  /**
   * Construct a boolean node.
   *
   * @param value
   *   Boolean value
   * @return
   *   Tree node representing boolean
   */
  def makeBool(value: Boolean): Tree

  /**
   * Construct a null node.
   *
   * @return
   *   Tree node representing null
   */
  def makeNull(): Tree

  /**
   * Construct an array node.
   *
   * @param elements
   *   Array elements
   * @return
   *   Tree node representing array
   */
  def makeArray(elements: Vector[Tree]): Tree

  /**
   * Construct an object node.
   *
   * @param fields
   *   Object fields
   * @return
   *   Tree node representing object
   */
  def makeObject(fields: VectorMap[String, Tree]): Tree

  // Visitor implementation delegates to make* methods

  override def visitString(value: String): Tree = makeString(value)

  override def visitNumber(value: BigDecimal): Tree = makeNumber(value)

  override def visitBool(value: Boolean): Tree = makeBool(value)

  override def visitNull(): Tree = makeNull()

  override def visitArray(elements: Vector[Tree]): Tree = makeArray(elements)

  override def visitObject(): ObjectVisitor[Tree] = {
    new TreeConstructionObjectVisitor(this)
  }

}

/** ObjectVisitor for tree construction. */
final class TreeConstructionObjectVisitor[Tree](parent: TreeConstructionVisitor[Tree])
    extends ObjectVisitor[Tree] {

  private val fields = scala.collection.mutable.LinkedHashMap.empty[String, Tree]

  private var lastKey: String = ""

  override def visitKey(key: String): Unit = {
    lastKey = key
  }

  override def visitValue(): Visitor[Tree] = parent

  override def visitValue(value: Tree): Unit = {
    fields(lastKey) = value
  }

  override def done(): Tree = {
    parent.makeObject(VectorMap.from(fields))
  }

}

/** Companion object with adapter examples. */
object TreeConstructionVisitor {

  /**
   * Example adapter for constructing Jackson JsonNode.
   *
   * '''IMPORTANT''': This is documentation only. To use it:
   *   1. Add Jackson dependency to your build.sbt
   *   2. Copy this implementation to your codebase
   *   3. Uncomment and use
   *
   * {{{
   * // Add to build.sbt:
   * // libraryDependencies += "com.fasterxml.jackson.core" % "jackson-databind" % "2.17.0"
   *
   * import com.fasterxml.jackson.databind.JsonNode
   * import com.fasterxml.jackson.databind.node.{JsonNodeFactory, ObjectNode, ArrayNode}
   * import scala.jdk.CollectionConverters._
   *
   * class JacksonConstructionVisitor(factory: JsonNodeFactory)
   *     extends TreeConstructionVisitor[JsonNode] {
   *
   *   override def makeString(value: String): JsonNode =
   *     factory.textNode(value)
   *
   *   override def makeNumber(value: BigDecimal): JsonNode =
   *     factory.numberNode(value.bigDecimal)
   *
   *   override def makeBool(value: Boolean): JsonNode =
   *     factory.booleanNode(value)
   *
   *   override def makeNull(): JsonNode =
   *     factory.nullNode()
   *
   *   override def makeArray(elements: Vector[JsonNode]): JsonNode = {
   *     val arr = factory.arrayNode()
   *     elements.foreach(arr.add)
   *     arr
   *   }
   *
   *   override def makeObject(fields: VectorMap[String, JsonNode]): JsonNode = {
   *     val obj = factory.objectNode()
   *     fields.foreach { case (k, v) => obj.set(k, v) }
   *     obj
   *   }
   * }
   *
   * object JacksonConstructionVisitor {
   *   def apply(factory: JsonNodeFactory): JacksonConstructionVisitor =
   *     new JacksonConstructionVisitor(factory)
   * }
   *
   * // Usage:
   * val factory = JsonNodeFactory.instance
   * val visitor = JacksonConstructionVisitor(factory)
   * val toonString = """user:\n  name: Ada\n  age: 30"""
   * val jacksonNode = Toon.decode(toonString).map(Dispatch(_, visitor))
   * }}}
   */
  def forJacksonExample: String =
    """
       See TreeConstructionVisitor scaladoc for Jackson adapter example.
       Add Jackson dependency and copy the adapter to your codebase.
       """

  /**
   * Example adapter for constructing Circe Json.
   *
   * {{{
   * // Add to build.sbt:
   * // libraryDependencies += "io.circe" %% "circe-core" % "0.14.6"
   *
   * import io.circe.Json
   *
   * object CirceConstructionVisitor extends TreeConstructionVisitor[Json] {
   *
   *   override def makeString(value: String): Json =
   *     Json.fromString(value)
   *
   *   override def makeNumber(value: BigDecimal): Json =
   *     Json.fromBigDecimal(value)
   *
   *   override def makeBool(value: Boolean): Json =
   *     Json.fromBoolean(value)
   *
   *   override def makeNull(): Json =
   *     Json.Null
   *
   *   override def makeArray(elements: Vector[Json]): Json =
   *     Json.fromValues(elements)
   *
   *   override def makeObject(fields: VectorMap[String, Json]): Json =
   *     Json.fromFields(fields.toSeq)
   * }
   *
   * // Usage:
   * val toonString = """user:\n  name: Ada"""
   * val circeJson = Toon.decode(toonString).map(Dispatch(_, CirceConstructionVisitor))
   * }}}
   */
  def forCirceExample: String =
    """
See TreeConstructionVisitor scaladoc for Circe adapter example.
Add Circe dependency and copy the adapter to your codebase.
"""

}
