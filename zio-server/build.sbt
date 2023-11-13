scalaVersion := "2.13.12"

name := "zio-server"
version := "1.0"

libraryDependencies ++= Seq(
  "dev.zio" %% "zio-http" % "3.0.0-RC3"
)
