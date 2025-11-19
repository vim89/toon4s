package io.toonformat.toon4s.visitor

import io.toonformat.toon4s.JsonValue
import io.toonformat.toon4s.JsonValue._

/**
 * Universal dispatcher for zero-overhead tree traversal with the Visitor Pattern.
 *
 * The Dispatch object provides a single generic function that traverses any JsonValue structure and
 * calls the appropriate visitor methods. This separates tree traversal logic from computation
 * logic, enabling flexible composition without intermediate data structures.
 *
 * ==Key properties==
 *   - '''Generic''': Works with any Visitor[T] implementation
 *   - '''Zero-overhead''': No intermediate structures created during traversal
 *   - '''Composable''': Visitors can be chained for complex transformations
 *   - '''Type-safe''': Generic type T flows through entire traversal
 *
 * ==Usage==
 * {{{
 * import io.toonformat.toon4s.visitor._
 * import io.toonformat.toon4s.JsonValue._
 *
 * val json = JObj(VectorMap(
 *   "name" -> JString("Alice"),
 *   "age" -> JNumber(30)
 * ))
 *
 * // Dispatch to a visitor
 * val visitor = new StringifyVisitor(indent = 2)
 * val result: String = Dispatch(json, visitor)
 * }}}
 *
 * ==How It Works==
 * The dispatch function performs pattern matching on the JsonValue and calls the corresponding
 * visitor method:
 *   - `JString` → `visitor.visitString(value)`
 *   - `JNumber` → `visitor.visitNumber(value)`
 *   - `JBool` → `visitor.visitBool(value)`
 *   - `JNull` → `visitor.visitNull()`
 *   - `JArray` → Recursively dispatch elements, then `visitor.visitArray(results)`
 *   - `JObj` → Object visitor lifecycle (visitKey → visitValue → visitValue → done)
 *
 * ==Object traversal lifecycle==
 * For objects, dispatch follows this sequence:
 * {{{
 * val objVisitor = visitor.visitObject()
 * for ((key, value) <- fields) {
 *   objVisitor.visitKey(key)                    // 1. Notify of key
 *   val valueVisitor = objVisitor.visitValue()  // 2. Get visitor for value
 *   val result = dispatch(value, valueVisitor)  // 3. Recursively process value
 *   objVisitor.visitValue(result)               // 4. Provide processed result
 * }
 * objVisitor.done()                             // 5. Finalize object
 * }}}
 *
 * @see
 *   [[Visitor]] for the visitor interface
 * @see
 *   [[ObjectVisitor]] for object traversal
 * @see
 *   Li Haoyi's article:
 *   [[https://www.lihaoyi.com/post/ZeroOverheadTreeProcessingwiththeVisitorPattern.html]]
 */
object Dispatch {

  /**
   * Dispatch a JsonValue to a visitor for processing.
   *
   * This is the core dispatcher that traverses the JsonValue tree and calls appropriate visitor
   * methods. The traversal is depth-first and eager (not lazy).
   *
   * ==Performance Characteristics==
   *   - '''Time Complexity''': O(n) where n is the number of nodes in the tree
   *   - '''Space Complexity''': O(d) where d is the tree depth (call stack)
   *   - '''Allocations''': Zero intermediate structures (visitors may allocate for their output)
   *
   * ==Example - simple dispatch==
   * {{{
   * val json = JNumber(42)
   * val visitor = new SummationVisitor()
   * val sum: Int = Dispatch(json, visitor)  // Returns 42
   * }}}
   *
   * ==Example - Chained visitors==
   * {{{
   * // Chain: Validate → Transform → Stringify
   * val visitor = new ValidationVisitor(
   *   new TransformVisitor(
   *     new StringifyVisitor(indent = 2)
   *   )
   * )
   * val result: String = Dispatch(json, visitor)
   * }}}
   *
   * @param input
   *   The JsonValue to traverse
   * @param visitor
   *   The visitor that will process the nodes
   * @tparam T
   *   The output type produced by the visitor
   * @return
   *   The result of type T after complete traversal
   */
  def apply[T](input: JsonValue, visitor: Visitor[T]): T = {
    input match {
    // Primitive values - direct delegation to visitor
    case JString(value) => visitor.visitString(value)
    case JNumber(value) => visitor.visitNumber(value)
    case JBool(value)   => visitor.visitBool(value)
    case JNull          => visitor.visitNull()

    // Array - recursively dispatch elements, then visit array
    case JArray(elements) =>
      val results = elements.map(elem => apply(elem, visitor))
      visitor.visitArray(results)

    // Object - use ObjectVisitor lifecycle
    case JObj(fields) =>
      val objVisitor = visitor.visitObject()
      fields.foreach {
        case (key, value) =>
          objVisitor.visitKey(key)
          val valueVisitor = objVisitor.visitValue()
          val result = apply(value, valueVisitor)
          objVisitor.visitValue(result)
      }
      objVisitor.done()
    }
  }

}
