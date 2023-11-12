package Models

import io.circe.syntax._
import io.circe._
import io.circe.literal._
import org.http4s.circe._
import cats.effect._

import io.circe.generic.auto._

import org.http4s.EntityDecoder

object Models {

  case class Person(
      name: String,
      age: Int,
      childrens: List[String],
      spouse: Option[String]
  )

  object Person {
    implicit val PersonEncoder: Encoder[Person] =
      Encoder.instance { person: Person =>
        json"""{"name": ${person.name}, "age": ${person.age}, "childrens": ${person.childrens}, "spose": ${person.spouse}}"""
      }

    implicit val personDecoder: EntityDecoder[IO, Person] = jsonOf[IO, Person]

  }

}
