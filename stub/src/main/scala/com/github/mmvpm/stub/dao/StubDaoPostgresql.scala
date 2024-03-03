package com.github.mmvpm.stub.dao

import cats.data.{EitherT, NonEmptyList}
import cats.effect.kernel.MonadCancelThrow
import cats.implicits.{catsSyntaxApplicativeError, _}
import com.github.mmvpm.model.{Stub, StubID}
import com.github.mmvpm.stub.dao.table.StubEntry
import com.github.mmvpm.stub.dao.util.StubUpdate
import doobie.Transactor
import doobie.implicits._
import doobie.postgres.implicits._
import doobie.util.fragments.in

class StubDaoPostgresql[F[_]: MonadCancelThrow](implicit val tr: Transactor[F]) extends StubDao[F] {

  override def getStub(stubId: StubID): EitherT[F, String, Option[Stub]] =
    sql"select * from stubs where id = $stubId"
      .query[StubEntry]
      .option
      .transact[F](tr)
      .attemptT
      .map(_.map(_.toStub))
      .leftMap(_.getMessage)

  def getStubs(stubIds: NonEmptyList[StubID]): EitherT[F, String, List[Stub]] =
    (fr"select * from stubs where" ++ in(fr"id", stubIds))
      .query[StubEntry]
      .to[List]
      .transact[F](tr)
      .attemptT
      .map(_.map(_.toStub))
      .leftMap(_.getMessage)

  override def createStub(stub: Stub): EitherT[F, String, Boolean] =
    sql"insert into stubs (id, data) values (${stub.id}, ${stub.data})".update.run
      .transact[F](tr)
      .attemptT
      .map(_ > 0)
      .leftMap(_.getMessage)

  override def updateStub(stubId: StubID, stubUpdate: StubUpdate): EitherT[F, String, Boolean] =
    sql"update stubs set data = ${stubUpdate.data} where id = $stubId".update.run
      .transact[F](tr)
      .attemptT
      .map(_ > 0)
      .leftMap(_.getMessage)
}
