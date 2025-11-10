package io.toonformat.toon4s.visitor

/**
 * Extension methods for tree walkers (typeclass pattern).
 *
 * Enables idiomatic usage:
 * {{{
 * import io.toonformat.toon4s.visitor.TreeWalkerOps._
 *
 * // Implicit walker in scope
 * implicit val walker: TreeWalker[JsonNode] = JacksonWalker
 *
 * // Extension method syntax
 * val toon: String = jacksonNode.toToon(indent = 2)
 * val filtered: String = jacksonNode.toToonFiltered(Set("password"), indent = 2)
 * val repaired: JsonValue = jacksonNode.toJsonValue  // Convert to toon4s AST
 * }}}
 */
object TreeWalkerOps {

  /**
   * Extension methods for trees that have a TreeWalker instance.
   *
   * This is the typeclass pattern: we "pimp" any Tree type with toToon/toJsonValue methods.
   */
  implicit class TreeOps[Tree](tree: Tree)(implicit walker: TreeWalker[Tree]) {

    /**
     * Convert tree directly to TOON string without JsonValue intermediate.
     *
     * @param indent
     *   Indentation size (default: 2)
     * @return
     *   TOON string
     */
    def toToon(indent: Int = 2): String = {
      walker.dispatch(tree, new StringifyVisitor(indent))
    }

    /**
     * Convert tree to TOON with key filtering.
     *
     * @param keysToFilter
     *   Keys to remove
     * @param indent
     *   Indentation size
     * @return
     *   TOON string with filtered keys
     */
    def toToonFiltered(keysToFilter: Set[String], indent: Int = 2): String = {
      walker.dispatch(
        tree,
        new FilterKeysVisitor(keysToFilter, new StringifyVisitor(indent)),
      )
    }

    /**
     * Convert tree to JsonValue (toon4s AST).
     *
     * Useful for interop with existing toon4s code.
     *
     * @return
     *   JsonValue
     */
    def toJsonValue: io.toonformat.toon4s.JsonValue = {
      walker.dispatch(tree, new ConstructionVisitor())
    }

    /**
     * Apply visitor to tree (generic dispatch).
     *
     * @param visitor
     *   Visitor to apply
     * @tparam T
     *   Result type
     * @return
     *   Result of visitor traversal
     */
    def visit[T](visitor: Visitor[T]): T = {
      walker.dispatch(tree, visitor)
    }

  }

}
