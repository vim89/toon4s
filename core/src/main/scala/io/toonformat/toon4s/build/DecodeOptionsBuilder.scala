package io.toonformat.toon4s

// Phantom markers reused from EncodeOptionsBuilder (Missing/Present)
sealed trait DMissing
sealed trait DPresent

final class DecodeOptionsBuilder[HasIndent] private (
    private val indentOpt: Option[Int],
    private val strictFlag: Boolean,
    private val strictness: Strictness
) {
  def indent(n: Int): DecodeOptionsBuilder[DPresent] =
    new DecodeOptionsBuilder(Some(n), strictFlag, strictness)

  def strict(flag: Boolean): DecodeOptionsBuilder[HasIndent] =
    new DecodeOptionsBuilder(indentOpt, flag, strictness)

  def withStrictness(s: Strictness): DecodeOptionsBuilder[HasIndent] =
    new DecodeOptionsBuilder(indentOpt, strictFlag, s)

  def build(implicit ev: HasIndent =:= DPresent): DecodeOptions =
    DecodeOptions(indent = indentOpt.get, strict = strictFlag, strictness = strictness)
}

object DecodeOptionsBuilder {
  type IndentSet = DPresent

  def empty: DecodeOptionsBuilder[DMissing] =
    new DecodeOptionsBuilder(None, strictFlag = true, strictness = Strictness.Strict)
}

