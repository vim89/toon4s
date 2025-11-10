package io.toonformat.toon4s.visitor

import io.toonformat.toon4s.JsonValue

/**
 * Typeclass for converting values to JsonValue via visitor pattern.
 *
 * Enables elegant `.toJsonValue` syntax.
 *
 * ==Usage==
 * {{{
 * case class User(name: String, age: Int)
 *
 * implicit val userConverter: VisitorConverter[User] = new VisitorConverter[User] {
 *   def convert(user: User): JsonValue = {
 *     val visitor = new ConstructionVisitor()
 *     // Build using visitor pattern
 *     val obj = visitor.visitObject()
 *     obj.visitKey("name")
 *     obj.visitValue(visitor.visitString(user.name))
 *     obj.visitKey("age")
 *     obj.visitValue(visitor.visitNumber(user.age))
 *     obj.done()
 *   }
 * }
 *
 * val user = User("Alice", 30)
 * val json = user.toJsonValue  // Extension method via VisitorConverterOps
 * val toon = user.toToon(indent = 2)
 * }}}
 */
trait VisitorConverter[T] {

  def convert(value: T): JsonValue

}

object VisitorConverter {

  /** Summon pattern for cleaner implicit resolution. */
  def apply[T](implicit conv: VisitorConverter[T]): VisitorConverter[T] = conv

  // Standard instances
  implicit val stringConverter: VisitorConverter[String] = (value: String) =>
    Dispatch(JsonValue.JString(value), new ConstructionVisitor())

  implicit val intConverter: VisitorConverter[Int] = (value: Int) =>
    Dispatch(JsonValue.JNumber(value), new ConstructionVisitor())

  implicit val boolConverter: VisitorConverter[Boolean] = (value: Boolean) =>
    Dispatch(JsonValue.JBool(value), new ConstructionVisitor())

  implicit def listConverter[T: VisitorConverter]: VisitorConverter[List[T]] = (values: List[T]) =>
    Dispatch(JsonValue.JArray(values.map(_.toJsonValue).toVector), new ConstructionVisitor())

  implicit def vectorConverter[T: VisitorConverter]: VisitorConverter[Vector[T]] =
    (values: Vector[T]) =>
      Dispatch(JsonValue.JArray(values.map(_.toJsonValue)), new ConstructionVisitor())

  // Extension to add .toJsonValue to any type with VisitorConverter
  implicit class VisitorConverterOps[T](value: T)(implicit converter: VisitorConverter[T]) {

    def toJsonValue: JsonValue = converter.convert(value)

    def toToon(indent: Int = 2): String =
      Dispatch(converter.convert(value), new StringifyVisitor(indent))

    def toToonFiltered(keysToFilter: Set[String], indent: Int = 2): String =
      Dispatch(
        converter.convert(value),
        new FilterKeysVisitor(keysToFilter, new StringifyVisitor(indent)),
      )

  }

}
