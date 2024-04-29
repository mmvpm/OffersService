package com.github.mmvpm.service.api.request

import com.github.mmvpm.model.OfferID
import com.github.mmvpm.model.OfferStatus.OfferStatus
import com.github.mmvpm.service.api.request.UpdateOfferStatusBatchRequest._

case class UpdateOfferStatusBatchRequest(requests: List[UpdateOfferStatusRequest])

object UpdateOfferStatusBatchRequest {
  case class UpdateOfferStatusRequest(offerId: OfferID, newStatus: OfferStatus)
}
