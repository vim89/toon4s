package io.toonformat.toon4s

import munit.FunSuite
import java.io.StringReader

class DecodeAuditSpec extends FunSuite {
  test("decodeAudit collects indentation warnings in audit mode") {
    val input = """
users[1]:
	- id: 1
""".stripMargin // contains a tab before '-'

    val reader = new StringReader(input)
    val opts   = DecodeOptions(indent = 2, strict = false, strictness = Strictness.Audit)
    val result = Toon.decodeAudit(reader, opts)
    result match {
      case Right((warnings, json)) =>
        assert(warnings.exists(_.toLowerCase.contains("tab indentation")))
      case Left((warnings, err))   =>
        fail(s"Unexpected error: $err with warnings: ${warnings.mkString(", ")}")
    }
  }
}
