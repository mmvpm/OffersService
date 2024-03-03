package com.github.mmvpm.stub.service

import cats.data.EitherT
import com.github.mmvpm.model.StubID
import com.github.mmvpm.stub.api.request.{CreateStubRequest, GetStubsRequest, UpdateStubRequest}
import com.github.mmvpm.stub.api.response.{OkResponse, StubResponse, StubsResponse}

trait StubService[F[_]] {
  def getStub(stubId: StubID): EitherT[F, String, StubResponse]
  def getStubs(request: GetStubsRequest): EitherT[F, String, StubsResponse]
  def createStub(request: CreateStubRequest): EitherT[F, String, StubResponse]
  def updateStub(stubId: StubID, request: UpdateStubRequest): EitherT[F, String, OkResponse]
}
