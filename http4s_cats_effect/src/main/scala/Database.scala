package Database

import doobie.implicits._
import cats.effect._
import doobie.util.transactor.Transactor._

object Database {

  def createTable(transactor: Aux[IO, Unit]) = sql"""CREATE TABLE person (
                    id SERIAL,
                    name VARCHAR,
                    age int)""".update.run.transact(transactor)

  def addPersonToDB(
      person: Models.Models.InputPerson,
      transactor: Aux[IO, Unit]
  ) = for {
    n <-
      sql"insert into person (name,age) values (${person.name}, ${person.age}) returning id"
        .query[Int]
        .unique
        .transact(transactor)
    _ <- IO.println(s"n = $n")
  } yield n

  def selectPersons(n: Int, transactor: Aux[IO, Unit]) = for {
    list <- sql"select * from person"
      .query[Models.Models.Person]
      .stream
      .take(n)
      .compile
      .toList
      .transact(transactor)
    _ <- IO.println(list)
  } yield list

  def selectPerson(personId: Int, transactor: Aux[IO, Unit]) = for {
    somePerson <- sql"select * from person where id = $personId"
      .query[Models.Models.Person]
      .option
      .transact(transactor)
    _ <- IO.println(somePerson)
  } yield somePerson

  def updatePersonInDB(
      newPerson: Models.Models.Person,
      transactor: Aux[IO, Unit]
  ) =
    for {
      n <-
        sql"update person set age = ${newPerson.age}, name = ${newPerson.name} where id = ${newPerson.id}".update.run
          .transact(transactor)
      _ <- IO.println(s"updated $n persons")

    } yield n

  def deletePersonFromDb(id: Int, transactor: Aux[IO, Unit]) = for {
    n <- sql"delete from person where id = $id".update.run.transact(transactor)
  } yield n

  // val testInput = new Models.Models.InputPerson("Jack", 23)

  // val testUpdate = new Models.Models.Person(1, "Jane", 24)

  // override def run(args: List[String]) =
  //  updatePerson(testUpdate).as(ExitCode.Success)
  // selectPersons(3).as(ExitCode.Success)
  //  addPersonToDB(testInput).as(ExitCode.Success)
  // IO().use(_ => IO.never).as(ExitCode.Success)

  // val a = io.unsafeRunSync()

}
