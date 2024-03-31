package com.github.mmvpm.bot

import scala.concurrent.duration.FiniteDuration

case class Config(ofs: OfsConfig)

case class OfsConfig(baseUrl: String, requestTimeout: FiniteDuration)
