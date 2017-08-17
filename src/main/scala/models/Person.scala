package models

import java.util.UUID

import ru.yudnikov.core.modeling.{Manager, Model, Reference}

/**
  * Created by Don on 16.08.2017.
  */
case class Person(
                   name: String,
                   parent: Option[Reference[Person]],
                   id: UUID = UUID.randomUUID()
                 ) extends Model(Person)

object Person extends Manager[Person]
