package com.github.mmvpm.parseidon.client.youla

import cats.data.EitherT
import cats.MonadThrow
import cats.implicits.{toBifunctorOps, toFunctorOps}
import com.github.mmvpm.parseidon.YoulaConfig
import com.github.mmvpm.parseidon.client.youla.request.CatalogRequest
import com.github.mmvpm.parseidon.client.youla.response.CatalogResponse
import io.circe.generic.auto._
import sttp.client3.{SttpBackend, UriContext, basicRequest}
import sttp.client3.circe._

class YoulaClientSttp[F[_]: MonadThrow](config: YoulaConfig, sttpBackend: SttpBackend[F, Any]) extends YoulaClient[F] {

  override def search(query: String): EitherT[F, String, CatalogResponse] = {
    val requestUri = uri"${config.graphqlUrl}"
    val request = CatalogRequest.make(query, config.sha256)

    val response = basicRequest
      .post(requestUri)
      .body(request)
      .contentType("application/json")
      .header("accept", "application/json")
      .header("x-app-id", config.xAppId)
      .header("x-uid", config.xUid)
      .response(asJson[CatalogResponse])
      .readTimeout(config.requestTimeout)
      .send(sttpBackend)
      .map(_.body.leftMap(_.getMessage))

    EitherT(response)
  }
}
