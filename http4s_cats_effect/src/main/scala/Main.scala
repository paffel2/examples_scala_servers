import cats.effect._
import cats.Monad
import cats.syntax.all._

import org.http4s._
import org.http4s.dsl.io._
import org.http4s.server.Router
import org.http4s.circe._
import org.http4s.ember.server._

import com.comcast.ip4s._

import Models.Models._

import io.circe.syntax._

object Main extends IOApp {

  val helloWorldService = HttpRoutes.of[IO] {
    case GET -> Root / "hello" =>
      Ok("Hello world!")
    case POST -> Root / "check" / route => Ok(checkSome[IO](route))

    case GET -> Root / "person" =>
      Ok(testPerson.asJson(Person.PersonEncoder))

    case req @ POST -> Root / "person" => checkBody[Person](req)
  }

  val service = helloWorldService

  def checkSome[F[_]](str: String)(implicit f: Monad[F]): F[String] =
    str match {
      case "name" => f.pure("OK")
      case _      => f.pure("NOT OK")
    }

  def checkBody[A](
      req: Request[IO]
  )(implicit decoder: EntityDecoder[IO, A]): IO[Response[IO]] =
    req
      .as[A]
      .attempt
      .map(result => {
        result match {
          case Left(error)  => InternalServerError(s"Error: $error")
          case Right(value) => Ok(s"Good json: $value")
        }
      })
      .flatten

  val httpApp = Router("/" -> helloWorldService, "/api" -> service).orNotFound

  val testPerson = Person("Bob", 42, List("Alex"), Some("Alice"))

  val server = EmberServerBuilder
    .default[IO]
    .withHost(ipv4"127.0.0.1")
    .withPort(port"8000")
    .withHttpApp(httpApp)
    .build

  override def run(args: List[String]): IO[ExitCode] =
    server.use(_ => IO.never).as(ExitCode.Success)
}
