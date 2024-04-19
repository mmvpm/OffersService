package com.github.mmvpm.parsing.dao.queue

import cats.data.EitherT
import cats.Monad
import cats.effect.kernel.Sync
import com.github.mmvpm.parsing.dao.queue.PageQueueReaderRedis._
import com.github.mmvpm.parsing.dao.util.PageSyntax.RichString
import com.github.mmvpm.parsing.dao.QueueKey
import com.github.mmvpm.parsing.dao.util.RedisClientFactory
import com.github.mmvpm.parsing.model.Page
import com.github.mmvpm.util.EitherUtils.safe
import com.redis.RedisClient

class PageQueueReaderRedis[F[_]: Monad: Sync](redisFactory: RedisClientFactory) extends PageQueueReader[F] {

  private lazy val redis: RedisClient = redisFactory.newInstance()

  override def getNextBlocking: EitherT[F, String, Page] =
    safe(redis.blpop(BlockingReadTimeout, QueueKey).head._2.fromRedis)
}

object PageQueueReaderRedis {
  private val BlockingReadTimeout = 0 // endless waiting
}
