package com.github.mmvpm.parsing

import cats.effect.std.Random
import cats.effect.{ExitCode, IO, IOApp}
import com.github.mmvpm.parsing.client.ofs.{OfsClient, OfsClientRetrying, OfsClientSttp}
import com.github.mmvpm.parsing.client.util.{RetryUtils, RetryUtilsImpl}
import com.github.mmvpm.parsing.client.youla.{YoulaClient, YoulaClientRetrying, YoulaClientSttp}
import com.github.mmvpm.parsing.consumer.{PageConsumer, PageConsumerImpl}
import com.github.mmvpm.parsing.dao.queue._
import com.github.mmvpm.parsing.dao.util.RedisClientFactory
import com.github.mmvpm.parsing.dao.visited.{PageVisitedDao, PageVisitedDaoRedis}
import com.github.mmvpm.parsing.parser.{PageParser, PageParserJsoup}
import com.github.mmvpm.parsing.producer.catalog.{CatalogConverter, CatalogConverterImpl}
import com.github.mmvpm.parsing.producer.query.{QueryGenerator, QueryGeneratorFromSeq}
import com.github.mmvpm.parsing.producer.{PageProducer, PageProducerImpl}
import com.github.mmvpm.parsing.util.ResourcesUtils.unsafeReadLines
import com.github.mmvpm.util.ConfigUtils.configByStage
import com.github.mmvpm.util.Logging
import net.ruippeixotog.scalascraper.browser._
import pureconfig.ConfigSource
import pureconfig.generic.auto._
import sttp.client3.SttpBackend
import sttp.client3.httpclient.cats.HttpClientCatsBackend

object Main extends IOApp with Logging {

  override def run(args: List[String]): IO[ExitCode] = {
    val config = ConfigSource.resources(configByStage(args)).loadOrThrow[Config]
    HttpClientCatsBackend.resource[IO]().use(runParser(config, _)).as(ExitCode.Success)
  }

  private def runParser(config: Config, sttpBackend: SttpBackend[IO, Any]): IO[Unit] =
    for {
      random <- Random.scalaUtilRandom[IO]
      browser = JsoupBrowser()
      redisFactory = new RedisClientFactory(config.redis)

      // separate redis client for each dao
      pageVisitedDao: PageVisitedDao[IO] = new PageVisitedDaoRedis[IO](redisFactory)
      pageQueueReader: PageQueueReader[IO] = new PageQueueReaderRedis[IO](redisFactory)
      pageQueueWriter: PageQueueWriter[IO] = new PageQueueWriterRedis[IO](redisFactory)

      retryUtils: RetryUtils[IO] = new RetryUtilsImpl[IO](config.retry)
      youlaClient: YoulaClient[IO] = new YoulaClientSttp[IO](config.youla, sttpBackend)
      ofsClient: OfsClient[IO] = new OfsClientSttp[IO](config.ofs, sttpBackend)
      ofsClientRetying: OfsClient[IO] = new OfsClientRetrying(ofsClient, retryUtils)
      youlaClientRetying: YoulaClient[IO] = new YoulaClientRetrying(youlaClient, retryUtils)

      pageParser: PageParser[IO] = new PageParserJsoup[IO](browser)
      catalogConverter: CatalogConverter = new CatalogConverterImpl(config.youla)

      queryWords = unsafeReadLines("queries.txt")
      queryGenerator: QueryGenerator[IO] = new QueryGeneratorFromSeq[IO](queryWords, random)

      pageConsumer: PageConsumer[IO] =
        new PageConsumerImpl[IO](
          config.youla,
          pageVisitedDao,
          pageQueueReader,
          pageParser,
          ofsClientRetying
        )
      pageProducer: PageProducer[IO] =
        new PageProducerImpl[IO](
          config.youla,
          pageQueueWriter,
          youlaClientRetying,
          queryGenerator,
          catalogConverter
        )

      result <- IO.race(pageConsumer.run.value, pageProducer.run.value)
      _ = result match {
        case Left(Left(error))  => log.error(s"Consumer failed: $error")
        case Right(Left(error)) => log.error(s"Producer failed: $error")
        case _                  => log.info(s"Parser stopped")
      }
    } yield ()

}
