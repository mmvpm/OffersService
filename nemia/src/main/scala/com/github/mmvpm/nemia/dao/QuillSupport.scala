package com.github.mmvpm.nemia.dao

import com.github.mmvpm.model.{OfferStatus, UserStatus}
import com.github.mmvpm.model.OfferStatus.OfferStatus
import com.github.mmvpm.model.UserStatus.UserStatus
import com.github.mmvpm.util.StringUtils.RichString
import io.getquill.MappedEncoding

import java.net.URL

trait QuillSupport {

  implicit val offerStatusEncoder: MappedEncoding[String, OfferStatus] = MappedEncoding(OfferStatus.withName)
  implicit val offerStatusDecoder: MappedEncoding[OfferStatus, String] = MappedEncoding(_.toString)

  implicit val userStatusEncoder: MappedEncoding[String, UserStatus] = MappedEncoding(UserStatus.withName)
  implicit val userStatusDecoder: MappedEncoding[UserStatus, String] = MappedEncoding(_.toString)

  implicit val urlEncoder: MappedEncoding[String, URL] = MappedEncoding(_.toURL)
  implicit val urlDecoder: MappedEncoding[URL, String] = MappedEncoding(_.toString)
}
