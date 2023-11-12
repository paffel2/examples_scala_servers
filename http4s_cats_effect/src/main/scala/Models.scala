package Models

import io.circe.syntax._
import io.circe._
import io.circe.literal._
import org.http4s.circe._
import cats.effect._

import io.circe.generic.auto._

import org.http4s.EntityDecoder

object Models {

  class InputPerson(
      val name: String,
      val age: Int
  )

  object InputPerson {
    implicit val personDecoder: EntityDecoder[IO, InputPerson] =
      jsonOf[IO, InputPerson]
  }

  case class Person(id: Int, override val name: String, override val age: Int)
      extends InputPerson(name, age)

  object Person {
    implicit val PersonEncoder: Encoder[Person] =
      Encoder.instance { person: Person =>
        json"""{"id": ${person.id}, "name": ${person.name}, "age": ${person.age}}"""
      }

    implicit val personDecoder: EntityDecoder[IO, Person] = jsonOf[IO, Person]

  }

}
