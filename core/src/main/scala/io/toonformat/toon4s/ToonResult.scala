package io.toonformat.toon4s

/** Java api which represents either a successful result of type T or an error of type Error. */
trait ToonResult[Error, T] {

  /** Indicates whether the result is a success. */
  def isSuccess: Boolean

  /** Gets the successful value. */
  def getValue: T

  /** Gets the error. */
  def getError: Error

}

/** Java api companion object for creating ToonResult instances. */
object ToonResult {

  private case class DefaultToonResult[Error, T](success: Boolean, value: T, error: Error)
      extends ToonResult[Error, T] {

    override def isSuccess: Boolean = success

    override def getValue: T = value

    override def getError: Error = error

  }

  /** Creates a successful ToonResult with the given value. */
  def success[Error, T](value: T): ToonResult[Error, T] =
    DefaultToonResult(success = true, value, null.asInstanceOf[Error])

  /** Creates a failed ToonResult with the given error. */
  def failure[Error, T](error: Error): ToonResult[Error, T] =
    DefaultToonResult(success = false, null.asInstanceOf[T], error)

}
