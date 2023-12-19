package com.github.mmvpm.parseidon.client.nemia.response

case class SignUpResponse(user: NemiaUser)

case class NemiaUser(id: String) // not necessary to copy all fields
