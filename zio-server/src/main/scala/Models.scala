package Models

import zio.json._
import zio._

object Models {

  case class InputPerson(
      val name: String,
      val age: Int
  )

  object InputPerson {
    implicit val decoder: JsonDecoder[InputPerson] =
      DeriveJsonDecoder.gen[InputPerson]
  }

  case class Person(id: Int, name: String, age: Int)

  object Person {
    implicit val encoder: JsonEncoder[Person] =
      DeriveJsonEncoder.gen[Person]

    implicit val decoder: JsonDecoder[Person] =
      DeriveJsonDecoder.gen[Person]
  }

}
