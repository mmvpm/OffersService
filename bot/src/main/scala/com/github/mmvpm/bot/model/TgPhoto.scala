package com.github.mmvpm.bot.model

import com.github.mmvpm.bot.model.TgPhoto._
import com.github.mmvpm.model.Photo

import java.net.{URI, URL}

case class TgPhoto(url: Option[URL], telegramId: Option[String]) {

  def media: String =
    url
      .map(_.toString)
      .orElse(telegramId)
      .getOrElse(default.url.get.toString)
}

object TgPhoto {

  val default: TgPhoto =
    TgPhoto(Some(new URI("https://velaxom.ru/assets/images/rasprodazha/kessler-parts/no-image.png").toURL), None)

  def first(photos: Seq[Photo]): TgPhoto =
    photos.find(p => p.url.nonEmpty || p.telegramId.nonEmpty).map(from).getOrElse(default)

  def from(photo: Photo): TgPhoto =
    TgPhoto(photo.url, photo.telegramId)
}
