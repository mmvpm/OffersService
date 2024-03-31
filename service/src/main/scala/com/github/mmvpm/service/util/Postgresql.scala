package com.github.mmvpm.service.util

import cats.effect.{Async, Resource}
import cats.implicits.catsSyntaxOptionId
import com.github.mmvpm.service.PostgresqlConfig
import doobie.hikari.{Config, HikariTransactor}
import doobie.util.ExecutionContexts

object Postgresql {

  def makeTransactor[F[_]: Async](conf: PostgresqlConfig): Resource[F, HikariTransactor[F]] = {

    val hikariConfig = Config(
      jdbcUrl = conf.url.some,
      username = conf.user.some,
      password = conf.password.some,
      maximumPoolSize = conf.poolSize.some,
      driverClassName = "org.postgresql.Driver".some
    )

    for {
      ce <- ExecutionContexts.fixedThreadPool[F](conf.poolSize)
      xa <- HikariTransactor.fromConfig[F](hikariConfig, ce)
    } yield xa
  }
}
