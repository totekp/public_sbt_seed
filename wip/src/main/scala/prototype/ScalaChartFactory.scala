package prototype

import org.jfree.chart.ChartFactory
import org.jfree.data.xy.{XYSeries, XYSeriesCollection}

import scalax.chart.XYChart
import scalax.chart.module.ChartFactories.XYLineChart

object ScalaChartFactory {

  def createLineChart(
                       title: String,
                       domainLabel: String,
                       rangeLabel: String,
                       data: Seq[(String, IndexedSeq[(Double, Double)])]
                     ): XYChart = {
    val chart = XYLineChart.apply(data)
    chart.title = title
    chart.plot.getDomainAxis.setLabel(domainLabel)
    chart.plot.getRangeAxis.setLabel(rangeLabel)
    chart
  }

  def createScatterChart(
                          title: String,
                          domainLabel: String,
                          rangeLabel: String,
                          data: Seq[(String, IndexedSeq[(Double, Double)])]
                        ): XYChart = {
    val series: Seq[XYSeries] = {
      for {
        (seriesName, seriesData) <- data
        series = {
          val ss = new XYSeries(seriesName)
          seriesData.foreach { case (x, y) => ss.add(x, y) }
          ss
        }
      } yield {
        series
      }
    }
    val collection: XYSeriesCollection = new XYSeriesCollection()
    series.foreach(collection.addSeries)

    val jChart = ChartFactory.createScatterPlot(title, domainLabel, rangeLabel, collection)
    val chart = XYChart.fromPeer(jChart)
    chart
  }
}
