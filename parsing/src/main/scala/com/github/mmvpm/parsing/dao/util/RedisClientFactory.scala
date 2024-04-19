package com.github.mmvpm.parsing.dao.util

import com.github.mmvpm.parsing.RedisConfig
import com.redis.RedisClient

class RedisClientFactory(redisConfig: RedisConfig) {

  def newInstance(): RedisClient =
    new RedisClient(redisConfig.host, redisConfig.port)
}
