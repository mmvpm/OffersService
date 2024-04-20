package com.github.mmvpm.service.dao.schema

import com.github.mmvpm.model.{Photo, PhotoID}

import java.net.URL

case class PhotosEntry(id: PhotoID, url: Option[URL], blob: Option[Array[Byte]], telegramId: Option[String]) {

  def toPhoto: Photo =
    Photo(id, url, blob, telegramId)
}

object PhotosEntry {

  def from(photo: Photo): PhotosEntry =
    PhotosEntry(photo.id, photo.url, photo.blob, photo.telegramId)
}
