package com.github.mmvpm.parseidon.producer

import cats.data.EitherT
import cats.Monad
import cats.effect.Sync
import cats.implicits.toTraverseOps
import com.github.mmvpm.parseidon.client.youla.YoulaClient
import com.github.mmvpm.parseidon.producer.catalog.CatalogConverter
import com.github.mmvpm.parseidon.producer.query.QueryGenerator
import com.github.mmvpm.parseidon.YoulaConfig
import com.github.mmvpm.parseidon.dao.queue.PageQueueWriter
import com.github.mmvpm.util.EitherUtils.sleep
import com.github.mmvpm.util.Logging

class PageProducerImpl[F[_]: Monad: Sync](
    youlaConfig: YoulaConfig,
    pageQueueWriter: PageQueueWriter[F],
    youlaClient: YoulaClient[F],
    queryGenerator: QueryGenerator[F],
    catalogConverter: CatalogConverter
) extends PageProducer[F]
    with Logging {

  override def run: EitherT[F, String, Unit] =
    EitherT(Monad[F].iterateWhile(one.value)(_.isRight))

  private def one: EitherT[F, String, Unit] =
    for {
      query <- EitherT.liftF(queryGenerator.randomQuery)
      _ = log.info(s"request youla for: '$query'")
      catalog <- youlaClient.search(query) // TODO: retry
      pages = catalogConverter.convert(catalog)
      _ = log.info(s"youla catalog size: ${pages.size}")
      _ <- pages.map(pageQueueWriter.append).sequence
      _ <- sleep(youlaConfig.catalogRequestDelay)
    } yield ()
}
