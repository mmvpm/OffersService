package com.github.mmvpm.model

import java.net.URL

case class Photo(id: PhotoID, url: Option[URL], blob: Option[Array[Byte]], telegramId: Option[String])
