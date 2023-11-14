package Database

import zio._
import io.getquill._
import io.getquill.jdbczio.Quill
import java.sql.SQLException
import Models.Models._

class DataService(quill: Quill.Postgres[SnakeCase]) {
  import quill._
  def getPersonsList: ZIO[Any, SQLException, List[Person]] = run(query[Person])

  def getPersonById(id: Int): ZIO[Any, SQLException, Option[Person]] =
    run(query[Person].filter(p => p.id == lift(id))).map(_.headOption)

  def insertPerson(person: InputPerson): ZIO[Any, SQLException, Long] = run(
    query[Person]
      .insert(_.age -> lift(person.age), _.name -> lift(person.name))
  )

  def updatePerson(person: Person): ZIO[Any, SQLException, Long] = run(
    query[Person]
      .filter(_.id == lift(person.id))
      .updateValue(lift(person))
  )

  def deletePerson(id: Int): ZIO[Any, SQLException, Long] = run(
    query[Person].filter(_.id == lift(id)).delete
  )

}
object DataService {
  def getPersonsList: ZIO[DataService, SQLException, List[Person]] =
    ZIO.serviceWithZIO[DataService](_.getPersonsList)

  def getPersonById(id: Int): ZIO[DataService, SQLException, Option[Person]] =
    ZIO.serviceWithZIO[DataService](_.getPersonById(id))

  def insertPerson(person: InputPerson): ZIO[DataService, SQLException, Long] =
    ZIO.serviceWithZIO[DataService](_.insertPerson(person))

  def updatePerson(person: Person): ZIO[DataService, SQLException, Long] =
    ZIO.serviceWithZIO[DataService](_.updatePerson(person))

  def deletePerson(id: Int): ZIO[DataService, SQLException, Long] =
    ZIO.serviceWithZIO[DataService](_.deletePerson(id))

  val live = ZLayer.fromFunction(new DataService(_))
}
object Example extends ZIOAppDefault {
  override def run = {
    DataService
      .deletePerson(6)
      .provide(
        DataService.live,
        Quill.Postgres.fromNamingStrategy(SnakeCase),
        Quill.DataSource.fromPrefix("myDatabaseConfig")
      )
      .debug("Results")
      .exitCode
  }
}
