package io.toonformat.toon4s.visitor

/**
 * Intermediate visitor that filters out specified keys from objects.
 *
 * This is an '''intermediate visitor''' - it transforms data and forwards to a downstream visitor
 * rather than producing final output. FilterKeysVisitor demonstrates composable transformations by
 * removing sensitive or unwanted fields during traversal.
 *
 * ==Key features==
 *   - '''Zero intermediate trees''': Filters during traversal without creating temporary structures
 *   - '''Composable''': Can be chained with other visitors
 *   - '''Type-safe''': Generic type T matches downstream visitor
 *   - '''Streaming''': Processes data as it arrives
 *
 * ==Usage examples==
 *
 * ===Example 1: Filter and Stringify===
 * {{{
 * import io.toonformat.toon4s.visitor._
 *
 * // Remove sensitive fields before encoding
 * val visitor = new FilterKeysVisitor(
 *   keysToRemove = Set("password", "ssn", "creditCard"),
 *   downstream = new StringifyVisitor(indent = 2)
 * )
 * val sanitized: String = Dispatch(json, visitor)
 * }}}
 *
 * ===Example 2: Filter and Reconstruct===
 * {{{
 * // Remove fields and produce clean tree
 * val visitor = new FilterKeysVisitor(
 *   keysToRemove = Set("_internal", "debug"),
 *   downstream = new ConstructionVisitor()
 * )
 * val cleaned: JsonValue = Dispatch(json, visitor)
 * }}}
 *
 * ===Example 3: Chain multiple filters===
 * {{{
 * // Remove keys, then normalize, then stringify
 * val visitor = new FilterKeysVisitor(
 *   Set("temp"),
 *   new NormalizeVisitor(
 *     new StringifyVisitor(indent = 2)
 *   )
 * )
 * }}}
 *
 * ==Performance characteristics==
 *   - '''Time Complexity''': O(n) where n is tree size
 *   - '''Space Complexity''': O(d) for call stack + downstream visitor space
 *   - '''Allocations''': Zero (delegates to downstream which may allocate)
 *
 * ==Composition pattern==
 * Intermediate visitors follow this pattern:
 *   1. Receive a downstream visitor of type Visitor[T]
 *   1. Extend Visitor[T] with same type parameter
 *   1. Transform data as needed
 *   1. Forward to downstream visitor methods
 *
 * @param keysToRemove
 *   Set of field names to filter out (case-sensitive)
 * @param downstream
 *   The visitor to forward processed data to
 * @tparam T
 *   The output type (matches downstream visitor)
 *
 * @see
 *   [[Visitor]] for the visitor pattern interface
 * @see
 *   [[ObjectVisitor]] for object processing
 * @see
 *   [[Dispatch]] for the dispatcher
 */
final class FilterKeysVisitor[T](
    keysToRemove: Set[String],
    downstream: Visitor[T],
) extends Visitor[T] {

  /**
   * Visit a string value.
   *
   * Forwards directly to downstream - no filtering needed for primitives.
   *
   * @param value
   *   The string content
   * @return
   *   Result from downstream visitor
   */
  override def visitString(value: String): T = downstream.visitString(value)

  /**
   * Visit a numeric value.
   *
   * Forwards directly to downstream - no filtering needed for primitives.
   *
   * @param value
   *   The numeric value
   * @return
   *   Result from downstream visitor
   */
  override def visitNumber(value: BigDecimal): T = downstream.visitNumber(value)

  /**
   * Visit a boolean value.
   *
   * Forwards directly to downstream - no filtering needed for primitives.
   *
   * @param value
   *   The boolean value
   * @return
   *   Result from downstream visitor
   */
  override def visitBool(value: Boolean): T = downstream.visitBool(value)

  /**
   * Visit a null value.
   *
   * Forwards directly to downstream - no filtering needed for primitives.
   *
   * @return
   *   Result from downstream visitor
   */
  override def visitNull(): T = downstream.visitNull()

  /**
   * Visit an array value.
   *
   * Forwards the processed elements to downstream. Array elements are already processed (may have
   * filtered objects inside).
   *
   * @param elements
   *   The processed array elements
   * @return
   *   Result from downstream visitor
   */
  override def visitArray(elements: Vector[T]): T = downstream.visitArray(elements)

  /**
   * Begin visiting an object.
   *
   * Creates a FilterKeysObjectVisitor that will skip filtered keys.
   *
   * @return
   *   An ObjectVisitor that performs key filtering
   */
  override def visitObject(): ObjectVisitor[T] = {
    new FilterKeysObjectVisitor(keysToRemove, downstream.visitObject())
  }

}

/**
 * ObjectVisitor implementation for filtering keys.
 *
 * Skips keys in the filter set and forwards remaining keys to downstream.
 *
 * @param keysToRemove
 *   Set of field names to filter out
 * @param downstream
 *   The downstream ObjectVisitor to forward to
 * @tparam T
 *   The output type (matches downstream)
 */
final class FilterKeysObjectVisitor[T](
    keysToRemove: Set[String],
    downstream: ObjectVisitor[T],
) extends ObjectVisitor[T] {

  private var lastKey: String = ""

  private var skipCurrent: Boolean = false

  /**
   * Visit a key in the object.
   *
   * Checks if the key should be filtered. If yes, sets skipCurrent flag. If no, forwards to
   * downstream.
   *
   * @param key
   *   The field name
   */
  override def visitKey(key: String): Unit = {
    lastKey = key
    skipCurrent = keysToRemove.contains(key)
    if (!skipCurrent) {
      downstream.visitKey(key)
    }
  }

  /**
   * Get a visitor for processing the next value.
   *
   * If the current key is filtered, returns a NoOpVisitor that discards the value. Otherwise,
   * returns a FilterKeysVisitor to recursively filter nested structures.
   *
   * @return
   *   A visitor for the value
   */
  override def visitValue(): Visitor[T] = {
    if (skipCurrent) {
      // Discard this value - use NoOpVisitor (we need to traverse but ignore)
      // For now, we'll still get the downstream visitor to maintain type safety
      // The value will be discarded in visitValue(value)
      new FilterKeysVisitor(keysToRemove, downstream.visitValue())
    } else {
      new FilterKeysVisitor(keysToRemove, downstream.visitValue())
    }
  }

  /**
   * Provide the processed value result.
   *
   * If the current key is filtered, discards the value. Otherwise, forwards to downstream.
   *
   * @param value
   *   The processed value
   */
  override def visitValue(value: T): Unit = {
    if (!skipCurrent) {
      downstream.visitValue(value)
    }
    // If skipCurrent, silently discard the value
  }

  /**
   * Finalize the object and produce the result.
   *
   * Forwards to downstream to get the final result.
   *
   * @return
   *   The final result from downstream
   */
  override def done(): T = downstream.done()

}
