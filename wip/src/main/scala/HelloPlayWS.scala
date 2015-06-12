import com.ning.http.client.AsyncHttpClientConfig
import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat
import org.scalatest.Matchers
import play.api.libs.ws.ning.NingWSClient
import play.api.libs.ws.{WS, WSClient}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future, ExecutionContext, ExecutionContextExecutor}
import scala.io.Source

object HelloPlayWS extends App with Matchers {
  implicit val ec: ExecutionContextExecutor = scala.concurrent.ExecutionContext.Implicits.global
  val builder = new AsyncHttpClientConfig.Builder
  builder.setUserAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_18_8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2272.45 Safari/537.33")
  implicit val wsClientConfig: WSClient = NingWSClient(builder.build())

  try {
    val fGoogleResult: Future[String] = {
      getHistoricalDataGoogle("NYSE:MET", LocalDate.parse("2015-08-18").minusMonths(3), LocalDate.parse("2015-08-18"))
    }
    val fYahooResult = getHistoricalDataYahoo("MET", LocalDate.parse("2015-08-18").minusMonths(3), LocalDate.parse("2015-08-18"), "d")

    val googleResult = Await.result(fGoogleResult, Duration.Inf)
    println(googleResult)
    assert(Source.fromString(googleResult).getLines().toList.contains("1-Jun-15,52.09,52.50,51.90,52.27,4523598"))
    val yahooResult = Await.result(fYahooResult, Duration.Inf)
    println(yahooResult)
    assert(Source.fromString(yahooResult).getLines().toList.contains("2015-06-01,52.09,52.50,51.900002,52.27,4523600,51.91954"))

  } finally {
    wsClientConfig.close()
  }

    /**
     * http://www.google.com/finance/historical?q=NASDAQ:NFLX&output=csv
     *
     * q = exchange:symbol
     * startdate = Mar 10, 2009 // "%b %d, %Y"
     * enddate
     *
     * Date,Open,High,Low,Close,Volume
     */
    def getHistoricalDataGoogle(symbol: String, start: LocalDate, end: LocalDate)(implicit ec: ExecutionContext) = {
      val q = symbol
      val formatter = DateTimeFormat.forPattern("MMM. d, y")
      val startdate = formatter.print(start)
      val enddate = formatter.print(end)
      val url = s"http://www.google.com/finance/historical"
      val req = WS.clientUrl(url)
        .withQueryString(
          "q" -> q,
          "startdate" -> startdate,
          "enddate" -> enddate,
          "output" -> "csv"
        )
      println("%s?%s".format(
        url,
        req.queryString.toVector.flatMap {
          case (k, vs) =>
            vs.map(v => "%s=%s".format(k, v))
        }.mkString("&")
      ))
      req.queryString.toVector.flatMap {
        case (k, vs) =>
          vs.map(v => "%s=%s".format(k, v))
      }.mkString("&")

      req.get().map(_.body)
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
  def getHistoricalDataYahoo(symbol: String, start: LocalDate, to: LocalDate, interval: String): Future[String] = {
    val a = start.getMonthOfYear - 1
    val b = start.getDayOfMonth
    val c = start.getYear
    val d = to.getMonthOfYear - 1
    val e = to.getDayOfMonth
    val f = to.getYear
    val g = interval
    val url = s"http://ichart.finance.yahoo.com/table.csv?s=$symbol&d=$d&e=$e&f=$f&g=$g&a=$a&b=$b&c=$c&ignore=.csv"
    println(url)
    WS.clientUrl(url)
      .get()
      .map(_.body)
  }

}