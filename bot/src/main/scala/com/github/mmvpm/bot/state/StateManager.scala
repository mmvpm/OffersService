package com.github.mmvpm.bot.state

import com.bot4s.telegram.models.Message

trait StateManager[F[_]] {
  def getNextState(tag: String, current: State)(implicit message: Message): F[State]
}
