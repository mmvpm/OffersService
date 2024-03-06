package com.github.mmvpm.stub.util

import cats.effect.Sync
import cats.syntax.functor._
import com.github.mmvpm.stub.PostgresqlConfig
import org.flywaydb.core.Flyway

object FlywayMigration {

  def migrate[F[_]](config: PostgresqlConfig)(implicit F: Sync[F]): F[Unit] =
    F.delay(loadFlyway(config).migrate()).void

  def clean[F[_]](config: PostgresqlConfig)(implicit F: Sync[F]): F[Unit] =
    F.delay(loadFlyway(config).clean()).void

  // internal

  private val MigrationDirectory = "db.migration"

  private def loadFlyway(config: PostgresqlConfig): Flyway =
    Flyway
      .configure()
      .locations(MigrationDirectory)
      .cleanDisabled(false)
      .dataSource(config.url, config.user, config.password)
      .load()
}
