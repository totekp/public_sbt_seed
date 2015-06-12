
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source

import scala.concurrent.ExecutionContextExecutor

object HelloAkkaStreams {

  def main(args: Array[String]) {

    implicit val system: ActorSystem = ActorSystem("Sys")
    implicit val ec: ExecutionContextExecutor = system.dispatcher

    implicit val materializer: ActorMaterializer = ActorMaterializer()

    val text = scala.io.Source.fromFile("project/Dependencies.scala").getLines().mkString

    Source(() => text.split("\\s+").iterator)
      .map(_.toLowerCase.filter(_.isLetterOrDigit))
      .filter(_.trim.nonEmpty)
      .runForeach(println)
      .onComplete { possibleError => system.shutdown() }
  }
}