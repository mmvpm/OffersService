package com.github.mmvpm.stub.dao

import cats.data.{EitherT, NonEmptyList}
import com.github.mmvpm.model.{Stub, StubID}
import com.github.mmvpm.stub.dao.util.StubUpdate

trait StubDao[F[_]] {
  def getStub(stubId :StubID): EitherT[F, String, Option[Stub]]
  def getStubs(stubIds: NonEmptyList[StubID]): EitherT[F, String, List[Stub]]
  def createStub(stub: Stub): EitherT[F, String, Boolean]
  def updateStub(stubId: StubID, stubUpdate: StubUpdate): EitherT[F, String, Boolean]
}
