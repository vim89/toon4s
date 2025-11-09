package io.toonformat.toon4s

import java.io.StringWriter

import munit.FunSuite

class JToon4sSpec extends FunSuite {

  test("JToon4s.encode/decode roundtrip") {
    val data = Map("tags" -> List("jazz", "chill", "lofi"))
    val enc = JToon4s.encode(data, EncodeOptions())
    assert(enc.isSuccess)
    val toon = enc.getValue
    val dec = JToon4s.decode(toon, DecodeOptions())
    assert(dec.isSuccess)
  }

  test("JToon4s.encodeTo streams to Writer") {
    val data = Map("n" -> 42)
    val sw = new StringWriter()
    val res = JToon4s.encodeTo(data, sw, EncodeOptions())
    assert(res.isSuccess)
    val s = sw.toString
    assert(s.contains("n: 42"))
  }

}
