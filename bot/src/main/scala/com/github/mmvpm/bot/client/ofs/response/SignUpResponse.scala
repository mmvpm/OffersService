package com.github.mmvpm.bot.client.ofs.response

case class SignUpResponse(user: OfsUser)

case class OfsUser(id: String) // not necessary to copy all fields
