package com.github.mmvpm.nemia.api.util

import cats.effect.{IO, Resource}
import com.dimafeng.testcontainers.PostgreSQLContainer
import com.github.mmvpm.nemia.PostgresqlConfig
import com.github.mmvpm.nemia.dao.util.FlywayMigration
import com.github.mmvpm.nemia.dao.util.Postgresql.makeTransactor
import doobie.Transactor
import org.testcontainers.containers.wait.strategy.Wait.defaultWaitStrategy
import org.testcontainers.utility.DockerImageName

trait PostgresContainerSupport {

  def makePostgresTransactor: Resource[IO, Transactor[IO]] =
    for {
      container <- makePostgresContainer
      config = PostgresqlConfig(
        container.jdbcUrl,
        container.username,
        container.password,
        poolSize = 2
      )
      _ <- Resource.eval(FlywayMigration.migrate[IO](config))
      tx <- makeTransactor[IO](config)
    } yield tx

  private def makePostgresContainer: Resource[IO, PostgreSQLContainer] =
    Resource.make(
      IO.delay {
        val image = DockerImageName.parse("postgres")
        val container = PostgreSQLContainer.Def(image).start()
        container.container.waitingFor(defaultWaitStrategy)
        container
      }
    )(container => IO.delay(container.stop()))
}
