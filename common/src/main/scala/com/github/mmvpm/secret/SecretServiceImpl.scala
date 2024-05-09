package com.github.mmvpm.secret

import cats.effect.std.Env
import com.github.mmvpm.secret.SecretServiceImpl._

class SecretServiceImpl[F[_]: Env] extends SecretService[F] {

  def telegramToken: F[Option[String]] =
    Env[F].get(TelegramToken)

  def redisPassword: F[Option[String]] =
    Env[F].get(RedisPassword)

  def postgresPassword: F[Option[String]] =
    Env[F].get(PostgresPassword)
}

object SecretServiceImpl {
  private val TelegramToken = "TELEGRAM_TOKEN"
  private val RedisPassword = "REDIS_PASSWORD"
  private val PostgresPassword = "POSTGRES_PASSWORD"
}
