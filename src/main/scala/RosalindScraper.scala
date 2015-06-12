import java.util.concurrent.TimeUnit

import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import org.jsoup.Jsoup
import play.api.libs.json._

import scala.collection.JavaConverters._
import scala.collection.parallel.immutable.ParSeq
import scala.util.control.NonFatal
import scala.util.{ Success, Try }
import scala.concurrent.duration._

object RosalindScraper extends App {

  case class Sample(
    id: String,
    in: String,
    out: String,
    reference: Option[String] = None,
    source: Option[String] = None)
  object Sample {
    val itemName = "sample"

    implicit lazy val jsonFormat: OFormat[Sample] = {
      val format = Json.format[Sample]
      OFormat(
        format,
        OWrites[Sample] { o =>
          format.writes(o).asInstanceOf[JsObject] ++
            Json.obj(
              "itemName" -> itemName,
              "writeTime" -> DateTime.now()
            )
        })
    }
  }

  implicit val isoDateTimeFormat: Format[DateTime] = Format[DateTime](
    Reads { jsValue =>
      try {
        jsValue.validate[String].map(ISODateTimeFormat.dateTime.parseDateTime)
      } catch {
        case NonFatal(e) =>
          JsError("validate.error.expected.isodatetime")
      }
    },
    Writes { dateTime => JsString(ISODateTimeFormat.dateTime.print(dateTime)) }
  )

  def getProblemIds(url: String): List[String] = {
    val doc = Jsoup.connect(url).get()
    doc.select("html > body > div.container.main > table.problem-list > tbody > tr").listIterator().asScala.toList
      .map(_.child(0).text())
  }

  def process(id: String, url: String, reference: Option[String] = None): Try[Sample] = Try {
    val doc = Jsoup.connect(url).get
    val pres = doc.select("html > body > div.container.main > div.problem-statement pre")
    val texts = pres.listIterator().asScala.map(_.text()).toVector.takeRight(2)
    assert(texts.size == 2, s"$id texts.size == 2: ${texts}")
    Sample(id, texts(0), texts(1), reference, source = Some(url))
  }

  val t0 = System.nanoTime()
  // http://rosalind.info/problems/list-view/
  // http://rosalind.info/problems/list-view/?location=bioinformatics-textbook-track
  // http://rosalind.info/problems/list-view/?location=algorithmic-heights
  // http://rosalind.info/problems/list-view/?location=bioinformatics-armory
  val url = "http://rosalind.info/problems/list-view/"
  val possibleSamples: ParSeq[Try[Sample]] = getProblemIds(url).par
    .map {
      id =>
        println(s"Fetching ${id}")
        val s = process(id, s"http://rosalind.info/problems/$id/", Some(url))
        println(s"Fetched ${id}")
        s
    }

  def getInputName(id: String) = s"../resources/rosalind_a/${id}.txt"

  def getOutputName(id: String) = s"../resources/rosalind_a/${id}.txt2"

  val samples = possibleSamples.collect { case Success(sample) => sample }
  samples.foreach {
    sample =>
      //      val a = new PrintWriter(new File(getInputName(sample.id)))
      //      val b = new PrintWriter(new File(getOutputName(sample.id)))
      //      a.println(sample.in)
      //      b.println(sample.out)
      //      a.close()
      //      b.close()
      println(
        s"""
          |${sample.id}
          |-
          |${Json.stringify(Json.toJson(sample))}
          |-
          |${sample.in}
          |-
          |${sample.out}
        """.stripMargin.trim)
  }
  val t1 = System.nanoTime()
  val elapsed0 = (t1 - t0).nanoseconds
  println(s"Downloaded ${samples.size} samples out of ${possibleSamples.size} possible samples in ${elapsed0.toUnit(TimeUnit.SECONDS)} seconds.")
}

