package com.github.mmvpm.bot.state

import com.bot4s.telegram.models.Message
import com.github.mmvpm.bot.model.ChatID

import java.util.concurrent.ConcurrentHashMap

class StorageImpl[State](default: State) extends Storage[State] {

  private val storage = new ConcurrentHashMap[ChatID, State]

  def getRaw(chatId: ChatID): State =
    storage.getOrDefault(chatId, default)

  def get(implicit message: Message): State =
    storage.getOrDefault(message.chat.id, default)

  def setRaw(value: State)(chatId: ChatID): State =
    storage.put(chatId, value)

  def set(value: State)(implicit message: Message): State =
    storage.put(message.chat.id, value)
}
