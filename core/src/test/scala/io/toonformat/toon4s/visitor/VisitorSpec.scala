package io.toonformat.toon4s.visitor

import scala.collection.immutable.VectorMap

import io.toonformat.toon4s.JsonValue
import io.toonformat.toon4s.JsonValue._

class VisitorSpec extends munit.FunSuite {

  // ===== Dispatch Tests =====

  test("Dispatch - string primitive") {
    val json = JString("hello")
    val visitor = new ConstructionVisitor()
    val result = Dispatch(json, visitor)
    assertEquals(result, JString("hello"))
  }

  test("Dispatch - number primitive") {
    val json = JNumber(BigDecimal("42.5"))
    val visitor = new ConstructionVisitor()
    val result = Dispatch(json, visitor)
    assertEquals(result, JNumber(BigDecimal("42.5")))
  }

  test("Dispatch - boolean true") {
    val json = JBool(true)
    val visitor = new ConstructionVisitor()
    val result = Dispatch(json, visitor)
    assertEquals(result, JBool(true))
  }

  test("Dispatch - boolean false") {
    val json = JBool(false)
    val visitor = new ConstructionVisitor()
    val result = Dispatch(json, visitor)
    assertEquals(result, JBool(false))
  }

  test("Dispatch - null") {
    val json = JNull
    val visitor = new ConstructionVisitor()
    val result = Dispatch(json, visitor)
    assertEquals(result, JNull)
  }

  test("Dispatch - empty array") {
    val json = JArray(Vector.empty)
    val visitor = new ConstructionVisitor()
    val result = Dispatch(json, visitor)
    assertEquals(result, JArray(Vector.empty))
  }

  test("Dispatch - array of primitives") {
    val json = JArray(Vector(JNumber(1), JNumber(2), JNumber(3)))
    val visitor = new ConstructionVisitor()
    val result = Dispatch(json, visitor)
    assertEquals(result, JArray(Vector(JNumber(1), JNumber(2), JNumber(3))))
  }

  test("Dispatch - nested array") {
    val json = JArray(Vector(
      JArray(Vector(JNumber(1), JNumber(2))),
      JArray(Vector(JNumber(3), JNumber(4))),
    ))
    val visitor = new ConstructionVisitor()
    val result = Dispatch(json, visitor)
    assertEquals(result, json)
  }

  test("Dispatch - empty object") {
    val json = JObj(VectorMap.empty)
    val visitor = new ConstructionVisitor()
    val result = Dispatch(json, visitor)
    assertEquals(result, JObj(VectorMap.empty))
  }

  test("Dispatch - simple object") {
    val json = JObj(VectorMap(
      "name" -> JString("Alice"),
      "age" -> JNumber(30),
    ))
    val visitor = new ConstructionVisitor()
    val result = Dispatch(json, visitor)
    assertEquals(result, json)
  }

  test("Dispatch - nested object") {
    val json = JObj(VectorMap(
      "user" -> JObj(VectorMap(
        "name" -> JString("Bob"),
        "active" -> JBool(true),
      ))
    ))
    val visitor = new ConstructionVisitor()
    val result = Dispatch(json, visitor)
    assertEquals(result, json)
  }

  test("Dispatch - complex nested structure") {
    val json = JObj(VectorMap(
      "users" -> JArray(Vector(
        JObj(VectorMap("id" -> JNumber(1), "name" -> JString("Alice"))),
        JObj(VectorMap("id" -> JNumber(2), "name" -> JString("Bob"))),
      )),
      "metadata" -> JObj(VectorMap(
        "count" -> JNumber(2),
        "status" -> JString("active"),
      )),
    ))
    val visitor = new ConstructionVisitor()
    val result = Dispatch(json, visitor)
    assertEquals(result, json)
  }

  test("Dispatch - preserves field order in objects") {
    val json = JObj(VectorMap(
      "z" -> JNumber(3),
      "a" -> JNumber(1),
      "m" -> JNumber(2),
    ))
    val visitor = new ConstructionVisitor()
    val result = Dispatch(json, visitor)
    assertEquals(result, json)
    assertEquals(result.asInstanceOf[JObj].value.keys.toSeq, Seq("z", "a", "m"))
  }

  // ===== StringifyVisitor Tests =====

  test("StringifyVisitor - simple string") {
    val json = JString("hello")
    val visitor = new StringifyVisitor(indent = 2)
    val result = Dispatch(json, visitor)
    assertEquals(result, "hello")
  }

  test("StringifyVisitor - string with spaces requires quotes") {
    val json = JString("hello world")
    val visitor = new StringifyVisitor(indent = 2)
    val result = Dispatch(json, visitor)
    assertEquals(result, "\"hello world\"")
  }

