package com.github.mmvpm.parsing.client.youla

import cats.MonadThrow
import cats.data.EitherT
import cats.implicits.{toBifunctorOps, toFunctorOps}
import com.github.mmvpm.parsing.YoulaConfig
import com.github.mmvpm.parsing.client.youla.request.CatalogRequest
import com.github.mmvpm.parsing.client.youla.response.CatalogResponse
import io.circe.generic.auto._
import sttp.client3.circe._
import sttp.client3.{SttpBackend, UriContext, basicRequest}

class YoulaClientSttp[F[_]: MonadThrow](config: YoulaConfig, sttpBackend: SttpBackend[F, Any]) extends YoulaClient[F] {

  override def search(query: String, pageNumber: Int): EitherT[F, String, CatalogResponse] = {
    val requestUri = uri"${config.graphqlUrl}"
    val request = CatalogRequest.make(query, config.sha256, pageNumber)

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
