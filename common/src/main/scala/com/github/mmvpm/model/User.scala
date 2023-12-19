package com.github.mmvpm.model

import com.github.mmvpm.model.UserStatus.UserStatus

import java.time.Instant

case class User(id: UserID, description: UserDescription, status: UserStatus, rating: Rating, registeredAt: Instant)
