package com.github.mmvpm.parsing.dao.queue

import cats.Monad
import cats.data.EitherT
import cats.effect.kernel.Sync
import com.github.mmvpm.parsing.dao.QueueKey
import com.github.mmvpm.parsing.dao.util.PageSyntax.RichPage
import com.github.mmvpm.parsing.dao.util.RedisClientFactory
import com.github.mmvpm.parsing.model.Page
import com.github.mmvpm.util.EitherUtils.safe
import com.redis.RedisClient

class PageQueueWriterRedis[F[_]: Monad: Sync](redisFactory: RedisClientFactory) extends PageQueueWriter[F] {

  private lazy val redis: RedisClient = redisFactory.newInstance()

  override def append(page: Page): EitherT[F, String, Unit] =
    safe(redis.rpush(QueueKey, page.toRedis))
}
