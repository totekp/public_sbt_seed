package hello

import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._

object HelloPlayJson extends App {

  case class Door(key: String, room: Door)

  def trim(implicit r: Reads[String]): Reads[JsString] = r.map(s => JsString(s.trim))

  implicit val jsonFormat_Door: Format[Door] = {
    (
      (__ \ "key").format[String](trim andThen minLength[String](1) keepAnd maxLength[String](50)) and
      (__ \ "room").lazyFormatNullable(jsonFormat_Door).inmap[Door](
        _.orNull, Option.apply
      )
    )(Door.apply, unlift(Door.unapply))
  }

  val test = Door("AA", Door("BB", Door("CC", null)))
  val json = Json.toJson(test)
  println(json)
  println(Json.fromJson[Door](json))
  assert(Json.parse("""{"key":"AA","room":{"key":"BB","room":{"key":"CC"}}}""") == json)

}
