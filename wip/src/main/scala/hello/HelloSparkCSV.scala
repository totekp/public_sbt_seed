package hello

import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{DataFrame, SQLContext}
import org.apache.spark.{SparkConf, SparkContext}
import org.scalatest.Matchers

import scala.util.control.NonFatal
import scala.util.hashing.MurmurHash3

object HelloSparkCSV extends Matchers {

  def hash(in: String): Long = {
    MurmurHash3.stringHash(in)
  }

  def run(sc: SparkContext): Unit = {
    val sqlContext = new SQLContext(sc)

    val df: DataFrame = {
      sqlContext.read
        .format("com.databricks.spark.csv")
        .options(Map("header" -> "true", "path" -> "test.csv"))
        .load()
    }
    case class Tube(aa: String, bb: String, cc: String)
    val tubeRows: RDD[Tube] = {
      df.select("aa", "bb", "cc").map(x => Tube(x(0).toString, x(1).toString, x(2).toString))
    }
  }


  def main(args: Array[String]) {
    val conf = new SparkConf().setMaster("local[2]").setAppName("HelloSparkCSV")
    val sc = new SparkContext(conf)
    try {
      run(sc)
    } catch {
      case NonFatal(t) =>
        throw t
    } finally {
      sc.stop()
    }
  }

}