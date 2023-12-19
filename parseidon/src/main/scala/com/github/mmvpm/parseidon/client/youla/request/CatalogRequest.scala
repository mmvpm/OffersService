package com.github.mmvpm.parseidon.client.youla.request

case class CatalogRequest(operationName: String, variables: Variables, extensions: Extensions)

object CatalogRequest {

  private val QueryVersion = 1

  def make(query: String, sha256: String, cursor: String = ""): CatalogRequest =
    CatalogRequest(
      operationName = "catalogProductsBoard",
      variables = Variables(query, cursor),
      Extensions(PersistedQuery(sha256, QueryVersion))
    )
}

case class Variables(search: String, cursor: String)

case class Extensions(persistedQuery: PersistedQuery)

case class PersistedQuery(sha256Hash: String, version: Int)
