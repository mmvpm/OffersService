package com.github.mmvpm.parsing.client.ofs.response

case class SignUpResponse(user: OfsUser)

case class OfsUser(id: String) // not necessary to copy all fields
