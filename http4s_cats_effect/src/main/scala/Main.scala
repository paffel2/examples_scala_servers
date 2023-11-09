import cats.effect._
import org.http4s._
import org.http4s.dsl.io._
import org.http4s.server.Router
import cats.implicits._
import org.http4s.implicits._
import cats.syntax.all._
import com.comcast.ip4s._
import org.http4s.ember.server._
import scala.concurrent.duration._
import cats.syntax.all._
import cats.Monad

object Main extends IOApp {

  val helloWorldService = HttpRoutes.of[IO] {
    case GET -> Root / "hello" =>
      Ok("Hello world!")
    case POST -> Root / "check" / route => Ok(checkSome[IO](route))
  }

  val service = helloWorldService

  def checkSome[F[_]](str: String)(implicit f: Monad[F]): F[String] =
    str match {
      case "name" => f.pure("OK")
      case _      => f.pure("NOT OK")
    }

  val httpApp = Router("/" -> helloWorldService, "/api" -> service).orNotFound

  val server = EmberServerBuilder
    .default[IO]
    .withHost(ipv4"127.0.0.1")
    .withPort(port"8000")
    .withHttpApp(httpApp)
    .build

  override def run(args: List[String]): IO[ExitCode] =
    server.use(_ => IO.never).as(ExitCode.Success)
}
