package com.github.mmvpm.stub.api

import com.github.mmvpm.model.StubID
import com.github.mmvpm.stub.api.request._
import com.github.mmvpm.stub.api.response._
import com.github.mmvpm.stub.api.util.CirceInstances._
import com.github.mmvpm.stub.api.util.SchemaInstances._
import com.github.mmvpm.stub.service.StubService
import sttp.tapir._
import sttp.tapir.json.circe._
import sttp.tapir.server.ServerEndpoint

class StubHandler[F[_]](stubService: StubService[F]) extends Handler[F] {

  private val getStub: ServerEndpoint[Any, F] =
    endpoint.get
      .summary("Получить заглушку по её id")
      .in("api" / "v1" / "stub" / path[StubID]("stub-id"))
      .out(jsonBody[StubResponse])
      .errorOut(stringBody)
      .serverLogic(stubService.getStub(_).value)

  private val getStubs: ServerEndpoint[Any, F] =
    endpoint.post
      .summary("Получить все заглушки")
      .in("api" / "v1" / "stubs")
      .in(jsonBody[GetStubsRequest])
      .out(jsonBody[StubsResponse])
      .errorOut(stringBody)
      .serverLogic(stubService.getStubs(_).value)

  private val createStub: ServerEndpoint[Any, F] =
    endpoint.post
      .summary("Создать заглушку")
      .in("api" / "v1" / "stub")
      .in(jsonBody[CreateStubRequest])
      .out(jsonBody[StubResponse])
      .errorOut(stringBody)
      .serverLogic(request => stubService.createStub(request).value)

  private val updateStub: ServerEndpoint[Any, F] =
    endpoint.put
      .summary("Изменить заглушку")
      .in("api" / "v1" / "stub" / path[StubID]("stub-id"))
      .in(jsonBody[UpdateStubRequest])
      .out(jsonBody[OkResponse])
      .errorOut(stringBody)
      .serverLogic((stubService.updateStub _).tupled(_).value)
  
  override def endpoints: List[ServerEndpoint[Any, F]] =
    List(getStub, getStubs, createStub, updateStub)
}
