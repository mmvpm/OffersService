package com.github.mmvpm.model

import com.github.mmvpm.model.UserStatus.UserStatus

case class User(id: UserID, name: String, login: String, status: UserStatus)
