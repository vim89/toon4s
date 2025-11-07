package io.toonformat.toon4s.bench

import io.toonformat.toon4s._
import io.toonformat.toon4s.cli.token.TokenEstimator

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path, Paths}

object TokenBench {
  final case class Case(name: String, path: Path)

  private val tokenizers = List("cl100k", "o200k")

  private val samples = List(
    Case("tags-small", Paths.get("benchmarks/samples/tags.json")),
    Case("uniform-objects", Paths.get("benchmarks/samples/users.json")),
    Case("nested-irregular", Paths.get("benchmarks/samples/nested.json"))
  )

  def main(args: Array[String]): Unit = {
    println("# Token Efficiency (JSON → TOON)")
    println()
    samples.foreach {
      c =>
        val json = Files.readString(c.path, StandardCharsets.UTF_8)
        val toon = Toon
          .encode(SimpleJsonSupport.parseToScala(json), EncodeOptions(indent = 2))
          .fold(throw _, identity)
        tokenizers.foreach {
          tk =>
            val in   = TokenEstimator.estimateTokens(json, tk)
            val out  = TokenEstimator.estimateTokens(toon, tk)
            val pct  = if (in > 0) Math.round((1.0 - out.toDouble / in) * 100).toInt else 0
            val name = TokenEstimator.canonicalName(tk)
            println(
              f"- ${c.name}%-16s | $name%-10s | json=$in%6d | toon=$out%6d | savings=${pct}%3d%%"
            )
        }
    }
    println()
    println("# Throughput (encode/decode ops/sec, approximate)")
    val loops = 1000
    samples.foreach {
      c =>
        val json     = Files.readString(c.path, StandardCharsets.UTF_8)
        val any      = SimpleJsonSupport.parseToScala(json)
        val encNanos = timeN(loops) { Toon.encode(any, EncodeOptions(indent = 2)) }
        val encOps   = (loops.toDouble / (encNanos / 1e9)).toLong
        val toon     = Toon.encode(any, EncodeOptions(indent = 2)).fold(throw _, identity)
        val decNanos = timeN(loops) { Toon.decode(toon, DecodeOptions()) }
        val decOps   = (loops.toDouble / (decNanos / 1e9)).toLong
        println(f"- ${c.name}%-16s | encode=${encOps}%8d ops/s | decode=${decOps}%8d ops/s")
    }
    println()
    println("(Numbers vary by model/tokenizer and data shape; measure your own payloads.)")
  }

  private object SimpleJsonSupport {
    import io.toonformat.toon4s.json.SimpleJson
    def parseToScala(json: String): Any = SimpleJson.toScala(SimpleJson.parse(json))
  }

  private def timeN(n: Int)(thunk: => Any): Long = {
    var i   = 0
    var acc = 0L
    // warmup
    while (i < 50) { thunk; i += 1 }
    i = 0
    val t0  = System.nanoTime()
    while (i < n) { thunk; i += 1 }
    val t1  = System.nanoTime()
    t1 - t0
  }
}
