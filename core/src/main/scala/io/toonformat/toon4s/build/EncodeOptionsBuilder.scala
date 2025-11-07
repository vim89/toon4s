package io.toonformat.toon4s

// Phantom markers
sealed trait Missing
sealed trait Present

final class EncodeOptionsBuilder[HasIndent, HasDelimiter] private (
    private val indentOpt: Option[Int],
    private val delimiterOpt: Option[Delimiter],
    private val lengthMarker: Boolean
) {
  def indent(n: Int): EncodeOptionsBuilder[Present, HasDelimiter] =
    new EncodeOptionsBuilder(Some(n), delimiterOpt, lengthMarker)

  def delimiter(d: Delimiter): EncodeOptionsBuilder[HasIndent, Present] =
    new EncodeOptionsBuilder(indentOpt, Some(d), lengthMarker)

  def withLengthMarker(flag: Boolean): EncodeOptionsBuilder[HasIndent, HasDelimiter] =
    new EncodeOptionsBuilder(indentOpt, delimiterOpt, flag)

  def build(implicit ev1: HasIndent =:= Present, ev2: HasDelimiter =:= Present): EncodeOptions =
    EncodeOptions(indent = indentOpt.get, delimiter = delimiterOpt.get, lengthMarker = lengthMarker)
}

object EncodeOptionsBuilder {
  type IndentSet    = Present
  type DelimiterSet = Present

  def empty: EncodeOptionsBuilder[Missing, Missing] =
    new EncodeOptionsBuilder(None, None, lengthMarker = false)

  def defaults: EncodeOptions = EncodeOptions()
}
