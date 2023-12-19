package com.github.mmvpm.nemia.api.util

import com.github.mmvpm.nemia.api.error.ApiError
import sttp.client3.{HttpError, ResponseException}
import io.circe.Error

object JsonUtils {

  def parseFailure(error: ResponseException[ApiError, Error]): ApiError =
    error match {
      case HttpError(body, _) => body
      case exception => sys.error(exception.toString)
    }
}
