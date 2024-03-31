package com.github.mmvpm.model

import com.github.mmvpm.model.OfferStatus.OfferStatus

case class Offer(id: OfferID, userId: UserID, description: OfferDescription, status: OfferStatus)
