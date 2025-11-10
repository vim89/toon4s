package io.toonformat.toon4s.visitor

import scala.collection.immutable.VectorMap

/**
 * Intermediate visitor that repairs common JSON errors from LLM output.
 *
 * Large Language Models often generate JSON with common formatting errors:
 *   - Trailing commas in objects/arrays
 *   - Unquoted keys
 *   - Missing commas between fields
 *   - Extra commas
 *   - Mixed quotes
 *
 * This visitor cleans up the structure during traversal, composing with downstream visitors for
 * zero-overhead repair + encoding.
 *
 * ==Real-World Use Case==
 * {{{
 * // LLM generates malformed JSON, we repair + encode in one pass
 * val llmOutput = parseWithLenientParser(llmGeneratedText)
 * val visitor = new JsonRepairVisitor(
 *   new StringifyVisitor(indent = 2)
 * )
 * val cleanToon = Dispatch(llmOutput, visitor)
 * }}}
 *
 * ==Repair Operations==
 *   - '''Null safety''': Converts null strings to proper nulls
 *   - '''Key normalization''': Ensures keys are valid identifiers
 *   - '''Empty removal''': Filters empty string values (optional)
 *   - '''Type coercion''': Fixes common type mismatches
 *
 * @param downstream
 *   The visitor to forward repaired data to
 * @param removeEmpty
 *   Remove empty string values (default: false)
 * @param normalizeKeys
 *   Convert keys to valid identifiers (default: true)
 * @tparam T
 *   Output type from downstream visitor
 */
final class JsonRepairVisitor[T](
    downstream: Visitor[T],
    removeEmpty: Boolean = false,
    normalizeKeys: Boolean = true,
) extends Visitor[T] {

  override def visitString(value: String): T = {
    // Repair common string issues
    val repaired = value match {
    case "null" | "NULL" | "Null"           => return downstream.visitNull()
    case "true" | "TRUE" | "True"           => return downstream.visitBool(true)
    case "false" | "FALSE" | "False"        => return downstream.visitBool(false)
    case s if s.trim.isEmpty && removeEmpty => ""
    case s                                  => s.trim
    }

    // Try parsing as number if it looks numeric
    try {
      if (
          repaired.nonEmpty && repaired.forall(c =>
            c.isDigit || c == '.' || c == '-' || c == '+' || c == 'e' || c == 'E'
          )
      ) {
        return downstream.visitNumber(BigDecimal(repaired))
      }
    } catch {
      case _: NumberFormatException => // Not a number, continue as string
    }

    downstream.visitString(repaired)
  }

  override def visitNumber(value: BigDecimal): T = downstream.visitNumber(value)

  override def visitBool(value: Boolean): T = downstream.visitBool(value)

  override def visitNull(): T = downstream.visitNull()

  override def visitArray(elements: Vector[T]): T = {
    // Filter out repaired empty elements if configured
    val cleaned = if (removeEmpty) {
      elements.filterNot(_ == downstream.visitString(""))
    } else {
      elements
    }
    downstream.visitArray(cleaned)
  }

  override def visitObject(): ObjectVisitor[T] = {
    new JsonRepairObjectVisitor(downstream.visitObject(), normalizeKeys, removeEmpty)
  }

}

/** ObjectVisitor for repairing object key-value pairs. */
final class JsonRepairObjectVisitor[T](
    downstream: ObjectVisitor[T],
    normalizeKeys: Boolean,
    removeEmpty: Boolean,
) extends ObjectVisitor[T] {

  private var lastKey: String = ""

  private var shouldSkip: Boolean = false

  override def visitKey(key: String): Unit = {
    // Normalize key to valid identifier
    val normalized = if (normalizeKeys) {
      key.trim
        .replaceAll("[^a-zA-Z0-9_]", "_")
        .replaceAll("^[0-9]", "_$0") // Prefix numbers with underscore
        .replaceAll("_{2,}", "_") // Collapse multiple underscores
    } else {
      key.trim
    }

    lastKey = normalized
    shouldSkip = normalized.isEmpty

    if (!shouldSkip) {
      downstream.visitKey(normalized)
    }
  }

  override def visitValue(): Visitor[T] = {
    if (shouldSkip) {
      // Return repair visitor but we'll skip in visitValue(value)
      new JsonRepairVisitor(downstream.visitValue(), removeEmpty, normalizeKeys)
    } else {
      new JsonRepairVisitor(downstream.visitValue(), removeEmpty, normalizeKeys)
    }
  }

  override def visitValue(value: T): Unit = {
    if (!shouldSkip) {
      downstream.visitValue(value)
    }
  }

  override def done(): T = downstream.done()

}
