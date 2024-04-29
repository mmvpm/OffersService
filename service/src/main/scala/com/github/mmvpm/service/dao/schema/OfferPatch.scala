package com.github.mmvpm.service.dao.schema

import com.github.mmvpm.model.{Money, OfferStatus}
import com.github.mmvpm.model.OfferStatus.OfferStatus
import com.github.mmvpm.service.api.request.{UpdateOfferRequest, UpdateOfferStatusBatchRequest}

case class OfferPatch(
    name: Option[String] = None,
    price: Option[Money] = None,
    description: Option[String] = None,
    status: Option[OfferStatus] = None
)

object OfferPatch {

  def from(request: UpdateOfferRequest): OfferPatch =
    OfferPatch(
      request.name,
      request.price,
      request.description,
      Some(OfferStatus.OnModeration)
    )
}
