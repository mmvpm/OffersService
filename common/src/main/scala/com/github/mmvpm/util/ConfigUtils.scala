package com.github.mmvpm.util

object ConfigUtils {

  def configByStage(args: Iterable[String]): String =
    args.toList match {
      case "local" :: _ => "application-local.conf"
      case _            => "application.conf"
    }
}
