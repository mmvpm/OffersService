package com.github.mmvpm.parsing.producer

import cats.Monad
import cats.data.EitherT
import cats.effect.kernel.Temporal
import cats.implicits.{toFlatMapOps, toFunctorOps, toTraverseOps}
import com.github.mmvpm.parsing.YoulaConfig
import com.github.mmvpm.parsing.client.youla.YoulaClient
import com.github.mmvpm.parsing.dao.queue.PageQueueWriter
import com.github.mmvpm.parsing.model.Page
import com.github.mmvpm.parsing.producer.catalog.CatalogConverter
import com.github.mmvpm.parsing.producer.query.QueryGenerator
import com.github.mmvpm.util.Logging

class PageProducerImpl[F[_]: Temporal](
    youlaConfig: YoulaConfig,
    pageQueueWriter: PageQueueWriter[F],
    youlaClient: YoulaClient[F],
    queryGenerator: QueryGenerator[F],
    catalogConverter: CatalogConverter
) extends PageProducer[F]
    with Logging {

  override def run: EitherT[F, String, Unit] =
    EitherT(Monad[F].iterateWhile(step)(_.isRight))

  private def step: F[Either[String, Unit]] =
    queryGenerator.randomQuery.flatMap(processQuery(_).value)

  private def processQuery(query: String, pageNumber: Int = 0): EitherT[F, String, Unit] =
    for {
      pages <- getCatalog(query, pageNumber)
      _ <- sendToQueue(pages)
      _ <- makeDelay
      _ <- if (pages.nonEmpty) processQuery(query, pageNumber + 1) else EitherT.pure[F, String](())
    } yield ()

  private def getCatalog(query: String, page: Int): EitherT[F, String, List[Page]] = {
    log.info(s"request youla for: '$query', page = $page")
    for {
      catalog <- youlaClient.search(query, page)
      pages = catalogConverter.convert(catalog)
      _ = log.info(s"youla catalog size: ${pages.size}")
    } yield pages
  }

  private def sendToQueue(pages: List[Page]): EitherT[F, String, Unit] =
    pages.map(pageQueueWriter.append).sequence.void

  private def makeDelay: EitherT[F, String, Unit] =
    EitherT.liftF(Temporal[F].sleep(youlaConfig.catalogRequestDelay))
}
