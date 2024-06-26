package com.github.mmvpm.bot.render

import com.bot4s.telegram.methods.Request
import com.bot4s.telegram.models.Message
import com.github.mmvpm.bot.model.MessageID
import com.github.mmvpm.bot.state.State

trait Renderer {
  def render(
      state: State,
      editMessage: Option[MessageID],
      photoIds: Option[Seq[MessageID]]
  )(implicit message: Message): Seq[Request[?]]
}
