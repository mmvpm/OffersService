package com.github.mmvpm.nemia.api.util

object EitherUtils {

  /**
   * This is only for tests!
   */
  implicit class RichEither[A, B](either: Either[A, B]) {
    def error: A = either.swap.toOption.get
    def response: B = either.toOption.get
  }
}
