import zio._
import zio.http._
import Models.Models.Person
import Models.Models.InputPerson
import zio.json._
import java.io.IOException
import java.util.Properties
import zio.stream.ZStream
import Database.DataService
import io.getquill.jdbczio.Quill
import io.getquill._

object Main extends ZIOAppDefault {

  def routes = Routes(
    Method.GET / "hello" -> handler(Response.text("Hello world!")),
    Method.POST / "check" / string("name") -> handler {
      (name: String, req: Request) =>
        checkSome(name) match {
          case None    => Response.text("bad name").status(Status.BadRequest)
          case Some(_) => Response.text("OK")
        }
    },
    Method.GET / "person" /*page size parameter*/ -> handler(
      getListOfPersonsFromDb
    ),
    Method.POST / "person" -> handler { (req: Request) =>
      addPerson(req)
    },
    Method.GET / "person" / int("id") -> handler { (id: Int, _: Request) =>
      getPersonById(id)
    },
    Method.PUT / "person" -> handler { (req: Request) => updatePerson(req) },
    Method.DELETE / "person" -> handler { (req: Request) => deletePerson(req) }
  )

  def app =
    routes.toHttpApp

  val testPerson = Person(1, "Bob", 42)

  def checkSome(str: String): Option[String] =
    str match {
      case "name" => Some("OK")
      case _      => None
    }

  def printLine(string: String) =
    Console
      .printLine(string)
      .mapError(e =>
        Response.text(s"Error: $e").status(Status.InternalServerError)
      )

  def handleJson[A](
      req: Request
  )(
      f: A => ZIO[DataService, Response, Response]
  )(implicit decoder: JsonDecoder[A]) =
    for {
      a <- for {
        value <- req.body.asString.mapError(e =>
          Response.text(s"Error: $e").status(Status.InternalServerError)
        )

      } yield value.fromJson[A]

      result <- a match {
        case Left(e) =>
          for {
            _ <- printLine(s"Error: $e. Body ${req.body.toString()}")

          } yield (Response.text("bad json").status(Status.BadRequest))
        case Right(value) =>
          for {
            _ <- printLine(s"Good json $value")
            result <- f(value)
          } yield result

      }
    } yield result

  def addPerson(req: Request) = handleJson(req)(insertPersonToDb)

  def updatePerson(req: Request) = handleJson(req)(updatePersonToDb)

  def deletePerson(req: Request) = req.url.queryParams.get("id") match {
    case None =>
      ZIO.succeed(Response.text("No id parameter").status(Status.BadRequest))
    case Some(value) =>
      value.toIntOption match {
        case None =>
          ZIO.succeed(
            Response.text("Bad id parameter").status(Status.BadRequest)
          )
        case Some(id) => deletePersonToDb(id)
      }
  }

  def getListOfPersonsFromDb =
    DataService.getPersonsList
      .map(_.toJson)
      .map(a => Response.json(a))
      .mapError(e =>
        Response.text(s"Error: $e").status(Status.InternalServerError)
      )

  def getPersonById(id: Int) =
    DataService
      .getPersonById(id)
      .map(value =>
        value match {
          case None =>
            Response
              .text(s"Person with id $id not found")
              .status(Status.BadRequest)
          case Some(value) => Response.json(value.toJson)
        }
      )
      .mapError(e =>
        Response.text(s"Error: $e").status(Status.InternalServerError)
      )

  def insertPersonToDb(person: InputPerson) =
    DataService
      .insertPerson(person)
      .mapError(e =>
        Response.text(s"Error: $e").status(Status.InternalServerError)
      )
      .map(value =>
        if (value == 1)
          Response.text(s"Person $person added")
        else
          Response
            .text("Person didn't added")
            .status(Status.InternalServerError)
      )

  def updatePersonToDb(person: Person) =
    DataService
      .updatePerson(person)
      .mapError(e =>
        Response.text(s"Error: $e").status(Status.InternalServerError)
      )
      .map(value =>
        if (value == 1)
          Response.text(s"Person $person updated")
        else
          Response
            .text("Person didn't updated")
            .status(Status.InternalServerError)
      )

  def deletePersonToDb(id: Int) =
    DataService
      .deletePerson(id)
      .mapError(e =>
        Response.text(s"Error: $e").status(Status.InternalServerError)
      )
      .map(value =>
        if (value == 1)
          Response.text(s"Person $id deleted")
        else
          Response
            .text("Person didn't deleted")
            .status(Status.InternalServerError)
      )

  override val run =
    Server
      .serve(app)
      .provide(
        Server.defaultWithPort(8000),
        DataService.live,
        Quill.Postgres.fromNamingStrategy(io.getquill.SnakeCase),
        Quill.DataSource.fromPrefix("myDatabaseConfig")
      )

}
