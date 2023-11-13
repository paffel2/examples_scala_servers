import zio._
import zio.http._
import Models.Models.Person
import Models.Models.InputPerson
import zio.json._
import java.io.IOException
import java.util.Properties
import zio.stream.ZStream

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
      Response.json(testPerson.toJson)
    ),
    Method.POST / "person" -> handler { (req: Request) => checkPersonJson(req) }
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
    Console.printLine(string).mapError(_ => Response.internalServerError)

  def checkPersonJson(req: Request) = for {
    a <- for {
      value <- req.body.asString.mapError(_ => Response.internalServerError)

    } yield value.fromJson[InputPerson]

    result <- a match {
      case Left(e) =>
        for {
          _ <- printLine(s"Error: $e. Body ${req.body.toString()}")

        } yield (Response.text("bad json").status(Status.BadRequest))
      case Right(value) =>
        for {
          _ <- printLine(s"Good json $value")
        } yield (Response
          .text("good json"))

    }
  } yield result

  override val run =
    Server.serve(app).provide(Server.defaultWithPort(8000))

}
