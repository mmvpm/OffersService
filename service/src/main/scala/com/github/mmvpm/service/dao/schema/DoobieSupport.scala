package com.github.mmvpm.service.dao.schema

import com.github.mmvpm.model.OfferStatus.OfferStatus
import com.github.mmvpm.model.UserStatus.UserStatus
import com.github.mmvpm.model.{OfferStatus, UserStatus}
import doobie._

import java.net.{URI, URL}

trait DoobieSupport {
  implicit val urlMeta: Meta[URL] = Meta[String].timap(new URI(_).toURL)(_.toString)
  implicit val offerStatusMeta: Meta[OfferStatus] = Meta[String].timap(OfferStatus.withName)(_.toString)
  implicit val userStatusMeta: Meta[UserStatus] = Meta[String].timap(UserStatus.withName)(_.toString)
}
