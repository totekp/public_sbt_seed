
import java.io.{File, PrintWriter}

import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}

import scala.util.control.NonFatal

object HelloSpark {
  def run(sc: SparkContext) {
    val logFile = "project/Dependencies.scala"
    val logData = sc.textFile(logFile, 2).cache()
    val wordCount: RDD[(String, Int)] = {
      logData
        .flatMap(_.toLowerCase.replaceAll("\\W+", " ").split("\\s+"))
        .filter(_.length > 0)
        .map(k => (k, 1))
        .reduceByKey(_ + _)
    }
    val result: Vector[(String, Int)] = wordCount.collect().sortBy(- _._2).toVector
    println(result)
//    val pw = new PrintWriter(new File("HelloSpark.out.txt"))
//    pw.println(result.map {case (w, c) => s"$w,$c"}.mkString("\n"))
//    pw.close()
    sc.stop()
  }

  def main(args: Array[String]) {
    val conf = new SparkConf().setMaster("local[2]").setAppName("HelloSparkGraphX")
    val sc = new SparkContext(conf)
    try {
      run(sc)
    } catch {
      case NonFatal(t) =>
        throw t
    } finally  {
      sc.stop()
    }
  }
}