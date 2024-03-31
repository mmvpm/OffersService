package com.github.mmvpm.service.dao.schema

import com.github.mmvpm.model.UserStatus.UserStatus

case class UserPatch(
    login: Option[String] = None,
    name: Option[String] = None,
    status: Option[UserStatus] = None
)
