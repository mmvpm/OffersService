package com.github.mmvpm.parseidon.util

import com.github.mmvpm.parseidon.YoulaConfig

import scala.concurrent.duration.DurationInt

trait YoulaConfigSupport {

  val config: YoulaConfig = YoulaConfig(
    baseUrl = "https://youla.ru",
    graphqlUrl = "https://api-gw.youla.io/graphql",
    requestTimeout = 20.seconds,
    catalogRequestDelay = 5.seconds,
    pageRequestDelay = 500.milliseconds,
    sha256 = "6e7275a709ca5eb1df17abfb9d5d68212ad910dd711d55446ed6fa59557e2602",
    xAppId = "web/3",
    xUid = "657a0aec0dcc0"
  )
}
