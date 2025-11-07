package io.toonformat.toon4s
package encode

trait EncodeLineWriter {
  def push(depth: Int, line: String): Unit
  def pushListItem(depth: Int, line: String): Unit
}

final class LineWriter(indentSize: Int) extends EncodeLineWriter {
  private val builder = new StringBuilder
  private var first   = true

  private def pad(depth: Int): Unit = {
    var i = 0
    while (i < depth * indentSize) {
      builder.append(' ')
      i += 1
    }
  }

  def push(depth: Int, line: String): Unit = {
    if (!first) builder.append('\n') else first = false
    pad(depth)
    builder.append(line)
  }

  def pushListItem(depth: Int, line: String): Unit = {
    if (!first) builder.append('\n') else first = false
    pad(depth)
    builder.append("- ")
    builder.append(line)
  }

  override def toString: String = builder.result()
}

import java.io.Writer

final class StreamLineWriter(indentSize: Int, out: Writer) extends EncodeLineWriter {
  private var first = true

  private def pad(depth: Int): Unit = {
    var i      = 0
    val spaces = depth * indentSize
    while (i < spaces) { out.write(' '); i += 1 }
  }

  def push(depth: Int, line: String): Unit = {
    if (!first) out.write('\n') else first = false
    pad(depth)
    out.write(line)
  }

  def pushListItem(depth: Int, line: String): Unit = {
    if (!first) out.write('\n') else first = false
    pad(depth)
    out.write("- ")
    out.write(line)
  }
}
