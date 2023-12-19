package com.github.mmvpm.nemia.api.request

import com.github.mmvpm.model.{Money, Photo}

case class UpdateOfferRequest(
    name: Option[String] = None,
    price: Option[Money] = None,
    text: Option[String] = None,
    photos: Option[List[Photo]] = None)
