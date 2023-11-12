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

import doobie._
import doobie.util.transactor.Transactor._

import Database.Database._

object Main extends IOApp {

  object OptionalNumOfElementsQueryParameterMatcher
      extends OptionalQueryParamDecoderMatcher[Int]("n")

  object PersonIdQueryParameterMatcher
      extends QueryParamDecoderMatcher[Int]("id")

  def helloWorldService(transactor: Aux[IO, Unit]) = HttpRoutes.of[IO] {
    case GET -> Root / "hello" =>
      Ok("Hello world!")
    case POST -> Root / "check" / route => Ok(checkSome[IO](route))

    case GET -> Root / "person" :? OptionalNumOfElementsQueryParameterMatcher(
          someN
        ) =>
      Ok(
        getPersonsList(
          n = someN.getOrElse(10),
          transactor = transactor
        ) // add checking someN
          .map(_.asJson)
      )

    case GET -> Root / "person" / n => // add checking type of parameter
      Ok(
        selectPerson(n.toInt, transactor).map(_.asJson)
      )

    case req @ POST -> Root / "person" =>
      addPerson(req, transactor)

    case req @ PUT -> Root / "person" =>
      updatePerson(req, transactor)

    case DELETE -> Root / "person" :? PersonIdQueryParameterMatcher(
          personId
        ) =>
      deletePerson(personId, transactor)
  }

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

  def checkInput[A](req: Request[IO])(implicit
      decoder: EntityDecoder[IO, A]
  ): IO[Either[Throwable, A]] =
    req.as[A].attempt

  def addPerson(req: Request[IO], transactor: Aux[IO, Unit]) = for {
    person <- checkInput[InputPerson](req)
    result <- person match {
      case Left(error) => InternalServerError(s"Error: $error")
      case Right(value) =>
        (for {
          result <- addPersonToDB(value, transactor).attempt

        } yield result match {
          case Left(error) => InternalServerError(s"Error: $error")
          case Right(n)    => Ok(s"Good json: person id $n")
        }).flatten
    }
  } yield result

  def updatePerson(req: Request[IO], transactor: Aux[IO, Unit]) = for {
    person <- checkInput[Person](req)
    result <- person match {
      case Left(error) => InternalServerError(s"Error: $error")
      case Right(value) =>
        (for {
          result <- updatePersonInDB(value, transactor).attempt

        } yield result match {
          case Left(error) => InternalServerError(s"Error: $error")
          case Right(n) =>
            if (n > 0)
              Ok(s"Person id ${value.id} updated")
            else
              InternalServerError(
                s"Error: person with id ${value.id} doesn't exist"
              )
        }).flatten
    }
  } yield result

  def deletePerson(personId: Int, transactor: Aux[IO, Unit]) = (for {
    result <- deletePersonFromDb(personId, transactor).attempt
  } yield result match {
    case Left(error) => InternalServerError(s"Error: $error")
    case Right(_)    => Ok(s"Person id ${personId} deleted")
  }).flatten

  def getPersonsList(n: Int = 10, transactor: Aux[IO, Unit]) = for {
    list <- selectPersons(n, transactor)
  } yield list

  def httpApp(transactor: Aux[IO, Unit]) =
    Router(
      "/" -> helloWorldService(transactor)
    ).orNotFound

  val testPerson = Person(1, "Bob", 42)

  val server = {

    val transactor: Aux[IO, Unit] = Transactor.fromDriverManager[IO](
      driver = "org.postgresql.Driver",
      url = "jdbc:postgresql://localhost:5432/test_db",
      user = "username",
      password = "userpassword",
      logHandler = None
    )

    EmberServerBuilder
      .default[IO]
      .withHost(ipv4"127.0.0.1")
      .withPort(port"8000")
      .withHttpApp(httpApp(transactor))
      .build
  }

  override def run(args: List[String]): IO[ExitCode] =
    server.use(_ => IO.never).as(ExitCode.Success)
}
