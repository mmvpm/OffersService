package com.github.mmvpm.moderation.model

sealed trait Resolution

object Resolution {
  case object Ok extends Resolution
  case object Ban extends Resolution
}
