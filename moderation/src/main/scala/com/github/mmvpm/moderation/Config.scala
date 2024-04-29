package com.github.mmvpm.moderation

import com.github.mmvpm.moderation.Config._

import scala.concurrent.duration.FiniteDuration

case class Config(ofs: OfsConfig, retry: RetryConfig, worker: WorkerConfig)

object Config {

  case class OfsConfig(baseUrl: String, requestTimeout: FiniteDuration)

  case class RetryConfig(amount: Int, baseDelay: FiniteDuration)

  case class WorkerConfig(delayWait: FiniteDuration, delayWork: FiniteDuration)
}
