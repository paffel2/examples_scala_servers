scalaVersion := "2.13.12"

name := "zio-server"
version := "1.0"

libraryDependencies ++= Seq(
  "dev.zio" %% "zio-http" % "3.0.0-RC3",
  "io.getquill" %% "quill-jdbc-zio" % "4.8.0",
  "org.postgresql" % "postgresql" % "42.3.1"
)
