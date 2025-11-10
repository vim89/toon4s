package io.toonformat.toon4s.visitor

import scala.collection.immutable.VectorMap

import io.toonformat.toon4s.JsonValue
import io.toonformat.toon4s.JsonValue._

/**
 * Terminal visitor that reconstructs JsonValue trees.
 *
 * This visitor demonstrates the '''identity transformation''' - it rebuilds the exact same
 * JsonValue structure during traversal. While this may seem pointless alone, it becomes powerful
 * when composed with intermediate visitors for transformations.
 *
 * ==Key use cases==
 *   - '''Transformation pipeline endpoint''': Chain with intermediate visitors to produce tree
 *     output
 *   - '''Validation with output''': Validate while reconstructing (fail fast on errors)
 *   - '''Normalization''': Apply transformations and produce normalized tree
 *   - '''Identity benchmark''': Measure pure visitor overhead
 *
 * ==Composition example==
 * {{{
 * import io.toonformat.toon4s.visitor._
 *
 * // Chain: Filter keys → Reconstruct tree
 * val visitor = new FilterKeysVisitor(
 *   keysToRemove = Set("password", "secret"),
 *   downstream = new ConstructionVisitor()
 * )
 * val filtered: JsonValue = Dispatch(json, visitor)
 * }}}
 *
 * ==Performance characteristics==
 *   - '''Time Complexity''': O(n) where n is tree size
 *   - '''Space Complexity''': O(n) for reconstructed tree + O(d) call stack
 *   - '''Allocations''': Same as original tree (case class instances)
 *
 * ==Zero-overhead insight==
 * While ConstructionVisitor allocates the same as the original tree, chaining it with intermediate
 * visitors avoids multiple intermediate trees:
 *   - '''Without visitors''': `filter(transform(normalize(tree)))` creates 3 intermediate trees
 *   - '''With visitors''': `new Filter(new Transform(new Normalize(new Construction())))` creates 1
 *     final tree
 *
 * @see
 *   [[Visitor]] for the visitor pattern interface
 * @see
 *   [[ObjectVisitor]] for object processing
 * @see
 *   [[Dispatch]] for the dispatcher
 */
final class ConstructionVisitor extends Visitor[JsonValue] {

  /**
   * Visit a string value.
   *
   * @param value
   *   The string content
   * @return
   *   A JString containing the value
   */
  override def visitString(value: String): JsonValue = JString(value)

  /**
   * Visit a numeric value.
   *
   * @param value
   *   The numeric value as BigDecimal
   * @return
   *   A JNumber containing the value
   */
  override def visitNumber(value: BigDecimal): JsonValue = JNumber(value)

  /**
   * Visit a boolean value.
   *
   * @param value
   *   The boolean value
   * @return
   *   A JBool containing the value
   */
  override def visitBool(value: Boolean): JsonValue = JBool(value)

  /**
   * Visit a null value.
   *
   * @return
   *   JNull singleton
   */
  override def visitNull(): JsonValue = JNull

  /**
   * Visit an array value.
   *
   * @param elements
   *   The reconstructed array elements
   * @return
   *   A JArray containing the elements
   */
  override def visitArray(elements: Vector[JsonValue]): JsonValue = JArray(elements)

  /**
   * Begin visiting an object.
   *
   * @return
   *   An ObjectVisitor that accumulates key-value pairs
   */
  override def visitObject(): ObjectVisitor[JsonValue] = new ConstructionObjectVisitor()

}

/**
 * ObjectVisitor implementation for reconstructing objects.
 *
 * Accumulates key-value pairs into a VectorMap and produces a JObj.
 */
final class ConstructionObjectVisitor extends ObjectVisitor[JsonValue] {

  private val builder = VectorMap.newBuilder[String, JsonValue]

  private var lastKey: String = ""

  /**
   * Visit a key in the object.
   *
   * Stores the key for use when the value is provided.
   *
   * @param key
   *   The field name
   */
  override def visitKey(key: String): Unit = {
    lastKey = key
  }

  /**
   * Get a visitor for processing the next value.
   *
   * Returns the parent ConstructionVisitor for recursive traversal.
   *
   * @return
   *   A ConstructionVisitor for the value
   */
  override def visitValue(): Visitor[JsonValue] = new ConstructionVisitor()

  /**
   * Provide the processed value result.
   *
   * Adds the key-value pair to the builder.
   *
   * @param value
   *   The reconstructed JsonValue
   */
  override def visitValue(value: JsonValue): Unit = {
    builder += (lastKey -> value)
  }

  /**
   * Finalize the object and produce the result.
   *
   * @return
   *   A JObj containing all accumulated key-value pairs
   */
  override def done(): JsonValue = JObj(builder.result())

}
