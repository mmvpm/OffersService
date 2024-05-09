package com.github.mmvpm.service.config

import cats.Monad
import cats.effect.std.Env
import com.github.mmvpm.secret.SecretService
import pureconfig.ConfigSource
import pureconfig.generic.auto._
import cats.implicits._

trait ConfigLoader[F[_]] {
  def load(filename: String): F[Config]
}

object ConfigLoader {

  def impl[F[_]: Monad: Env](secrets: SecretService[F]): ConfigLoader[F] =
    new Impl[F](secrets)

  private final class Impl[F[_]: Monad: Env](secrets: SecretService[F]) extends ConfigLoader[F] {

    def load(filename: String): F[Config] =
      enrichWithSecrets(ConfigSource.resources(filename).loadOrThrow[Config])

    private def enrichWithSecrets(config: Config): F[Config] =
      for {
        redisSecret <- secrets.redisPassword
        redisPassword = redisSecret.orElse(config.redis.password)

        postgresSecret <- secrets.postgresPassword
        postgresPassword = postgresSecret.orElse(config.postgresql.password)

        redisEnriched = config.redis.copy(password = redisPassword)
        postgresqlEnriched = config.postgresql.copy(password = postgresPassword)

        enriched = config.copy(
          redis = redisEnriched,
          postgresql = postgresqlEnriched
        )
      } yield enriched
  }
}
