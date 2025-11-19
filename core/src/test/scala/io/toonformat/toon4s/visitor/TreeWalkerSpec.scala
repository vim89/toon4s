package io.toonformat.toon4s.visitor

import scala.collection.immutable.VectorMap

import io.toonformat.toon4s.JsonValue
import io.toonformat.toon4s.JsonValue._

class TreeWalkerSpec extends munit.FunSuite {

  // Simple tree type for testing (mimics JsonNode structure)
  sealed trait SimpleTree

  case class TString(value: String) extends SimpleTree

  case class TNumber(value: BigDecimal) extends SimpleTree

  case class TBool(value: Boolean) extends SimpleTree

  case object TNull extends SimpleTree

  case class TArray(elements: Vector[SimpleTree]) extends SimpleTree

  case class TObject(fields: VectorMap[String, SimpleTree]) extends SimpleTree

  // Walker implementation for SimpleTree
  object SimpleTreeWalker extends TreeWalker[SimpleTree] {

    override def asString(tree: SimpleTree): Option[String] = tree match {
    case TString(s) => Some(s)
    case _          => None
    }

    override def asNumber(tree: SimpleTree): Option[BigDecimal] = tree match {
    case TNumber(n) => Some(n)
    case _          => None
    }

    override def asBool(tree: SimpleTree): Option[Boolean] = tree match {
    case TBool(b) => Some(b)
    case _        => None
    }

    override def isNull(tree: SimpleTree): Boolean = tree match {
    case TNull => true
    case _     => false
    }

    override def asArray(tree: SimpleTree): Option[Vector[SimpleTree]] = tree match {
    case TArray(elems) => Some(elems)
    case _             => None
    }

    override def asObject(tree: SimpleTree): Option[VectorMap[String, SimpleTree]] = tree match {
    case TObject(fields) => Some(fields)
    case _               => None
    }

  }

  // ===== TreeWalker Tests =====

  test("TreeWalker - dispatch string to StringifyVisitor") {
    val tree = TString("hello")
    val visitor = new StringifyVisitor(indent = 2)
    val result = SimpleTreeWalker.dispatch(tree, visitor)
    assertEquals(result, "hello")
  }

  test("TreeWalker - dispatch number to StringifyVisitor") {
    val tree = TNumber(42)
    val visitor = new StringifyVisitor(indent = 2)
    val result = SimpleTreeWalker.dispatch(tree, visitor)
    assertEquals(result, "42")
  }

  test("TreeWalker - dispatch boolean to StringifyVisitor") {
    val tree = TBool(true)
    val visitor = new StringifyVisitor(indent = 2)
    val result = SimpleTreeWalker.dispatch(tree, visitor)
    assertEquals(result, "true")
  }

  test("TreeWalker - dispatch null to StringifyVisitor") {
    val tree = TNull
    val visitor = new StringifyVisitor(indent = 2)
    val result = SimpleTreeWalker.dispatch(tree, visitor)
    assertEquals(result, "null")
  }

  test("TreeWalker - dispatch array to StringifyVisitor") {
    val tree = TArray(Vector(TNumber(1), TNumber(2), TNumber(3)))
    val visitor = new StringifyVisitor(indent = 2)
    val result = SimpleTreeWalker.dispatch(tree, visitor)
    assert(result.contains("[3]: 1,2,3"))
  }

  test("TreeWalker - dispatch object to StringifyVisitor") {
    val tree = TObject(VectorMap(
      "name" -> TString("Alice"),
      "age" -> TNumber(30),
    ))
    val visitor = new StringifyVisitor(indent = 2)
    val result = SimpleTreeWalker.dispatch(tree, visitor)
    assert(result.contains("name: Alice"))
    assert(result.contains("age: 30"))
  }

  test("TreeWalker - dispatch nested object") {
    val tree = TObject(VectorMap(
      "user" -> TObject(VectorMap(
        "id" -> TNumber(1),
        "name" -> TString("Bob"),
      ))
    ))
    val visitor = new StringifyVisitor(indent = 2)
    val result = SimpleTreeWalker.dispatch(tree, visitor)
    assert(result.contains("user:"))
    assert(result.contains("id: 1"))
    assert(result.contains("name: Bob"))
  }

  test("TreeWalker - dispatch to ConstructionVisitor converts to JsonValue") {
    val tree = TObject(VectorMap(
      "name" -> TString("Charlie"),
      "active" -> TBool(true),
    ))
    val visitor = new ConstructionVisitor()
    val result = SimpleTreeWalker.dispatch(tree, visitor)
    assertEquals(
      result,
      JObj(VectorMap(
        "name" -> JString("Charlie"),
        "active" -> JBool(true),
      )),
    )
  }

  test("TreeWalker - dispatch with FilterKeysVisitor") {
    val tree = TObject(VectorMap(
      "username" -> TString("alice"),
      "password" -> TString("secret"),
      "email" -> TString("alice@example.com"),
    ))
    val visitor = new FilterKeysVisitor(
      Set("password"),
      new ConstructionVisitor(),
    )
    val result = SimpleTreeWalker.dispatch(tree, visitor).asInstanceOf[JObj]
    assert(result.value.contains("username"))
    assert(result.value.contains("email"))
    assert(!result.value.contains("password"))
  }

  test("TreeWalker - dispatch with JsonRepairVisitor") {
    val tree = TObject(VectorMap(
      "is active" -> TString("true"),
      "user id" -> TString("42"),
    ))
    val visitor = new JsonRepairVisitor(
      new ConstructionVisitor(),
      normalizeKeys = true,
    )
    val result = SimpleTreeWalker.dispatch(tree, visitor).asInstanceOf[JObj]
    assert(result.value.contains("is_active"))
    assert(result.value.contains("user_id"))
    assertEquals(result.value("is_active"), JBool(true))
    assertEquals(result.value("user_id"), JNumber(42))
  }

