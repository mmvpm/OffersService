package com.github.mmvpm.parseidon.client.nemia.response

import com.github.mmvpm.model.OfferID

case class CreateOfferResponse(offer: NemiaOffer)

case class NemiaOffer(id: OfferID) // not necessary to copy all fields
