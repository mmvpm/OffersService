package com.github.mmvpm.parseidon

import scala.concurrent.duration.FiniteDuration

case class Config(redis: RedisConfig, nemia: NemiaConfig, youla: YoulaConfig)

case class RedisConfig(host: String, port: Int)

case class NemiaConfig(baseUrl: String, requestTimeout: FiniteDuration)

case class YoulaConfig(
    baseUrl: String,
    graphqlUrl: String,
    requestTimeout: FiniteDuration,
    catalogRequestDelay: FiniteDuration,
    pageRequestDelay: FiniteDuration,
    sha256: String,
    xAppId: String,
    xUid: String
)
