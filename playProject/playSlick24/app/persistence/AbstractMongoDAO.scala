package persistence

import play.api.libs.json._
import play.modules.reactivemongo.json._
import play.modules.reactivemongo.json.collection.JSONCollection
import reactivemongo.api.commands.{DefaultWriteResult, WriteResult}
import reactivemongo.api.indexes.Index
import reactivemongo.bson.BSONDocument

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.{ExecutionContext, Future}

trait Identifiable {
  def id: String
}

abstract class AbstractMongoDAO[Model <: Identifiable](val coll: JSONCollection, val _jsonFormat: OFormat[Model]) {
  mongoDAO =>

  private val ID = "_id"

  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.global

  implicit val jsonFormat: OFormat[Model] = _jsonFormat

  implicit class JsValueHelper(val j: JsValue) {
    def toBson: BSONDocument = {
      ImplicitBSONHandlers.jsWriter.write(j)
    }
  }

  implicit class JsObjectHelper(val j: JsObject) {
    def toBson: BSONDocument = {
      ImplicitBSONHandlers.JsObjectWriter.write(j)
    }
  }

  /*
    val zomIndex = List(
      Index(Seq("god" -> IndexType.Descending), unique = true)
    )
    Future.sequence(zomIndex.map(this.coll.indexesManager.ensure))
   */
  val indices = ArrayBuffer.empty[Index]

  def ensureIndexes(): Future[Vector[Boolean]] = {
    Future.sequence(indices.toVector.map(this.coll.indexesManager.ensure))
  }

  def drop(): Future[Unit] = {
    this.coll.drop()
  }

  def findOne(id: String): Future[Option[Model]] = {
    this.coll.find(Json.obj(ID -> id)).one[Model]
  }

  def find(query: JsObject): Future[Seq[Model]] = {
    this.coll.find(query).cursor[Model]().collect[Vector]()
  }

  /**
   *
   * @param obj
   * @param ow
   * @param upsert usually true
   * @return
   */
  def save(obj: Model, ow: Boolean, upsert: Boolean): Future[WriteResult] = {
    if (!ow && !upsert) {
      this.coll.insert(obj)
    } else {
      if (ow) {
        this.coll.update(Json.obj(ID -> obj.id), obj, upsert = upsert)
      } else {
        for {
          a <- this.coll.find(Json.obj(ID -> obj.id)).one[BSONDocument]
          b <- {
            if (a.nonEmpty) Future.successful(DefaultWriteResult(ok = true, 0, Seq.empty, None, None, None))
            else this.coll.update(Json.obj(ID -> obj.id), obj, upsert = upsert)
          }
        } yield {
          b
        }
      }
    }
  }

  def deleteById(id: String): Future[WriteResult] = {
    this.coll.remove(Json.obj(ID -> id))
  }

  /**
   *
   * @param obj
   * @param upsert usually false
   * @return
   */
  def update(obj: Model, upsert: Boolean): Future[WriteResult] = {
    save(obj, ow = true, upsert)
  }
}