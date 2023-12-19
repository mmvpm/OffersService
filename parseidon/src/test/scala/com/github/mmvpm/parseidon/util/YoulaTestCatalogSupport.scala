package com.github.mmvpm.parseidon.util

import com.github.mmvpm.parseidon.client.youla.response.CatalogResponse
import io.circe.generic.auto._
import io.circe.parser.parse
import io.circe.Decoder.Result

import scala.io.Source
import scala.util.Using

trait YoulaTestCatalogSupport {

  val youlaTestCatalog: String =
    Using(Source.fromResource("youla-catalog-response.json"))(_.getLines().mkString("\n")).get

  val youlaTestCatalogJson: Result[CatalogResponse] =
    parse(youlaTestCatalog).toOption.get.as[CatalogResponse]

  val youlaTestCatalogUrls: Set[String] = Set(
    "/all/zhivotnye/sobaki/sobaka-v-dobryie-ruki-648c01f3ea4d2477335a14c3",
    "/sankt-peterburg/zhivotnye/sobaki/sobaka-v-dobryie-ruki-biesplatno-649a9953c435925f046587d3"
  )
}
