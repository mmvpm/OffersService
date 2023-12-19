package com.github.mmvpm.nemia.dao.util

import com.github.mmvpm.nemia.dao.table.{Offers, Users}
import com.github.mmvpm.util.MonadUtils.EnsureException

object DbSyntax {

  implicit class RichOffers(dbOffers: List[Offers]) {
    def single: Offers =
      dbOffers.headOption.getOrElse(throw new EnsureException(s"offer is not found"))
  }

  implicit class RichUsers(dbOffers: List[Users]) {
    def single: Users =
      dbOffers.headOption.getOrElse(throw new EnsureException(s"user is not found"))
  }
}
