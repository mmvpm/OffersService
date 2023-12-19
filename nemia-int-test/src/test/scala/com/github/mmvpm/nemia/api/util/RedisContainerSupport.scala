package com.github.mmvpm.nemia.api.util

import cats.effect.{IO, Resource}
import com.redis.testcontainers.RedisContainer
import org.testcontainers.containers.wait.strategy.Wait.defaultWaitStrategy

trait RedisContainerSupport {

  def makeRedisContainer: Resource[IO, RedisContainer] =
    Resource.make(
      IO.delay {
        val container = new RedisContainer()
        container.start()
        container.waitingFor(defaultWaitStrategy)
        container
      }
    )(container => IO.delay(container.stop()))
}