  test("StringifyVisitor - string with colon requires quotes") {
    val json = JString("key: value")
    val visitor = new StringifyVisitor(indent = 2)
    val result = Dispatch(json, visitor)
    assert(result.startsWith("\""))
  }

  test("StringifyVisitor - reserved keyword null requires quotes") {
    val json = JString("null")
    val visitor = new StringifyVisitor(indent = 2)
    val result = Dispatch(json, visitor)
    assertEquals(result, "\"null\"")
  }

  test("StringifyVisitor - reserved keyword true requires quotes") {
    val json = JString("true")
    val visitor = new StringifyVisitor(indent = 2)
    val result = Dispatch(json, visitor)
    assertEquals(result, "\"true\"")
  }

  test("StringifyVisitor - reserved keyword false requires quotes") {
    val json = JString("false")
    val visitor = new StringifyVisitor(indent = 2)
    val result = Dispatch(json, visitor)
    assertEquals(result, "\"false\"")
  }

  test("StringifyVisitor - empty string requires quotes") {
    val json = JString("")
    val visitor = new StringifyVisitor(indent = 2)
    val result = Dispatch(json, visitor)
    assertEquals(result, "\"\"")
  }

  test("StringifyVisitor - number") {
    val json = JNumber(42)
    val visitor = new StringifyVisitor(indent = 2)
    val result = Dispatch(json, visitor)
    assertEquals(result, "42")
  }

  test("StringifyVisitor - decimal number") {
    val json = JNumber(BigDecimal("3.14"))
    val visitor = new StringifyVisitor(indent = 2)
    val result = Dispatch(json, visitor)
    assertEquals(result, "3.14")
  }

  test("StringifyVisitor - boolean true") {
    val json = JBool(true)
    val visitor = new StringifyVisitor(indent = 2)
    val result = Dispatch(json, visitor)
    assertEquals(result, "true")
  }

  test("StringifyVisitor - boolean false") {
    val json = JBool(false)
    val visitor = new StringifyVisitor(indent = 2)
    val result = Dispatch(json, visitor)
    assertEquals(result, "false")
  }

  test("StringifyVisitor - null") {
    val json = JNull
    val visitor = new StringifyVisitor(indent = 2)
    val result = Dispatch(json, visitor)
    assertEquals(result, "null")
  }

  test("StringifyVisitor - empty array") {
    val json = JArray(Vector.empty)
    val visitor = new StringifyVisitor(indent = 2)
    val result = Dispatch(json, visitor)
    assertEquals(result, "arr[0]:")
  }

  test("StringifyVisitor - inline array of primitives") {
    val json = JArray(Vector(JNumber(1), JNumber(2), JNumber(3)))
    val visitor = new StringifyVisitor(indent = 2)
    val result = Dispatch(json, visitor)
    assertEquals(result, "arr[3]: 1,2,3")
  }

  test("StringifyVisitor - inline array of strings") {
    val json = JArray(Vector(JString("a"), JString("b"), JString("c")))
    val visitor = new StringifyVisitor(indent = 2)
    val result = Dispatch(json, visitor)
    assertEquals(result, "arr[3]: a,b,c")
  }

  test("StringifyVisitor - list array with complex elements") {
    val json = JArray(Vector(
      JObj(VectorMap("id" -> JNumber(1))),
      JObj(VectorMap("id" -> JNumber(2))),
    ))
    val visitor = new StringifyVisitor(indent = 2)
    val result = Dispatch(json, visitor)
    assert(result.contains("arr[2]:"))
    assert(result.contains("- "))
  }

  test("StringifyVisitor - simple object") {
    val json = JObj(VectorMap(
      "name" -> JString("Alice"),
      "age" -> JNumber(30),
    ))
    val visitor = new StringifyVisitor(indent = 2)
    val result = Dispatch(json, visitor)
    assert(result.contains("name: Alice"))
    assert(result.contains("age: 30"))
  }

  test("StringifyVisitor - empty object") {
    val json = JObj(VectorMap.empty)
    val visitor = new StringifyVisitor(indent = 2)
    val result = Dispatch(json, visitor)
    assertEquals(result, "")
  }

  test("StringifyVisitor - nested object") {
    val json = JObj(VectorMap(
      "user" -> JObj(VectorMap("name" -> JString("Bob")))
    ))
    val visitor = new StringifyVisitor(indent = 2)
    val result = Dispatch(json, visitor)
    assert(result.contains("user:"))
    assert(result.contains("name: Bob"))
  }

  // ===== ConstructionVisitor Tests =====

