package com.github.mmvpm.nemia.api.util

import cats.effect.{IO, Resource}
import com.dimafeng.testcontainers.PostgreSQLContainer
import com.github.mmvpm.nemia.Config
import com.github.mmvpm.nemia.dao.util.FlywayMigration
import com.github.mmvpm.nemia.dao.util.Postgresql.makeTransactor
import doobie.Transactor
import org.testcontainers.containers.wait.strategy.Wait.defaultWaitStrategy
import org.testcontainers.utility.DockerImageName

trait PostgresContainerSupport {

  def makePostgresTransactor(config: Config): Resource[IO, Transactor[IO]] =
    for {
      _ <- makePostgresContainer
      _ <- Resource.eval(FlywayMigration.migrate[IO](config.postgresql))
      tx <- makeTransactor[IO](config.postgresql)
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
