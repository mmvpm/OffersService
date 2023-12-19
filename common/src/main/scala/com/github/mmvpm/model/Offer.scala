package com.github.mmvpm.model

import com.github.mmvpm.model.OfferStatus.OfferStatus

import java.time.Instant

case class Offer(
    id: OfferID,
    userId: UserID,
    description: OfferDescription,
    status: OfferStatus,
    createdAt: Instant,
    updatedAt: Instant)
