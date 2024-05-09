package com.github.mmvpm.service.dao.util

import cats.effect.{Async, Resource}
import cats.implicits.catsSyntaxOptionId
import com.github.mmvpm.service.config.PostgresqlConfig
import doobie.hikari.{Config, HikariTransactor}
import doobie.util.ExecutionContexts

object Postgresql {

  def makeTransactor[F[_]: Async](config: PostgresqlConfig): Resource[F, HikariTransactor[F]] = {

    val hikariConfig = Config(
      jdbcUrl = config.url.some,
      username = config.user.some,
      password = config.password,
      maximumPoolSize = config.poolSize.some,
      driverClassName = "org.postgresql.Driver".some
    )

    for {
      ce <- ExecutionContexts.fixedThreadPool[F](config.poolSize)
      xa <- HikariTransactor.fromConfig[F](hikariConfig, ce)
    } yield xa
  }
}
