import io.vertx.core.Vertx
import io.vertx.ext.web.Router
import sttp.tapir.{plainBody, query}
import sttp.tapir.ztapir._
import sttp.tapir.server.vertx.zio.VertxZioServerInterpreter
import sttp.tapir.server.vertx.zio.VertxZioServerInterpreter._
import zio._
import io.vertx.ext.web.Route
import sttp.tapir.json.zio.jsonBody
import sttp.tapir.generic.auto._

import Database.Database._
import Database.Models._
import io.getquill.jdbczio.Quill
import io.getquill.SnakeCase

class ServerEndpoints(dbRepository: DataService) {

  val getPersonEndpoint: ZServerEndpoint[Any, Any] =
    endpoint.get
      .in("get")
      .out(jsonBody[PersonListReponse])
      .zServerLogic(_ =>
        dbRepository.getPersonsList
          .catchAll(_ =>
            for {
              _ <- ZIO.logError("Something wrong")
            } yield List()
          )
          .map(list => PersonListReponse(list))
      )

  val getServerEndpointTest: ZServerEndpoint[Any, Any] =
    endpoint.get
      .in("gettest")
      .out(jsonBody[PersonListReponse])
      .zServerLogic(_ =>
        ZIO.succeed(PersonListReponse(List(Person(1, "aaa", 123))))
      )

  val addPersonEndpoint: ZServerEndpoint[Any, Any] =
    endpoint.post
      .in("post")
      .in(query[String]("name"))
      .in(query[Int]("age"))
      .out(plainBody[String])
      .zServerLogic(values =>
        dbRepository
          .insertPerson(values._1, values._2)
          .catchAll(_ =>
            for {
              _ <- ZIO.logError("Something wrong")
            } yield "Something went wrong"
          )
          .map(value => value.toString())
      )

  val getPersonByIdEndpoint: ZServerEndpoint[Any, Any] =
    endpoint.get
      .in("get_by_id")
      .in(path[Int])
      .out(jsonBody[Option[Person]])
      .zServerLogic(id =>
        dbRepository
          .getPersonById(id)
          .catchAll(_ =>
            for {
              _ <- ZIO.logError("Something wrong")
            } yield None
          )
      )

  val deletePersonByIdEndpoint: ZServerEndpoint[Any, Any] =
    endpoint.delete
      .in("delete")
      .in(path[Int])
      .out(plainBody[String])
      .zServerLogic(id =>
        dbRepository
          .deletePerson(id)
          .catchAll(_ =>
            for {
              _ <- ZIO.logError("Something wrong")
            } yield "Something wrong"
          )
          .map(value => value.toString())
      )

  val updatePersonEndpoint: ZServerEndpoint[Any, Any] =
    endpoint.put
      .in("put")
      .in(jsonBody[Person])
      .out(plainBody[String])
      .zServerLogic(newPerson =>
        dbRepository
          .updatePerson(newPerson)
          .catchAll(_ =>
            for {
              _ <- ZIO.logError("Something wrong")
            } yield "Something went wrong"
          )
          .map(value => value.toString())
      )

  // TODO: add catching exceptions

}

object ServerEndpoints {
  val live = ZLayer.fromFunction(
    new ServerEndpoints(_)
  )
}

object Main extends ZIOAppDefault {
  override implicit val runtime: zio.Runtime[Any] = zio.Runtime.default

  def addEndpoints(
      router: Router,
      listOfEndpoins: List[ZServerEndpoint[Any, Any]]
  ): Unit =
    listOfEndpoins match {
      case x :: xs => {
        val attach = VertxZioServerInterpreter().route(x)
        val a = attach(router)
        addEndpoints(router, xs)
      }
      case _ => ()
    }

  override def run = (for {
    endpoints <- ZIO.service[ServerEndpoints]
    _ <- ZIO
      .scoped(
        ZIO
          .acquireRelease(
            ZIO
              .attempt {
                val vertx = Vertx.vertx()
                val server = vertx.createHttpServer()
                val router = Router.router(vertx)
                addEndpoints(
                  router,
                  List(
                    endpoints.getServerEndpointTest,
                    endpoints.getPersonEndpoint,
                    endpoints.addPersonEndpoint,
                    endpoints.getPersonByIdEndpoint,
                    endpoints.deletePersonByIdEndpoint,
                    endpoints.updatePersonEndpoint
                  )
                )

                server.requestHandler(router).listen(8000)
              }
              .flatMap(_.asRIO)
          ) { server =>
            ZIO.attempt(server.close()).flatMap(_.asRIO).orDie
          } *> ZIO.never
      )

  } yield ())
    .provide(
      ServerEndpoints.live,
      DataService.live,
      Quill.Postgres.fromNamingStrategy(SnakeCase),
      Quill.DataSource.fromPrefix("myDatabaseConfig")
    )

}
