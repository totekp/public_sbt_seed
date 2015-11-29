import java.io.ByteArrayOutputStream

import org.scalactic.TripleEqualsSupport.Spread
import org.scalatest._
import org.scalatest.prop.TableDrivenPropertyChecks

import scala.io.Source
import scala.math.BigDecimal.RoundingMode

trait RichFlatSpec
  <: FlatSpec
  with CoreScalaTestSpec {

}

trait RichFunSuite
  <: FunSuite
  with CoreScalaTestSpec {

}

trait CoreScalaTestSpec
  <: Matchers
  with Inside
  with OptionValues
  with EitherValues
  with PartialFunctionValues
  with Inspectors
  with TableDrivenPropertyChecks
  with BeforeAndAfter {

  self: Suite =>

  def outToString(fn: => Unit): String = {
    val baos: ByteArrayOutputStream = new java.io.ByteArrayOutputStream()
    Console.withOut(baos) {
      fn
    }
    baos.toString
  }

  def outToTee(fn: => Unit): String = {
    val baos: ByteArrayOutputStream = new java.io.ByteArrayOutputStream()
    Console.withOut(baos) {
      fn
    }
    val out = baos.toString
    println(out)
    out
  }

  def getLines(in: String): Array[String] = {
    Source.fromString(in).getLines().toArray
  }

  def splitLineTrim(line: String, splitter: String => Array[String]): Array[String] = {
    splitter(line).map(_.trim)
  }

  def round(value: Double, scale: Int): Double = {
    BigDecimal(value).setScale(scale, RoundingMode.HALF_UP).doubleValue()
  }

  implicit class DoublePimper(val d: Double) {
    def round1: Double = {
      round(d, 1)
    }

    def spread: Spread[Double] = {
      val scale = BigDecimal(d).scale
      Spread(d, BigDecimal(10).pow(-scale).doubleValue())
    }

    def round2: Double = {
      round(d, 2)
    }

    def round3: Double = {
      round(d, 3)
    }
  }

  implicit class GenericPimper[A >: Null](in: A) {
    def test(b: A = null): Unit = {
      b match {
        case null =>
          System.err.println(in)
        case _ =>
          in should be(b)
      }
    }

    def |>[U](f: A => U): U = pipe(f)

    def pipe[U](f: A => U): U = f(in)

    case class OverlapPredicate(cursorPos: Int, count: Int)

    import scala.annotation.tailrec
    def countSubstring(substring: String, canOverlap: Option[OverlapPredicate => Int] = None): Int = {
      val text = in.toString
      @tailrec
      def count(pos: Int, cc: Int): Int = {
        val nextIdx = text.indexOf(substring, pos)
        if (nextIdx == -1) {
          cc
        }
        else {
          canOverlap match {
            case Some(pred) =>
              val predValue = pred(OverlapPredicate(pos, cc))
              assert(predValue > 0, "predValue should be > 0")
              count(nextIdx + predValue, cc + 1)
            case None =>
              count(nextIdx + substring.length, cc + 1)
          }
        }
      }
      count(0, 0)
    }

    def countSubstringRegex(regex: String): Int = {
      val text = in.toString
      regex.r.findAllMatchIn(text).length
    }
  }

}