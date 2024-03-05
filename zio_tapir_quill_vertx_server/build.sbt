scalaVersion := "2.13.12"

name := "zio_tapir_quill_vertx_server"
version := "1.0"

libraryDependencies ++= Seq(
  "dev.zio" %% "zio" % "2.0.21",
  "com.softwaremill.sttp.tapir" %% "tapir-vertx-server-zio" % "1.9.10",
  "com.softwaremill.sttp.tapir" %% "tapir-json-zio" % "1.9.10",
  "io.getquill" %% "quill-jdbc-zio" % "4.8.0",
  "org.postgresql" % "postgresql" % "42.3.1"
)