  test("ConstructionVisitor - identity for all primitives") {
    val inputs = Vector(
      JString("test"),
      JNumber(42),
      JBool(true),
      JBool(false),
      JNull,
    )
    val visitor = new ConstructionVisitor()
    inputs.foreach(json => assertEquals(Dispatch(json, visitor), json))
  }

  test("ConstructionVisitor - preserves array element order") {
    val json = JArray(Vector(JNumber(3), JNumber(1), JNumber(2)))
    val visitor = new ConstructionVisitor()
    val result = Dispatch(json, visitor)
    assertEquals(result.asInstanceOf[JArray].value, Vector(JNumber(3), JNumber(1), JNumber(2)))
  }

  test("ConstructionVisitor - preserves object field order") {
    val json = JObj(VectorMap(
      "c" -> JNumber(3),
      "a" -> JNumber(1),
      "b" -> JNumber(2),
    ))
    val visitor = new ConstructionVisitor()
    val result = Dispatch(json, visitor)
    assertEquals(result.asInstanceOf[JObj].value.keys.toSeq, Seq("c", "a", "b"))
  }

  test("ConstructionVisitor - deep nesting preserved") {
    val json = JObj(VectorMap(
      "level1" -> JObj(VectorMap(
        "level2" -> JObj(VectorMap(
          "level3" -> JNumber(42)
        ))
      ))
    ))
    val visitor = new ConstructionVisitor()
    val result = Dispatch(json, visitor)
    assertEquals(result, json)
  }

  // ===== FilterKeysVisitor Tests =====

  test("FilterKeysVisitor - removes specified key") {
    val json = JObj(VectorMap(
      "public" -> JString("visible"),
      "private" -> JString("hidden"),
    ))
    val visitor = new FilterKeysVisitor(
      Set("private"),
      new ConstructionVisitor(),
    )
    val result = Dispatch(json, visitor)
    val expected = JObj(VectorMap("public" -> JString("visible")))
    assertEquals(result, expected)
  }

  test("FilterKeysVisitor - removes multiple keys") {
    val json = JObj(VectorMap(
      "name" -> JString("Alice"),
      "password" -> JString("secret"),
      "ssn" -> JString("123-45-6789"),
      "email" -> JString("alice@example.com"),
    ))
    val visitor = new FilterKeysVisitor(
      Set("password", "ssn"),
      new ConstructionVisitor(),
    )
    val result = Dispatch(json, visitor).asInstanceOf[JObj]
    assert(!result.value.contains("password"))
    assert(!result.value.contains("ssn"))
    assert(result.value.contains("name"))
    assert(result.value.contains("email"))
  }

  test("FilterKeysVisitor - no keys removed if none match") {
    val json = JObj(VectorMap(
      "a" -> JNumber(1),
      "b" -> JNumber(2),
    ))
    val visitor = new FilterKeysVisitor(
      Set("c", "d"),
      new ConstructionVisitor(),
    )
    val result = Dispatch(json, visitor)
    assertEquals(result, json)
  }

  test("FilterKeysVisitor - removes all keys when all match") {
    val json = JObj(VectorMap(
      "a" -> JNumber(1),
      "b" -> JNumber(2),
    ))
    val visitor = new FilterKeysVisitor(
      Set("a", "b"),
      new ConstructionVisitor(),
    )
    val result = Dispatch(json, visitor)
    assertEquals(result, JObj(VectorMap.empty))
  }

  test("FilterKeysVisitor - filters nested objects") {
    val json = JObj(VectorMap(
      "user" -> JObj(VectorMap(
        "name" -> JString("Bob"),
        "password" -> JString("secret"),
      ))
    ))
    val visitor = new FilterKeysVisitor(
      Set("password"),
      new ConstructionVisitor(),
    )
    val result = Dispatch(json, visitor)
    val expected = JObj(VectorMap(
      "user" -> JObj(VectorMap("name" -> JString("Bob")))
    ))
    assertEquals(result, expected)
  }

  test("FilterKeysVisitor - filters in arrays of objects") {
    val json = JArray(Vector(
      JObj(VectorMap("id" -> JNumber(1), "secret" -> JString("a"))),
      JObj(VectorMap("id" -> JNumber(2), "secret" -> JString("b"))),
    ))
    val visitor = new FilterKeysVisitor(
      Set("secret"),
      new ConstructionVisitor(),
    )
    val result = Dispatch(json, visitor).asInstanceOf[JArray]
    result.value.foreach { elem =>
      val obj = elem.asInstanceOf[JObj]
      assert(!obj.value.contains("secret"))
      assert(obj.value.contains("id"))
    }
  }

