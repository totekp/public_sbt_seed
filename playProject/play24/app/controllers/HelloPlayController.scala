package controllers

import bsl4.Bsl4
import com.google.inject.Inject
import play.api.Application
import play.api.i18n._
import play.api.mvc._

import scala.concurrent.ExecutionContext

class HelloPlayController @Inject() (val messagesApi: MessagesApi)(implicit ec: ExecutionContext, app: Application)
  extends Controller
  with I18nSupport {

  def index: Action[AnyContent] = Action { implicit req =>
    Ok(views.html.helloTemplate(Bsl4.name))
  }

}
