package com.github.mmvpm.service.dao.schema

import com.github.mmvpm.model.OfferStatus.OfferStatus
import com.github.mmvpm.model.UserStatus.UserStatus
import com.github.mmvpm.model.{OfferStatus, UserStatus}
import doobie._

trait DoobieSupport {
  implicit val offerStatusMeta: Meta[OfferStatus] = Meta[String].timap(OfferStatus.withName)(_.toString)
  implicit val userStatusMeta: Meta[UserStatus] = Meta[String].timap(UserStatus.withName)(_.toString)
}
