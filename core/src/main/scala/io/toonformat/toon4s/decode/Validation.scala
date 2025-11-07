package io.toonformat.toon4s
package decode

import io.toonformat.toon4s.error.DecodeError
import io.toonformat.toon4s.{Constants => C, Delimiter, Strictness}

trait WarningSink  { def warn(msg: String): Unit                                          }
object WarningSink { object Noop extends WarningSink { def warn(msg: String): Unit = () } }

private[decode] object Validation {

  private def warnOrThrow(msg: String, toErr: String => DecodeError)(implicit
      strictness: Strictness,
      ws: WarningSink
  ): Unit =
    strictness match {
      case Strictness.Strict  => throw toErr(msg)
      case Strictness.Audit   => ws.warn(msg)
      case Strictness.Lenient => ()
    }
  def assertExpectedCount(
      actual: Int,
      expected: Int,
      itemType: String
  )(implicit strictness: Strictness, ws: WarningSink): Unit =
    if (actual != expected)
      warnOrThrow(s"Expected $expected $itemType, but got $actual", DecodeError.Range.apply)

  def validateNoExtraListItems(cursor: LineCursor, itemDepth: Int, expectedCount: Int)(implicit
      strictness: Strictness,
      ws: WarningSink
  ): Unit =
    if (!cursor.atEnd)
      cursor.peek match {
        case Some(next)
            if next.depth == itemDepth && (next.content.startsWith(
              C.ListItemPrefix
            ) || next.content == C.ListItemMarker) =>
          warnOrThrow(
            s"Expected $expectedCount list array items, but found more",
            DecodeError.Range.apply
          )
        case _ => ()
      }

  def validateNoExtraTabularRows(
      cursor: LineCursor,
      rowDepth: Int,
      header: ArrayHeaderInfo
  )(implicit strictness: Strictness, ws: WarningSink): Unit =
    if (!cursor.atEnd)
      cursor.peek match {
        case Some(next)
            if next.depth == rowDepth && !next.content
              .startsWith(C.ListItemPrefix) && isDataRow(next.content, header.delimiter) =>
          warnOrThrow(
            s"Expected ${header.length} tabular rows, but found more",
            DecodeError.Range.apply
          )
        case _ => ()
      }

  def validateNoBlankLinesInRange(
      startLine: Int,
      endLine: Int,
      blankLines: Vector[BlankLine],
      context: String
  )(implicit strictness: Strictness, ws: WarningSink): Unit =
    blankLines
      .find(
        blank => blank.lineNumber > startLine && blank.lineNumber < endLine
      )
      .foreach(
        blank =>
          warnOrThrow(
            s"Line ${blank.lineNumber}: Blank lines inside $context are not allowed",
            DecodeError.Syntax.apply
          )
      )

  private def isDataRow(content: String, delimiter: Delimiter): Boolean = {
    val colonPos     = content.indexOf(C.Colon)
    val delimiterPos = content.indexOf(delimiter.char)
    if (colonPos == -1) true
    else if (delimiterPos != -1 && delimiterPos < colonPos) true
    else false
  }
}
