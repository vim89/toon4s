package io.toonformat.toon4s.visitor

import scala.collection.immutable.VectorMap

/**
 * Zero-overhead tree processing with the visitor pattern.
 *
 * The Visitor pattern enables flexible, streaming, zero-overhead processing of JSON-compatible
 * structures by separating traversal logic from computation logic.
 *
 * ==Key benefits==
 *   - '''Zero intermediate structures''': Chain transformations without creating temporary trees
 *   - '''Composability''': Visitors can be chained together for complex transformations
 *   - '''Streaming''': Process data as it arrives, without loading entire tree into memory
 *   - '''Type safety''': Generic type `T` ensures type-correct composition
 *
 * ==Design==
 * The visitor pattern consists of three components:
 *   1. [[Visitor]] - Defines operations for each node type (primitives and objects)
 *   2. [[ObjectVisitor]] - Handles object traversal with key-value pairs
 *   3. [[Dispatch]] - Traverses structures and calls visitor methods
 *
 * ==Usage pattern==
 * {{{
 * import io.toonformat.toon4s.visitor._
 * import io.toonformat.toon4s.JsonValue._
 *
 * // Create a visitor
 * val visitor = new StringifyVisitor(indent = 2)
 *
 * // Dispatch to visitor
 * val result: String = Dispatch(jsonValue, visitor)
 *
 * // Or chain visitors
 * val chained = new ValidationVisitor(
 *   new TransformVisitor(
 *     new StringifyVisitor(indent = 2)
 *   )
 * )
 * }}}
 *
 * ==Visitor types==
 *   - '''Terminal visitors''': Produce final output (StringifyVisitor, SummationVisitor)
 *   - '''Intermediate visitors''': Transform and forward to downstream (ValidationVisitor,
 *     TransformVisitor)
 *
 * @tparam T
 *   The output type produced by this visitor
 *
 * @see
 *   [[ObjectVisitor]] for handling object traversal
 * @see
 *   [[Dispatch]] for the universal dispatcher
 * @see
 *   Li Haoyi's article:
 *   [[https://www.lihaoyi.com/post/ZeroOverheadTreeProcessingwiththeVisitorPattern.html]]
 */
abstract class Visitor[T] {

  /**
   * Visit a string value.
   *
   * @param value
   *   The string content
   * @return
   *   The visitor's output type T
   */
  def visitString(value: String): T

  /**
   * Visit a numeric value.
   *
   * @param value
   *   The numeric value as BigDecimal
   * @return
   *   The visitor's output type T
   */
  def visitNumber(value: BigDecimal): T

  /**
   * Visit a boolean value.
   *
   * @param value
   *   The boolean value (true or false)
   * @return
   *   The visitor's output type T
   */
  def visitBool(value: Boolean): T

  /**
   * Visit a null value.
   *
   * @return
   *   The visitor's output type T
   */
  def visitNull(): T

  /**
   * Visit an array value.
   *
   * This method receives the complete array for simple batch processing. For streaming array
   * processing, future enhancements may add an ArrayVisitor pattern similar to ObjectVisitor.
   *
   * @param elements
   *   The array elements as a vector
   * @return
   *   The visitor's output type T
   */
  def visitArray(elements: Vector[T]): T

  /**
   * Begin visiting an object.
   *
   * Returns an ObjectVisitor that will handle the key-value pairs.
   *
   * @return
   *   An ObjectVisitor[T] for processing object fields
   */
  def visitObject(): ObjectVisitor[T]

}

/**
 * Visitor for processing object key-value pairs.
 *
 * ObjectVisitor enables streaming object traversal with explicit lifecycle:
 *   1. Call `visitKey(key)` to notify of the next key
 *   1. Call `visitValue()` to get a Visitor for the value
 *   1. Process the value recursively via dispatch
 *   1. Call `visitValue(result)` with the processed value
 *   1. Repeat for all key-value pairs
 *   1. Call `done()` to finalize the object
 *
 * ==Usage example==
 * {{{
 * val objVisitor = visitor.visitObject()
 * for ((key, value) <- fields) {
 *   objVisitor.visitKey(key)
 *   val valueVisitor = objVisitor.visitValue()
 *   val result = Dispatch(value, valueVisitor)
 *   objVisitor.visitValue(result)
 * }
 * val finalResult = objVisitor.done()
 * }}}
 *
 * @tparam T
 *   The output type matching the parent Visitor[T]
 */
abstract class ObjectVisitor[T] {

  /**
   * Visit a key in the object.
   *
   * Called before processing the associated value. Implementations may use this to track state
   * (e.g., filtering keys, tracking last key for special formatting).
   *
   * @param key
   *   The field name
   */
  def visitKey(key: String): Unit

  /**
   * Get a visitor for processing the next value.
   *
   * Called after visitKey and before processing the value. Returns a Visitor[T] that will be passed
   * to the dispatcher for recursive traversal.
   *
   * @return
   *   A Visitor[T] for processing the value
   */
  def visitValue(): Visitor[T]

  /**
   * Provide the processed value result.
   *
   * Called after the value has been recursively processed via dispatch. Receives the result from
   * the value visitor.
   *
   * @param value
   *   The processed value of type T
   */
  def visitValue(value: T): Unit

  /**
   * Finalize the object and produce the result.
   *
   * Called after all key-value pairs have been visited. Returns the final output of type T.
   *
   * @return
   *   The final result of visiting this object
   */
  def done(): T

}
