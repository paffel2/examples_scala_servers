scalaVersion := "2.13.12"

name := "http4s_cats_effect"
version := "1.0"

val http4sVersion = "0.23.23"

// Only necessary for SNAPSHOT releases
resolvers += Resolver.sonatypeRepo("snapshots")

libraryDependencies ++= Seq(
  "org.http4s" %% "http4s-ember-client" % http4sVersion,
  "org.http4s" %% "http4s-ember-server" % http4sVersion,
  "org.http4s" %% "http4s-dsl" % http4sVersion,
  "org.typelevel" %% "cats-effect" % "3.5.2",
  "org.http4s" %% "http4s-circe" % http4sVersion,
  // Optional for auto-derivation of JSON codecs
  "io.circe" %% "circe-generic" % "0.14.1",
  // Optional for string interpolation to JSON model
  "io.circe" %% "circe-literal" % "0.14.1"
)
