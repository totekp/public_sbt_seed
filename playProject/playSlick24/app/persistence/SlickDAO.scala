package persistence

import slick.dbio
import slick.dbio.Effect.{Read, Transactional, Write}
import slick.driver.PostgresDriver.api._
import slick.jdbc.JdbcBackend

import scala.concurrent.{ExecutionContext, Future}

trait GuidModel {
  def guid: String
}

trait GuidTable[T <: GuidModel] extends Table[T] {
  def guid: Rep[String]
  //  def guidIndex = index("guid", guid, unique = true)
}

// Influence: https://github.com/7thsense/utils-persistence/blob/master/src/main/scala/com/theseventhsense/utils/persistence/db/SlickDAO.scala
abstract class AbstractGuidSlickDAO[T <: GuidModel, TableType <: GuidTable[T]](
  val db: JdbcBackend#DatabaseDef,
  val table: TableQuery[TableType]
                                                )(
  implicit val ec: ExecutionContext) {

//  implicit val ec: ExecutionContext
//  val db: JdbcBackend#DatabaseDef

//  type TableType <: GuidTable[T]
//  val table: TableQuery[TableType]

  def tableName: String = table.baseTableRow.tableName

  def count: Future[Long] = {
    db.run(table.length.result).map(_.toLong)
  }

  def saveAction(obj: T, ow: Boolean, upsert: Boolean): dbio.DBIOAction[T, NoStream, Read with Write with Transactional] = {
    (ow, upsert) match {
      case (true, _) =>
        table.insertOrUpdate(obj).map(count => obj)
      case (false, true) =>
        val action = for {
          optExisting <- table.filter(_.guid === obj.guid).result.headOption
          r <- {
            optExisting match {
              case Some(existingUser) =>
                DBIO.successful(existingUser)
              case None =>
                (table += obj).flatMap {
                  case 1 =>
                    DBIO.successful(obj)
                  case 0 =>
                    DBIO.failed(new Exception("Error while inserting item"))
                }
            }
          }
        } yield {
            r
          }
        action.transactionally
      case (false, false) =>
        throw new Exception("Invalid state: no ow && no upsert")
    }
  }

  def findOrSaveAction(item: T): dbio.DBIOAction[T, NoStream, Read with Write with Effect with Transactional] = {
    val action = for {
      optSaved <- table.filter(_.guid === item.guid).result.headOption
      result <- optSaved match {
        case None =>
          (table += item).flatMap {
            case 1 =>
              DBIO.successful(item)
            case 0 =>
              DBIO.failed(new Exception("Error while inserting item"))
          }
        case Some(saved) =>
          DBIO.successful(item)
      }
    } yield {
        result
      }
    action.transactionally
  }
}