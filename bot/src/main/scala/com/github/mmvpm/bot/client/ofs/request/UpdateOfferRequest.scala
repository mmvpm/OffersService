package com.github.mmvpm.bot.client.ofs.request

import com.github.mmvpm.model.Money

case class UpdateOfferRequest(
    name: Option[String] = None,
    price: Option[Money] = None,
    description: Option[String] = None
)
