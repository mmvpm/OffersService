package com.github.mmvpm.bot.client.ofs.response

import com.github.mmvpm.model.OfferID

case class CreateOfferResponse(offer: OfsOffer)

case class OfsOffer(id: OfferID) // it's not necessary to copy all fields
