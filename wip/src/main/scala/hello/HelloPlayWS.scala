package hello

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import com.ning.http.client.AsyncHttpClientConfig
import org.scalatest.Matchers
import play.api.libs.json.{Json, JsError, JsObject, JsResult}
import play.api.libs.ws.ning.NingWSClient
import play.api.libs.ws.{WS, WSClient}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutor, Future}
import scala.io.Source
import scala.concurrent.duration._

object HelloPlayWS extends App with Matchers {
  implicit val ec: ExecutionContextExecutor = scala.concurrent.ExecutionContext.Implicits.global
  val builder = new AsyncHttpClientConfig.Builder
  builder.setUserAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_18_8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2272.45 Safari/537.33")
  implicit val wsClientConfig: WSClient = NingWSClient(builder.build())
  val ws = WS

  try {
    println(getStockInfoGoogle(Seq("BAC")))
    val fGoogleResult = {
      Future.successful(getHistoricalDataGoogle("NYSE:MET", LocalDate.parse("2015-08-18").minusMonths(3), LocalDate.parse("2015-08-18")))
    }
    val fYahooResult = Future.successful(getHistoricalDataYahoo("MET", LocalDate.parse("2015-08-18").minusMonths(3), LocalDate.parse("2015-08-18"), "d"))

    val googleResult = Await.result(fGoogleResult, Duration.Inf)
    println(googleResult.keySet)
    println(googleResult)
    val googleAudit = "1-Jun-15,52.09,52.50,51.90,52.27,4523598"
    assert((googleResult.values.flatten.toSet intersect (googleAudit).split(",").toSet) ===
      (googleAudit).split(",").toSet)

    val yahooResult = Await.result(fYahooResult, Duration.Inf)
    println(yahooResult.keySet)
    println(yahooResult)
    val yahooAudit = "2015-06-01,52.09,52.50,51.900002,52.27,4523600,51.54075"
    val resultRow = {
      val ii = yahooResult("Date").indexWhere(_ == "2015-06-01")
      yahooResult.values.map(_(ii)).toVector
    }
    assert(resultRow.toSet === yahooAudit.split(",").toSet)

  } finally {
    wsClientConfig.close()
  }

  /**
  [
   {
      "id":"662744",
      "t":"BAC",
      "e":"NYSE",
      "l":"16.73",
      "l_fix":"16.73",
      "l_cur":"16.73",
      "s":"0",
      "ltt":"7:59PM EST",
      "lt":"Dec 11, 7:59PM EST",
      "lt_dts":"2015-12-11T19:59:34Z",
      "c":"-0.48",
      "c_fix":"-0.48",
      "cp":"-2.79",
      "cp_fix":"-2.79",
      "ccol":"chr",
      "pcls_fix":"17.21",
      "eo":"",
      "delay":"",
      "op":"16.97",
      "hi":"17.06",
      "lo":"16.64",
      "vo":"-",
      "avvo":"-",
      "hi52":"18.48",
      "lo52":"14.60",
      "mc":"179.09B",
      "pe":"12.39",
      "fwpe":"",
      "beta":"1.78",
      "eps":"1.35",
      "shares":"10.41B",
      "inst_own":"63%",
      "name":"Bank of America Corp",
      "type":"Company"
   }
]
   */
  def getStockInfoGoogle(tickers: Seq[String]): JsResult[Seq[JsObject]] = {
    val url = s"http://www.google.com/finance/info?infotype=infoquoteall&q=${tickers.mkString(",")}"
    ws.clientUrl(url)
      .get()
      .map {
        case r if r.status != 200 =>
          JsError(s"status: ${r.status}, body: ${r.body}")
        case r =>
          val jsonString = r.body.dropWhile(_ != '[')
          Json.parse(jsonString).validate[Seq[JsObject]]
      }.get(15.seconds)
  }

  implicit class FuturePimper[A](aa: Future[A]) {
    def get(duration: FiniteDuration) = {
      Await.result(aa, duration)
    }
  }

  /**
  http://www.google.com/finance/historical?q=NYSE:GOOG&output=csv
q = exchange:symbol
startdate = Mar 10, 2009 // "%b %d, %Y"
enddate

      ﻿Date,Open,High,Low,Close,Volume
    */
  def getHistoricalDataGoogle(symbol: String, start: LocalDate, end: LocalDate): Map[String, IndexedSeq[String]] = {
    val q = symbol
    val formatter = DateTimeFormatter.ofPattern("MMM. d, y")
    val startdate = formatter.format(start)
    val enddate = formatter.format(end)
    val url = s"http://www.google.com/finance/historical"
    val req = ws.clientUrl(url)
      .withQueryString(
        "q" -> q,
        "startdate" -> startdate,
        "enddate" -> enddate,
        "output" -> "csv"
      )

    val body = req.get()
          .get(15.seconds)
          .body
          .substring(1) // google quirk
        val lines = body.split("\n")
        val labels = lines.head.split(",")
        val data = lines.drop(1).map(_.split(","))
        val result = {
          for {
            (key, i) <- labels.zipWithIndex
          } yield {
            (key, data.map(_(i)).toVector)
          }
        }
        result.toMap
  }
  /**
    *
    * Date,Open,High,Low,Close,Volume,Adj Close
    *
    * @param symbol
    * @param start
    * @param to
    * @param interval w, d daily
    */
  def getHistoricalDataYahoo(symbol: String, start: LocalDate, to: LocalDate, interval: String): Map[String, IndexedSeq[String]] = {
    val a = start.getMonthValue - 1
    val b = start.getDayOfMonth
    val c = start.getYear
    val d = to.getMonthValue - 1
    val e = to.getDayOfMonth
    val f = to.getYear
    val g = interval
    val url = s"http://ichart.finance.yahoo.com/table.csv?s=$symbol&d=$d&e=$e&f=$f&g=$g&a=$a&b=$b&c=$c&ignore=.csv"

    val body = ws.clientUrl(url)
      .get()
      .get(15.seconds)
      .body
    val lines = body.split("\n")
    val labels = lines.head.split(",")
    val data = lines.drop(1).map(_.split(","))
    val result = {
      for {
        (key, i) <- labels.zipWithIndex
      } yield {
        (key, data.map(_(i)).toVector)
      }
    }
    result.toMap
  }

}