  test("FilterKeysVisitor - preserves primitives unchanged") {
    val primitives = Vector(
      JString("test"),
      JNumber(42),
      JBool(true),
      JNull,
    )
    val visitor = new FilterKeysVisitor(
      Set("any"),
      new ConstructionVisitor(),
    )
    primitives.foreach(json => assertEquals(Dispatch(json, visitor), json))
  }

  test("FilterKeysVisitor - preserves empty objects") {
    val json = JObj(VectorMap.empty)
    val visitor = new FilterKeysVisitor(
      Set("any"),
      new ConstructionVisitor(),
    )
    val result = Dispatch(json, visitor)
    assertEquals(result, json)
  }

  test("FilterKeysVisitor - chain with StringifyVisitor") {
    val json = JObj(VectorMap(
      "name" -> JString("Alice"),
      "password" -> JString("secret"),
    ))
    val visitor = new FilterKeysVisitor(
      Set("password"),
      new StringifyVisitor(indent = 2),
    )
    val result = Dispatch(json, visitor)
    assert(result.contains("name: Alice"))
    assert(!result.contains("password"))
    assert(!result.contains("secret"))
  }

  // ===== Composition Tests =====

  test("Composition - FilterKeys → Construction preserves type") {
    val json = JObj(VectorMap("a" -> JNumber(1), "b" -> JNumber(2)))
    val visitor = new FilterKeysVisitor(
      Set("b"),
      new ConstructionVisitor(),
    )
    val result = Dispatch(json, visitor)
    assert(result.isInstanceOf[JObj])
  }

  test("Composition - FilterKeys → Stringify produces string") {
    val json = JObj(VectorMap("a" -> JNumber(1)))
    val visitor = new FilterKeysVisitor(
      Set.empty,
      new StringifyVisitor(indent = 2),
    )
    val result = Dispatch(json, visitor)
    assert(result.isInstanceOf[String])
  }

  test("Composition - chained filters work correctly") {
    val json = JObj(VectorMap(
      "a" -> JNumber(1),
      "b" -> JNumber(2),
      "c" -> JNumber(3),
    ))
    // Filter out "b", then filter out "a"
    val visitor1 = new FilterKeysVisitor(
      Set("b"),
      new FilterKeysVisitor(
        Set("a"),
        new ConstructionVisitor(),
      ),
    )
    val result = Dispatch(json, visitor1).asInstanceOf[JObj]
    assertEquals(result.value.size, 1)
    assert(result.value.contains("c"))
  }

  // ===== Edge Cases =====

  test("Edge case - deeply nested empty objects") {
    val json = JObj(VectorMap(
      "a" -> JObj(VectorMap(
        "b" -> JObj(VectorMap.empty)
      ))
    ))
    val visitor = new ConstructionVisitor()
    val result = Dispatch(json, visitor)
    assertEquals(result, json)
  }

  test("Edge case - array with mixed types") {
    val json = JArray(Vector(
      JString("text"),
      JNumber(42),
      JBool(true),
      JNull,
      JArray(Vector(JNumber(1))),
      JObj(VectorMap("key" -> JString("value"))),
    ))
    val visitor = new ConstructionVisitor()
    val result = Dispatch(json, visitor)
    assertEquals(result, json)
  }

  test("Edge case - very large numbers preserved") {
    val json = JNumber(BigDecimal("999999999999999999999999999999.123456789"))
    val visitor = new ConstructionVisitor()
    val result = Dispatch(json, visitor)
    assertEquals(result, json)
  }

  test("Edge case - string with all special characters") {
    val json = JString("\\\"\\n\\r\\t")
    val visitor = new ConstructionVisitor()
    val result = Dispatch(json, visitor)
    assertEquals(result, json)
  }

  test("Edge case - object with single key") {
    val json = JObj(VectorMap("only" -> JNumber(1)))
    val visitor = new StringifyVisitor(indent = 2)
    val result = Dispatch(json, visitor)
    assertEquals(result, "only: 1")
  }

  test("Edge case - array with single element") {
    val json = JArray(Vector(JNumber(42)))
    val visitor = new StringifyVisitor(indent = 2)
    val result = Dispatch(json, visitor)
    assertEquals(result, "arr[1]: 42")
  }

  // ===== Package object dispatch function =====

  test("package dispatch function works same as Dispatch.apply") {
    val json = JObj(VectorMap("test" -> JNumber(123)))
    val visitor = new ConstructionVisitor()
    val result1 = Dispatch(json, visitor)
    val result2 = dispatch(json, visitor)
    assertEquals(result1, result2)
  }

}
