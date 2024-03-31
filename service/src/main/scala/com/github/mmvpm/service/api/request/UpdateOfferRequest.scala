package com.github.mmvpm.service.api.request

import com.github.mmvpm.model.Money

case class UpdateOfferRequest(
    name: Option[String] = None,
    price: Option[Money] = None,
    description: Option[String] = None
)
