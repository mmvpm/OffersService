package com.github.mmvpm.bot.client.ofs.request

case class AddOfferPhotosRequest(photoUrls: Seq[String], telegramIds: Seq[String])
