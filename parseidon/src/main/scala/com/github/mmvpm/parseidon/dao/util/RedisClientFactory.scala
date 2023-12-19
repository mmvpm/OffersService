package com.github.mmvpm.parseidon.dao.util

import com.github.mmvpm.parseidon.RedisConfig
import com.redis.RedisClient

class RedisClientFactory(redisConfig: RedisConfig) {

  def newInstance(): RedisClient =
    new RedisClient(redisConfig.host, redisConfig.port)
}
