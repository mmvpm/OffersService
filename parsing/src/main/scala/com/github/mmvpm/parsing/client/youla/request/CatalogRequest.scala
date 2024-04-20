package com.github.mmvpm.parsing.client.youla.request

import com.github.mmvpm.parsing.client.youla.request.CatalogRequest._

case class CatalogRequest(operationName: String, variables: Variables, extensions: Extensions)

object CatalogRequest {

  private val QueryVersion = 1

  private val SaintPetersburg = Location("576d0612d53f3d80945f8b5e")

  def make(query: String, sha256: String, page: Int, city: Location = SaintPetersburg): CatalogRequest =
    CatalogRequest(
      operationName = "catalogProductsBoard",
      variables = Variables(query, city, Cursor(page).toString),
      Extensions(PersistedQuery(sha256, QueryVersion))
    )

  case class Variables(search: String, location: Location, cursor: String)

  case class Cursor(page: Int) {
    override def toString: String = s"{\"page\":$page}"
  }

  case class Location(city: String)

  case class Extensions(persistedQuery: PersistedQuery)

  case class PersistedQuery(sha256Hash: String, version: Int)
}
