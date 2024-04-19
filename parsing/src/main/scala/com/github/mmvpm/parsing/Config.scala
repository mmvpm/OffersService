package com.github.mmvpm.parsing

import scala.concurrent.duration.FiniteDuration

case class Config(redis: RedisConfig, ofs: OfsConfig, youla: YoulaConfig, retry: RetryConfiguration)

case class RedisConfig(host: String, port: Int)

case class OfsConfig(baseUrl: String, requestTimeout: FiniteDuration)

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

case class RetryConfiguration(amount: Int, baseDelay: FiniteDuration)
