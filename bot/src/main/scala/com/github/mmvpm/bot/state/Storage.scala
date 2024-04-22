package com.github.mmvpm.bot.state

import com.bot4s.telegram.models.Message
import com.github.mmvpm.bot.model.ChatID

trait Storage[State] {
  def getRaw(chatId: ChatID): State
  def get(implicit message: Message): State
  def setRaw(value: State)(chatId: ChatID): State
  def set(value: State)(implicit message: Message): State
}
