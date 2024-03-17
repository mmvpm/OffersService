package com.github.mmvpm.bot.model

case class Draft(
    name: Option[String] = None,
    price: Option[Long] = None,
    description: Option[String] = None,
    photos: Seq[String] = Seq.empty)
