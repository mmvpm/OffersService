//noinspection TypeAnnotation
package com.github.mmvpm.parseidon.client.youla

import cats.effect.testing.scalatest.AsyncIOSpec
import cats.effect.IO
import com.github.mmvpm.parseidon.util.{YoulaConfigSupport, YoulaTestCatalogSupport}
import org.asynchttpclient.util.HttpConstants.Methods
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import sttp.client3.{Response, SttpBackend}
import sttp.client3.asynchttpclient.cats.AsyncHttpClientCatsBackend
import sttp.model.StatusCode

class YoulaClientSttpSpec extends AsyncFreeSpec with AsyncIOSpec with Matchers with Fixture {

  "YoulaClient" - {
    "get youla catalog" in {
      client.search("собака").value.asserting { case Right(catalog) =>
        val urls = catalog.data.feed.items.flatMap(_.product).map(_.url)
        urls.toSet shouldBe youlaTestCatalogUrls
      }
    }
  }
}

trait Fixture extends YoulaTestCatalogSupport with YoulaConfigSupport {

  private val sttpBackend: SttpBackend[IO, Any] = AsyncHttpClientCatsBackend
    .stub[IO]
    .whenRequestMatchesPartial {
      case r if r.method.toString() == Methods.POST =>
        Response.ok(youlaTestCatalogJson)
      case _ =>
        Response("Not found", StatusCode.BadGateway)
    }

  val client = new YoulaClientSttp[IO](config, sttpBackend)
}
