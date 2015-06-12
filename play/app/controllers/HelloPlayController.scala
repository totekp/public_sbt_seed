package controllers

import javax.inject._

import bsl4.Bsl4
import play.api.Application
import play.api.i18n._
import play.api.mvc._

import scala.concurrent.ExecutionContext

class HelloPlayController @Inject() (val messagesApi: MessagesApi)(implicit ec: ExecutionContext, app: Application)
  extends Controller
  with I18nSupport {

  def index = Action { implicit req =>
    Ok(views.html.helloTemplate(Bsl4.name))
  }

}
