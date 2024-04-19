package com.github.mmvpm.parsing.client.ofs.response

import com.github.mmvpm.model.OfferID

case class CreateOfferResponse(offer: OfsOffer)

case class OfsOffer(id: OfferID) // not necessary to copy all fields
