package com.github.mmvpm.service

import scala.concurrent.duration.FiniteDuration

case class Config(server: ServerConfig, session: SessionConfig, postgresql: PostgresqlConfig, redis: RedisConfig)

case class ServerConfig(host: String, port: Int)

case class PostgresqlConfig(url: String, user: String, password: String, poolSize: Int)

case class SessionConfig(expiration: FiniteDuration)

case class RedisConfig(host: String, port: Int)
