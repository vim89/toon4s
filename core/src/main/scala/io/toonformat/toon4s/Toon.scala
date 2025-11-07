package io.toonformat.toon4s

import io.toonformat.toon4s.encode.Encoders
import io.toonformat.toon4s.decode.Decoders
import io.toonformat.toon4s.error.{DecodeError, EncodeError}

import scala.util.Try

object Toon {
  def encode(value: Any, options: EncodeOptions = EncodeOptions()): EncodeResult[String] =
    for {
      normalized <- scala.util
                      .Try(internal.Normalize.toJson(value))
                      .toEither
                      .left
                      .map(
                        t => EncodeError.Normalization(t.getMessage)
                      )
      out        <- scala.util
                      .Try(Encoders.encode(normalized, options))
                      .toEither
                      .left
                      .map(
                        t => EncodeError.Normalization(t.getMessage)
                      )
    } yield out

  def encodeTo(
      value: Any,
      out: java.io.Writer,
      options: EncodeOptions = EncodeOptions()
  ): EncodeResult[Unit] =
    for {
      normalized <- scala.util
                      .Try(internal.Normalize.toJson(value))
                      .toEither
                      .left
                      .map(
                        t => EncodeError.Normalization(t.getMessage)
                      )
      _          <- scala.util
                      .Try({ Encoders.encodeTo(normalized, out, options); out.flush() })
                      .toEither
                      .left
                      .map(
                        t => EncodeError.Normalization(t.getMessage)
                      )
    } yield ()

  def decode(input: String, options: DecodeOptions = DecodeOptions()): DecodeResult[JsonValue] =
    scala.util
      .Try(Decoders.decode(input, options))
      .toEither
      .left
      .map {
        case err: DecodeError => err
        case other            => DecodeError.Unexpected(other.getMessage)
      }

  def encodeUnsafe(value: Any, options: EncodeOptions = EncodeOptions()): String =
    encode(value, options).fold(throw _, identity)

  def decodeUnsafe(input: String, options: DecodeOptions = DecodeOptions()): JsonValue =
    decode(input, options).fold(throw _, identity)

  def decodeFrom(
      in: java.io.Reader,
      options: DecodeOptions = DecodeOptions()
  ): DecodeResult[JsonValue] =
    scala.util
      .Try {
        val scan =
          io.toonformat.toon4s.decode.Scanner.toParsedLines(in, options.indent, options.strict)
        Decoders.decodeScan(scan, options)
      }
      .toEither
      .left
      .map {
        case err: DecodeError => err
        case other            => DecodeError.Unexpected(other.getMessage)
      }

  def decodeAudit(
      in: java.io.Reader,
      options: DecodeOptions = DecodeOptions()
  ): Either[(Vector[String], DecodeError), (Vector[String], JsonValue)] = {
    scala.util
      .Try(Decoders.decodeAudit(in, options))
      .toEither
      .left
      .map {
        case e: DecodeError => (Vector.empty[String], e)
        case t              => (Vector.empty[String], DecodeError.Unexpected(t.getMessage))
      }
  }
}
