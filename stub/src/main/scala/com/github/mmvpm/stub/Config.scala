package com.github.mmvpm.stub

case class Config(
    server: ServerConfig,
    postgresql: PostgresqlConfig)

case class ServerConfig(
    host: String,
    port: Int)

case class PostgresqlConfig(
    url: String,
    user: String,
    password: String,
    poolSize: Int)
