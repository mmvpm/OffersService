package com.github.mmvpm.moderation.client.ofs.request

import com.github.mmvpm.model.OfferID
import com.github.mmvpm.model.OfferStatus.OfferStatus
import com.github.mmvpm.moderation.client.ofs.request.UpdateOfferStatusBatchRequest._

case class UpdateOfferStatusBatchRequest(requests: List[UpdateOfferStatusRequest])

object UpdateOfferStatusBatchRequest {

  case class UpdateOfferStatusRequest(offerId: OfferID, newStatus: OfferStatus)

  def from(newStatuses: List[(OfferID, OfferStatus)]): UpdateOfferStatusBatchRequest =
    UpdateOfferStatusBatchRequest(
      newStatuses.map { case (offerId, newStatus) =>
        UpdateOfferStatusRequest(offerId, newStatus)
      }
    )
}