  test("TreeWalker - compose repair + filter + stringify") {
    val tree = TObject(VectorMap(
      "user name" -> TString("Alice"),
      "is admin" -> TString("false"),
      "password" -> TString("secret123"),
    ))
    val visitor = new JsonRepairVisitor(
      new FilterKeysVisitor(
        Set("password"),
        new StringifyVisitor(indent = 2),
      ),
      normalizeKeys = true,
    )
    val result = SimpleTreeWalker.dispatch(tree, visitor)
    assert(result.contains("user_name: Alice"))
    assert(result.contains("is_admin: false"))
    assert(!result.contains("password"))
  }

  test("TreeWalker - handles complex nested structure") {
    val tree = TObject(VectorMap(
      "users" -> TArray(Vector(
        TObject(VectorMap(
          "id" -> TNumber(1),
          "name" -> TString("Alice"),
        )),
        TObject(VectorMap(
          "id" -> TNumber(2),
          "name" -> TString("Bob"),
        )),
      ))
    ))
    val visitor = new ConstructionVisitor()
    val result = SimpleTreeWalker.dispatch(tree, visitor)
    assertEquals(
      result,
      JObj(VectorMap(
        "users" -> JArray(Vector(
          JObj(VectorMap(
            "id" -> JNumber(1),
            "name" -> JString("Alice"),
          )),
          JObj(VectorMap(
            "id" -> JNumber(2),
            "name" -> JString("Bob"),
          )),
        ))
      )),
    )
  }

  // ===== TreeConstructionVisitor Tests =====

  // Constructor for SimpleTree
  object SimpleTreeConstructionVisitor extends TreeConstructionVisitor[SimpleTree] {

    override def makeString(value: String): SimpleTree = TString(value)

    override def makeNumber(value: BigDecimal): SimpleTree = TNumber(value)

    override def makeBool(value: Boolean): SimpleTree = TBool(value)

    override def makeNull(): SimpleTree = TNull

    override def makeArray(elements: Vector[SimpleTree]): SimpleTree = TArray(elements)

    override def makeObject(fields: VectorMap[String, SimpleTree]): SimpleTree = TObject(fields)

  }

  test("TreeConstructionVisitor - construct string") {
    val json = JString("hello")
    val result = Dispatch(json, SimpleTreeConstructionVisitor)
    assertEquals(result, TString("hello"))
  }

  test("TreeConstructionVisitor - construct number") {
    val json = JNumber(42)
    val result = Dispatch(json, SimpleTreeConstructionVisitor)
    assertEquals(result, TNumber(42))
  }

  test("TreeConstructionVisitor - construct boolean") {
    val json = JBool(true)
    val result = Dispatch(json, SimpleTreeConstructionVisitor)
    assertEquals(result, TBool(true))
  }

  test("TreeConstructionVisitor - construct null") {
    val json = JNull
    val result = Dispatch(json, SimpleTreeConstructionVisitor)
    assertEquals(result, TNull)
  }

  test("TreeConstructionVisitor - construct array") {
    val json = JArray(Vector(JNumber(1), JNumber(2), JNumber(3)))
    val result = Dispatch(json, SimpleTreeConstructionVisitor)
    assertEquals(result, TArray(Vector(TNumber(1), TNumber(2), TNumber(3))))
  }

  test("TreeConstructionVisitor - construct object") {
    val json = JObj(VectorMap(
      "name" -> JString("Alice"),
      "age" -> JNumber(30),
    ))
    val result = Dispatch(json, SimpleTreeConstructionVisitor)
    assertEquals(
      result,
      TObject(VectorMap(
        "name" -> TString("Alice"),
        "age" -> TNumber(30),
      )),
    )
  }

  test("TreeConstructionVisitor - construct nested structure") {
    val json = JObj(VectorMap(
      "user" -> JObj(VectorMap(
        "id" -> JNumber(1),
        "tags" -> JArray(Vector(JString("admin"), JString("active"))),
      ))
    ))
    val result = Dispatch(json, SimpleTreeConstructionVisitor)
    assertEquals(
      result,
      TObject(VectorMap(
        "user" -> TObject(VectorMap(
          "id" -> TNumber(1),
          "tags" -> TArray(Vector(TString("admin"), TString("active"))),
        ))
      )),
    )
  }

  // ===== Round-trip Tests =====

  test("Round-trip - SimpleTree → JsonValue → SimpleTree") {
    val original = TObject(VectorMap(
      "name" -> TString("Alice"),
      "age" -> TNumber(30),
      "active" -> TBool(true),
      "metadata" -> TNull,
    ))

    // SimpleTree → JsonValue
    val jsonValue = SimpleTreeWalker.dispatch(original, new ConstructionVisitor())

    // JsonValue → SimpleTree
    val reconstructed = Dispatch(jsonValue, SimpleTreeConstructionVisitor)

    assertEquals(reconstructed, original)
  }

  test("Round-trip - with transformation") {
    val original = TObject(VectorMap(
      "username" -> TString("alice"),
      "password" -> TString("secret"),
      "email" -> TString("alice@example.com"),
    ))

    // SimpleTree → filtered JsonValue
    val filtered = SimpleTreeWalker.dispatch(
      original,
      new FilterKeysVisitor(Set("password"), new ConstructionVisitor()),
    )

    // JsonValue → SimpleTree
    val reconstructed = Dispatch(filtered, SimpleTreeConstructionVisitor)

    val expected = TObject(VectorMap(
      "username" -> TString("alice"),
      "email" -> TString("alice@example.com"),
    ))
    assertEquals(reconstructed, expected)
  }

}
