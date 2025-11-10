package io.toonformat.toon4s

/**
 * Zero-overhead tree processing with the Visitor Pattern.
 *
 * This package implements the Visitor Pattern for flexible, composable, zero-overhead processing of
 * JSON-compatible structures, as described in Li Haoyi's article "Zero-Overhead Tree Processing
 * with the Visitor Pattern". Link: Reference:
 * https://www.lihaoyi.com/post/ZeroOverheadTreeProcessingwiththeVisitorPattern.html
 *
 * ==Overview==
 *
 * The Visitor Pattern separates tree traversal from computation logic, enabling:
 *   - '''Zero intermediate structures''': Chain transformations without temporary trees
 *   - '''Composability''': Combine simple visitors to build complex operations
 *   - '''Streaming''': Process data as it arrives without loading entire trees
 *   - '''Type safety''': Generic types ensure correct composition
 *
 * ==Core components==
 *
 * '''[[Visitor]]''': Abstract visitor interface defining operations for each node type (strings,
 * numbers, booleans, null, arrays, objects).
 *
 * '''[[ObjectVisitor]]''': Handles object traversal with explicit key-value lifecycle.
 *
 * '''[[Dispatch]]''': Universal dispatcher that traverses JsonValue structures and calls visitor
 * methods.
 *
 * ==Visitor types==
 *
 * '''Terminal visitors''' (produce final output):
 *   - [[StringifyVisitor]] - Converts to TOON format string
 *   - [[ConstructionVisitor]] - Reconstructs JsonValue trees
 *
 * '''Intermediate visitors''' (transform and forward):
 *   - [[FilterKeysVisitor]] - Removes specified keys from objects
 *
 * ==Usage patterns==
 *
 * ===Pattern 1: Simple dispatch===
 * {{{
 * import io.toonformat.toon4s.visitor._
 * import io.toonformat.toon4s.JsonValue._
 *
 * val json = JObj(VectorMap("name" -> JString("Alice")))
 * val visitor = new StringifyVisitor(indent = 2)
 * val result: String = Dispatch(json, visitor)
 * }}}
 *
 * ===Pattern 2: Chained visitors===
 * {{{
 * // Chain: Filter → Stringify
 * val visitor = new FilterKeysVisitor(
 *   keysToRemove = Set("password"),
 *   downstream = new StringifyVisitor(indent = 2)
 * )
 * val sanitized: String = Dispatch(json, visitor)
 * }}}
 *
 * ===Pattern 3: Multiple transformations===
 * {{{
 * // Chain: Filter → Reconstruct tree
 * val visitor = new FilterKeysVisitor(
 *   Set("internal"),
 *   new ConstructionVisitor()
 * )
 * val cleaned: JsonValue = Dispatch(json, visitor)
 * }}}
 *
 * ==Performance benefits==
 *
 * '''Without visitors''' (multiple intermediate trees):
 * {{{
 * val step1 = filter(json)           // Creates intermediate tree 1
 * val step2 = normalize(step1)       // Creates intermediate tree 2
 * val result = stringify(step2)      // Final result
 * // Total: 2 intermediate trees + 1 final string
 * }}}
 *
 * '''With visitors''' (zero intermediate trees):
 * {{{
 * val visitor = new FilterKeysVisitor(
 *   keys,
 *   new NormalizeVisitor(
 *     new StringifyVisitor(indent = 2)
 *   )
 * )
 * val result = Dispatch(json, visitor)
 * // Total: 0 intermediate trees + 1 final string
 * }}}
 *
 * ==Creating custom visitors==
 *
 * ===Terminal visitor example===
 * {{{
 * final class SummationVisitor extends Visitor[Int] {
 *   def visitString(value: String) = 0
 *   def visitNumber(value: BigDecimal) = value.toInt
 *   def visitBool(value: Boolean) = if (value) 1 else 0
 *   def visitNull() = 0
 *   def visitArray(elements: Vector[Int]) = elements.sum
 *   def visitObject() = new SummationObjectVisitor()
 * }
 * }}}
 *
 * ===Intermediate visitor example===
 * {{{
 * final class UppercaseKeysVisitor[T](downstream: Visitor[T]) extends Visitor[T] {
 *   def visitString(v: String) = downstream.visitString(v)
 *   def visitNumber(v: BigDecimal) = downstream.visitNumber(v)
 *   def visitBool(v: Boolean) = downstream.visitBool(v)
 *   def visitNull() = downstream.visitNull()
 *   def visitArray(elems: Vector[T]) = downstream.visitArray(elems)
 *   def visitObject() = new UppercaseKeysObjectVisitor(downstream.visitObject())
 * }
 * }}}
 *
 * ==Design patterns used==
 *
 * This implementation demonstrates multiple Scala design patterns:
 *   - '''Visitor pattern''': Separates algorithms from data structures
 *   - '''Chain of responsibility''': Visitors can forward to next in chain
 *   - '''Strategy sattern''': Different visitors for different processing strategies
 *   - '''Template method''': Dispatch defines traversal, visitors define operations
 *   - '''Type classes''': Generic Visitor[T] works with any output type
 *
 * ==References==
 *   - Li Haoyi: "Zero-Overhead Tree Processing with the Visitor Pattern"
 *     [[https://www.lihaoyi.com/post/ZeroOverheadTreeProcessingwiththeVisitorPattern.html]]
 *   - Gang of Four: "Design patterns: Elements of reusable object-oriented software"
 *
 * @see
 *   [[Visitor]] for the visitor interface
 * @see
 *   [[ObjectVisitor]] for object traversal
 * @see
 *   [[Dispatch]] for the universal dispatcher
 */
package object visitor {

  /**
   * Convenience method for dispatching a JsonValue to a visitor.
   *
   * This is an alias for [[Dispatch.apply]] that enables cleaner syntax.
   *
   * @param input
   *   The JsonValue to traverse
   * @param visitor
   *   The visitor to process nodes
   * @tparam T
   *   The output type
   * @return
   *   The result after complete traversal
   *
   * @example
   *   {{{
   * import io.toonformat.toon4s.visitor._
   *
   * val result = dispatch(json, new StringifyVisitor())
   * // Equivalent to: Dispatch(json, new StringifyVisitor())
   *   }}}
   */
  def dispatch[T](input: JsonValue, visitor: Visitor[T]): T = Dispatch(input, visitor)

}
