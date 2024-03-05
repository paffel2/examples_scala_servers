package Database

import zio.json._
import zio._
import io.getquill._
import io.getquill.jdbczio.Quill
import java.sql.SQLException

object Models {

  case class Person(
      val id: Int,
      val name: String,
      val age: Int
  )

  object Person {
    implicit val encoder: JsonEncoder[Person] =
      DeriveJsonEncoder.gen[Person]

    implicit val decoder: JsonDecoder[Person] =
      DeriveJsonDecoder.gen[Person]
  }

  case class PersonListReponse(persons: List[Person])

  object PersonListReponse {
    implicit val encoder: JsonEncoder[PersonListReponse] =
      DeriveJsonEncoder.gen[PersonListReponse]

    implicit val decoder: JsonDecoder[PersonListReponse] =
      DeriveJsonDecoder.gen[PersonListReponse]
  }
}

object Database {

  import Models._

  class DataService(quill: Quill.Postgres[SnakeCase]) {
    import quill._

    def getPersonsList: ZIO[Any, Any, List[Person]] =
      run(query[Person])

    def deletePerson(id: Int): ZIO[Any, Any, Long] = run(
      query[Person].filter(_.id == lift(id)).delete
    )

    def insertPerson(name: String, age: Int): ZIO[Any, Any, Long] =
      run(query[Person].insert(_.age -> lift(age), _.name -> lift(name)))

    def getPersonById(id: Int): ZIO[Any, Any, Option[Person]] =
      run(query[Person].filter(_.id == lift(id))).map(_.headOption)

    def updatePerson(newPerson: Person): ZIO[Any, Any, Long] = run(
      query[Person]
        .filter(_.id == lift(newPerson.id))
        .updateValue(lift(newPerson))
    )
  }

  // TODO: add catching exceptions
  // TODO TO ALL PROJECTS: add migrations

  object DataService {

    val live = ZLayer.fromFunction(new DataService(_))
  }

}
