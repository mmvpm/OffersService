package com.github.mmvpm.bot.util

import cats.Monad
import com.github.mmvpm.bot.state.State

object StateUtils {

  implicit class StateSyntax(state: State) {
    def pure[F[_]: Monad]: F[State] = Monad[F].pure(state)
  }
}
