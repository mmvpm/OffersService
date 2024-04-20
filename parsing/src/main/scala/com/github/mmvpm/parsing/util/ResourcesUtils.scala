package com.github.mmvpm.parsing.util

import scala.io.Source
import scala.util.Using

object ResourcesUtils {

  def unsafeReadLines(resource: String): Seq[String] =
    Using(Source.fromResource(resource)) { source =>
      source.getLines().map(_.trim).filter(_.nonEmpty).toSeq
    }.get
}
