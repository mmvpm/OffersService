package com.github.mmvpm.parsing.dao.visited

import cats.Monad
import cats.data.EitherT
import cats.effect.kernel.Sync
import com.github.mmvpm.parsing.dao.VisitedKey
import com.github.mmvpm.parsing.dao.util.PageSyntax.RichPage
import com.github.mmvpm.parsing.dao.util.RedisClientFactory
import com.github.mmvpm.parsing.model.Page
import com.github.mmvpm.util.EitherUtils.safe
import com.redis.RedisClient

class PageVisitedDaoRedis[F[_]: Monad: Sync](redisFactory: RedisClientFactory) extends PageVisitedDao[F] {

  private lazy val redis: RedisClient = redisFactory.newInstance()

  override def isVisited(page: Page): EitherT[F, String, Boolean] =
    safe(redis.sismember(VisitedKey, page.toRedis))

  override def markVisited(page: Page): EitherT[F, String, Unit] =
    safe(redis.sadd(VisitedKey, page.toRedis))
}
