addCompilerPlugin(("org.typelevel" % "kind-projector" % "0.13.2").cross(CrossVersion.full))

ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.12"

val catsVersion = "2.9.0"
val catsEffect3 = "3.4.8"
val circeVersion = "0.14.6"
val tapirVersion = "1.7.6"
val tapirCirce = "1.9.5"
val http4sVersion = "0.23.23"
val logbackVersion = "1.4.11"
val apacheCommonsVersion = "1.16.0"
val pureConfigVersion = "0.17.4"
val flywayVersion = "9.16.0"
val doobieVersion = "1.0.0-RC2"
val quillVersion = "4.6.0"
val redisVersion = "3.42"
val scrapperVersion = "3.0.0"
val sttpClientVersion = "3.9.0"
val catsRetryVersion = "3.1.0"
val catsBackendVersion = "3.8.13"

val testVersion = "1.4.0"
val scalatestVersion = "3.2.17"
val mockitoVersion = "3.2.16.0"
val wireMockVersion = "3.0.0"
val catsTestingVersion = "1.4.0"
val testcontainersVersion = "0.40.15"
val testcontainersRedis = "1.3.2"
val testcontainersPostgresqlVersion = "0.40.12"

val cats = Seq(
  "org.typelevel" %% "cats-core" % catsVersion,
  "org.typelevel" %% "cats-effect" % catsEffect3
)

val circe = Seq(
  "io.circe" %% "circe-core" % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-parser" % circeVersion
)

val pureconfig = Seq(
  "com.github.pureconfig" %% "pureconfig" % pureConfigVersion
)

val redis = Seq(
  "net.debasishg" %% "redisclient" % redisVersion
)

val tapir = Seq(
  "com.softwaremill.sttp.tapir" %% "tapir-http4s-server" % tapirVersion,
  "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-bundle" % tapirVersion,
  "com.softwaremill.sttp.tapir" %% "tapir-json-circe" % tapirCirce
)

val http4s = Seq(
  "org.http4s" %% "http4s-ember-server" % http4sVersion
)

val logback = Seq(
  "ch.qos.logback" % "logback-classic" % logbackVersion
)

val apacheCommons = Seq(
  "commons-codec" % "commons-codec" % apacheCommonsVersion
)

val databases = Seq(
//  "io.getquill" %% "quill-doobie" % quillVersion,
  "org.tpolecat" %% "doobie-core" % doobieVersion,
  "org.tpolecat" %% "doobie-postgres" % doobieVersion,
  "org.tpolecat" %% "doobie-hikari" % doobieVersion,
  "org.flywaydb" % "flyway-core" % flywayVersion
)

val sttpClient = Seq(
  "com.softwaremill.sttp.client3" %% "core" % sttpClientVersion,
  "com.softwaremill.sttp.client3" %% "circe" % sttpClientVersion,
  "com.softwaremill.sttp.client3" %% "async-http-client-backend-cats" % catsBackendVersion
)

val catsRetry = Seq(
  "com.github.cb372" %% "cats-retry" % catsRetryVersion
)

val scrapper = Seq(
  "net.ruippeixotog" %% "scala-scraper" % scrapperVersion
)

val testcontainers = Seq(
  "com.redislabs.testcontainers" % "testcontainers-redis" % testcontainersRedis,
  "com.dimafeng" %% "testcontainers-scala-scalatest" % testcontainersVersion,
  "com.dimafeng" %% "testcontainers-scala-postgresql" % testcontainersPostgresqlVersion
)

val scalatest = Seq(
  "org.scalatest" %% "scalatest" % scalatestVersion % Test
)

val mockito = Seq(
  "org.scalatestplus" %% "mockito-4-11" % mockitoVersion % Test
)

val catsTesting = Seq(
  "org.typelevel" %% "cats-effect-testing-scalatest" % catsTestingVersion % Test
)

val tapirStubServer = Seq(
  "com.softwaremill.sttp.tapir" %% "tapir-sttp-stub-server" % tapirVersion % Test
)

lazy val common = (project in file("common"))
  .settings(
    name := "common"
  )

lazy val stub = (project in file("stub"))
  .dependsOn(common)
  .settings(
    name := "stub",
    libraryDependencies ++= Seq(
      cats,
      logback,
      pureconfig,
      tapir,
      http4s,
      databases
    ).flatten
  )

lazy val root = (project in file("."))
  .settings(
    name := "OffersService"
  )
  .aggregate(common, stub)
