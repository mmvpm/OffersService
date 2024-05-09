package com.github.mmvpm.secret

trait SecretService[F[_]] {
  def telegramToken: F[Option[String]]
  def redisPassword: F[Option[String]]
  def postgresPassword: F[Option[String]]
}
