package com.github.mmvpm.nemia.api.util

import com.github.mmvpm.nemia.Config
import pureconfig.ConfigSource
import pureconfig.generic.auto._

trait ConfigSupport {

  val config: Config =
    ConfigSource.default.loadOrThrow[Config]

  val baseUrl: String =
    s"http://${config.server.host}:${config.server.port}"
}
