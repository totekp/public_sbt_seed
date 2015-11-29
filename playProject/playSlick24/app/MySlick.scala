import play.api.Play
import play.api.db.slick.DatabaseConfigProvider
import slick.driver.JdbcProfile
import slick.driver.PostgresDriver.api._
import slick.jdbc.JdbcBackend

object MySlick {

  val dbFromPlay: JdbcBackend#DatabaseDef = DatabaseConfigProvider.get[JdbcProfile](Play.current).db
  val dbFromConfig: JdbcBackend#DatabaseDef = Database.forConfig("slick.dbs.default")

  // TODO play24 stuff
}
