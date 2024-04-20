package com.github.mmvpm.service.api.request

import java.net.URL

case class AddOfferPhotosRequest(photoUrls: Seq[URL], telegramIds: Seq[String])
