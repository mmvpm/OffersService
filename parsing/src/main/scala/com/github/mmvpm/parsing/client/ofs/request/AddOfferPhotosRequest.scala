package com.github.mmvpm.parsing.client.ofs.request

case class AddOfferPhotosRequest(photoUrls: Seq[String], telegramIds: Seq[String] = Seq.empty)
