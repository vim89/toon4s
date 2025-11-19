package io.toonformat.toon4s.visitor

import scala.collection.immutable.VectorMap

import io.toonformat.toon4s.JsonValue._
import io.toonformat.toon4s.visitor.VisitorConverter._

class VisitorConverterSpec extends munit.FunSuite {

  case class User(name: String, age: Int, email: String)

  case class Post(content: String, likes: Int)

  case class Feed(user: User, posts: List[Post])

  // Custom converters (like JSONSerialization.scala example)
  implicit val userConverter: VisitorConverter[User] = (user: User) =>
    JObj(VectorMap(
      "name" -> user.name.toJsonValue,
      "age" -> user.age.toJsonValue,
      "email" -> user.email.toJsonValue,
    ))

  implicit val postConverter: VisitorConverter[Post] = (post: Post) =>
    JObj(VectorMap(
      "content" -> post.content.toJsonValue,
      "likes" -> post.likes.toJsonValue,
    ))

  implicit val feedConverter: VisitorConverter[Feed] = (feed: Feed) =>
    JObj(VectorMap(
      "user" -> feed.user.toJsonValue,
      "posts" -> feed.posts.toJsonValue,
    ))

  test("VisitorConverter - convert String") {
    val result = "hello".toJsonValue
    assertEquals(result, JString("hello"))
  }

  test("VisitorConverter - convert Int") {
    val result = 42.toJsonValue
    assertEquals(result, JNumber(42))
  }

  test("VisitorConverter - convert Bool") {
    val result = true.toJsonValue
    assertEquals(result, JBool(true))
  }

  test("VisitorConverter - convert List[String]") {
    val result = List("a", "b", "c").toJsonValue
    assertEquals(result, JArray(Vector(JString("a"), JString("b"), JString("c"))))
  }

  test("VisitorConverter - convert User") {
    val user = User("Alice", 30, "alice@example.com")
    val result = user.toJsonValue
    assertEquals(
      result,
      JObj(VectorMap(
        "name" -> JString("Alice"),
        "age" -> JNumber(30),
        "email" -> JString("alice@example.com"),
      )),
    )
  }

  test("VisitorConverter - convert Post") {
    val post = Post("Hello World", 42)
    val result = post.toJsonValue
    assertEquals(
      result,
      JObj(VectorMap(
        "content" -> JString("Hello World"),
        "likes" -> JNumber(42),
      )),
    )
  }

  test("VisitorConverter - convert Feed with nested structures") {
    val feed = Feed(
      User("Bob", 25, "bob@example.com"),
      List(
        Post("First post", 10),
        Post("Second post", 20),
      ),
    )
    val result = feed.toJsonValue
    val expected = JObj(VectorMap(
      "user" -> JObj(VectorMap(
        "name" -> JString("Bob"),
        "age" -> JNumber(25),
        "email" -> JString("bob@example.com"),
      )),
      "posts" -> JArray(Vector(
        JObj(VectorMap(
          "content" -> JString("First post"),
          "likes" -> JNumber(10),
        )),
        JObj(VectorMap(
          "content" -> JString("Second post"),
          "likes" -> JNumber(20),
        )),
      )),
    ))
    assertEquals(result, expected)
  }

  test("VisitorConverter - toToon produces TOON string") {
    val user = User("Charlie", 28, "charlie@example.com")
    val toon = user.toToon(indent = 2)
    assert(toon.contains("name: Charlie"))
    assert(toon.contains("age: 28"))
    assert(toon.contains("email: charlie@example.com"))
  }

  test("VisitorConverter - toToonFiltered removes keys") {
    val user = User("Dave", 35, "dave@secret.com")
    val toon = user.toToonFiltered(Set("email"), indent = 2)
    assert(toon.contains("name: Dave"))
    assert(toon.contains("age: 35"))
    assert(!toon.contains("email"))
  }

}
