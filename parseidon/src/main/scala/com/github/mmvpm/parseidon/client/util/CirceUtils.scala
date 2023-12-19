package com.github.mmvpm.parseidon.client.util

import com.github.mmvpm.util.StringUtils.RichString
import io.circe.{Decoder, Encoder}

import java.net.URL

object CirceUtils {
  implicit val urlEncoder: Encoder[URL] = Encoder[String].contramap(_.toString)
  implicit val urlDecoder: Decoder[URL] = Decoder[String].map(_.toURL)
}
