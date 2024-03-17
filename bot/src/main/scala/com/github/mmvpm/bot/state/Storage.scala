package com.github.mmvpm.bot.state

import com.bot4s.telegram.models.Message

trait Storage[State] {
  def get(implicit message: Message): State
  def set(value: State)(implicit message: Message): State
}
