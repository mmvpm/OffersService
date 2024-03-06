package com.github.mmvpm.stub.service

import cats.data.EitherT
import cats.Monad
import cats.effect.std.UUIDGen
import com.github.mmvpm.model.{Stub, StubID}
import com.github.mmvpm.stub.api.request._
import com.github.mmvpm.stub.api.response._
import com.github.mmvpm.stub.dao.util._
import com.github.mmvpm.stub.dao.StubDao
import cats.implicits.catsSyntaxList

class StubServiceImpl[F[_]: Monad: UUIDGen](stubDao: StubDao[F]) extends StubService[F] {

  def getStub(stubId: StubID): EitherT[F, String, StubResponse] =
    for {
      optStub <- stubDao.getStub(stubId)
      stub <- EitherT.fromOption(optStub, s"stub '$stubId' is not found'")
    } yield StubResponse(stub)

  def getStubs(request: GetStubsRequest): EitherT[F, String, StubsResponse] =
    for {
      nonEmptyList <- EitherT.fromOption(request.ids.toNel, s"ids list is empty")
      stubs <- stubDao.getStubs(nonEmptyList)
    } yield StubsResponse(stubs)

  def createStub(request: CreateStubRequest): EitherT[F, String, StubResponse] =
    for {
      id <- EitherT.liftF(UUIDGen[F].randomUUID)
      stub = Stub(id, request.data)
      success <- stubDao.createStub(stub)
      _ <- EitherT.cond(success, (), "no stubs inserted to the database")
    } yield StubResponse(stub)

  def updateStub(stubId: StubID, request: UpdateStubRequest): EitherT[F, String, OkResponse] =
    for {
      success <- stubDao.updateStub(stubId, StubUpdate(request.data))
      _ <- EitherT.cond(success, (), "no stubs updated in the database")
    } yield OkResponse()
}
