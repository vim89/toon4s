package io.toonformat.toon4s

import java.io.Writer

import io.toonformat.toon4s.error.{DecodeError, EncodeError}

object JToon4s {

  def encode(
      value: Any,
      options: EncodeOptions = EncodeOptions(),
  ): ToonResult[EncodeError, String] =
    Toon.encode(value, options) match {
    case Right(toon) => ToonResult.success(toon)
    case Left(err)   => ToonResult.failure(err)
    }

  def encodeTo(
      value: Any,
      out: Writer,
      options: EncodeOptions = EncodeOptions(),
  ): ToonResult[EncodeError, Void] =
    Toon.encodeTo(value, out, options) match {
    case Right(_)  => ToonResult.success(null)
    case Left(err) => ToonResult.failure(err)
    }

  def decode(
      input: String,
      options: DecodeOptions = DecodeOptions(),
  ): ToonResult[DecodeError, JsonValue] =
    Toon.decode(input, options) match {
    case Right(value) => ToonResult.success(value)
    case Left(err)    => ToonResult.failure(err)
    }

  def decodeFrom(
      in: java.io.Reader,
      options: DecodeOptions = DecodeOptions(),
  ): ToonResult[DecodeError, JsonValue] =
    Toon.decodeFrom(in, options) match {
    case Right(value) => ToonResult.success(value)
    case Left(err)    => ToonResult.failure(err)
    }

}
