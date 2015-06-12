import sbt._

object Library {
  // http://www.scala-lang.org/news/2.11.7
  val ScalaVersion: String = "2.11.7"
  val ScalaLibrary = "org.scala-lang" % "scala-library" % ScalaVersion

  // http://akka.io/news/2015/09/16/akka-2.3.14-released.html
  val AkkaVersion: String = "2.3.14"
  val AkkaActor: ModuleID = "com.typesafe.akka" %% "akka-actor" % AkkaVersion
  val AkkaCluster = "com.typesafe.akka" %% "akka-cluster" % AkkaVersion
  val AkkaSlf4j = "com.typesafe.akka" %% "akka-slf4j" % AkkaVersion
  val AkkaTestkit = "com.typesafe.akka" %% "akka-testkit" % AkkaVersion

  // https://spark.apache.org/releases/spark-release-1-5-0.html
  val SparkVersion = "1.5.0"
  val SparkCore = "org.apache.spark" %% "spark-core" % SparkVersion
  val SparkMLib = "org.apache.spark" %% "spark-mllib" % SparkVersion
  val SparkGraphX = "org.apache.spark" %% "spark-graphx" % SparkVersion
  val SparkCSV = "com.databricks" %% "spark-csv" % "1.2.0"

  val BreezeVersion = "0.11.2"
  val BreezeParent = "org.scalanlp" %% "breeze-parent" % BreezeVersion
  //  val BreezeNatives = "org.scalanlp" %% "breeze-natives" % BreezeVersion

  // # db stuff
  // http://reactivemongo.org/releases/0.11/documentation/tutorial/play2.html (https://github.com/ReactiveMongo/Play-ReactiveMongo/issues/125)
  val PlayReactiveMongo = "org.reactivemongo" %% "play2-reactivemongo" % "0.11.6.play24" // reactive mongo
  val PlayReactiveMongoKzhou = "org.reactivemongo" %% "kzhou-play2-reactivemongo" % "0.11.x.fork" // reactive mongo

  // # play stuff
  // https://www.playframework.com/changelog
  val PlayLibrary = "com.typesafe.play" %% "play" % "2.4.3"
  val PlayMailer = "com.typesafe.play" %% "play-mailer" % "3.0.1"
  // https://www.playframework.com/documentation/2.4.x/PlaySlick
  // slick is transitive
  val PlaySlick = "com.typesafe.play" %% "play-slick" % "1.0.1"
  // evolutions is transitive
  val PlaySlickEvolutions = "com.typesafe.play" %% "play-slick-evolutions" % "1.0.1"

  // # other stuff
  val LogbackClassic = "ch.qos.logback" % "logback-classic" % "1.1.3"
  val ScalaBcrypt = "com.github.t3hnar" %% "scala-bcrypt" % "2.4"
  val StringMetrics = "com.rockymadden.stringmetric" %% "stringmetric-core" % "0.27.4"
  val JodaMoney = "org.joda" % "joda-money" % "0.10.0"
  val Jsap = "com.martiansoftware" % "jsap" % "2.1"
  val ApacheCommonsMath3 = "org.apache.commons" % "commons-math3" % "3.5"
  val TypesafeConfig = "com.typesafe" % "config" % "1.3.0"
  val Jsoup = "org.jsoup" % "jsoup" % "1.8.2"
  // stable url: https://github.com/stripe/stripe-java, https://github.com/stripe/stripe-java/blob/master/CHANGELOG
  val StripeJava = "com.stripe" % "stripe-java" % "1.37.0"
  // stable url: http://slick.typesafe.com/news/all-news.html
  // http://slick.typesafe.com/news/2015/09/11/slick-3.0.3-released.html
  val Slick = "com.typesafe.slick" %% "slick" % "3.0.3"
  val Slf4jNop = "org.slf4j" % "slf4j-nop" % "1.6.4"
  val H2 = "com.h2database" % "h2" % "1.3.175"
  val HikariCP = "com.zaxxer" % "HikariCP" % "2.4.1"
  val PostgresDriver = "org.postgresql" % "postgresql" % "9.4-1202-jdbc42"

  // # visual
  val JFreeChart = "org.jfree" % "jfreechart" % "1.0.19"
  val Wisp = "com.quantifind" %% "wisp" % "0.0.4"
  // http://www.jzy3d.org/download-0.9.1.php
  val Jzy3d = "org.jzy3d" % "jzy3d-api" % "0.9.1"

  // # testing libs
  // stable url: https://github.com/etorreborre/specs2/blob/master/notes
  // https://github.com/etorreborre/specs2/blob/master/notes/3.6.4.markdown
  val Specs2Core = "org.specs2" %% "specs2-core" % "3.6.4"
  // http://www.scalatest.org/release_notes (https://github.com/etorreborre/specs2/blob/master/notes/3.6.2.markdown)
  val ScalaTest = "org.scalatest" %% "scalatest" % "2.2.4"
  val ScalaTestPlus = "org.scalatestplus" %% "play" % "1.2.0"
  // https://github.com/rickynils/scalacheck/blob/master/RELEASE
  val ScalaCheck = "org.scalacheck" %% "scalacheck" % "1.12.4"
  // https://github.com/cucumber/cucumber-jvm/blob/master/History.md#124-2015-07-23
  val CucumberVersion = "1.2.4"
  val CucumberScala = "info.cukes" %% "cucumber-scala" % CucumberVersion
  val CucumberJUnit = "info.cukes" % "cucumber-junit" % CucumberVersion
  val CucumberJava = "info.cukes" % "cucumber-java" % CucumberVersion
  val JUnit = "junit" % "junit" % "4.12"
  val Mockito = "org.mockito" % "mockito-core" % "1.10.19"

  object Experimental {
    // http://akka.io/news/2015/07/15/akka-streams-1.0-released.html
    val AkkaStream = "com.typesafe.akka" %% "akka-stream-experimental" % "1.0"
    val Parboiled2 = "org.parboiled" %% "parboiled" % "2.2.0-SNAPSHOT"
  }

  val ALL = Seq(
    AkkaActor
    , AkkaSlf4j
    , AkkaCluster
    , AkkaTestkit
    , ScalaLibrary
    , LogbackClassic
    , SparkCore
    , SparkMLib
    , PlayReactiveMongo
    , PlayMailer
    , ScalaBcrypt
    , StringMetrics
    , PlayLibrary
    , Specs2Core
    , JodaMoney
    , Jsap
    , ApacheCommonsMath3
    , JFreeChart
    , Wisp
    , ScalaTest
    , ScalaTestPlus
    , ScalaCheck
    , CucumberScala
    , CucumberJUnit
    , CucumberJava
    , JUnit
    , Mockito
    , TypesafeConfig
    , Jzy3d
    , BreezeParent
    , Jsoup
    , StripeJava
    , Slick
    , Slf4jNop
    , HikariCP
    , PostgresDriver
  )

}

object Resolvers {
  val typesafeReleases: MavenRepository = "Typesafe Releases" at "https://repo.typesafe.com/typesafe/releases/"

  val sonatypeReleases = "Sonatype Releases" at "https://oss.sonatype.org/content/repositories/releases/"

  val sonatypeSnapshots = "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"

  val scalazBintrayReleases = "Scalaz Bintray Repo" at "https://dl.bintray.com/scalaz/releases"

  // Warn url is http. Best to put library in project
  val jzy3dReleases = "Jzy3d Releases" at "http://maven.jzy3d.org/releases"
}

// addSbtPlugin(..)
object Plugins {
  /**
   * Play plugin
   */
  val Play = "com.typesafe.play" % "sbt-plugin" % "2.4.3"

  /**
   * https://github.com/rtimush/sbt-updates
   */
  val SbtUpdates = "com.timushev.sbt" % "sbt-updates" % "0.1.9"

  /**
   * http://www.scala-js.org/doc/tutorial.html
   */
  val ScalaJs = "org.scala-js" % "sbt-scalajs" % "0.6.4"

  /**
   * https://github.com/heroku/sbt-heroku
   */
  val SbtHeroku = "com.heroku" % "sbt-heroku" % "0.4.3"

  /**
   * https://github.com/sbt/sbt-scalariform
   */
  val SbtScalariform = "com.typesafe.sbt" % "sbt-scalariform" % "1.3.0"

  // TODO http://www.scala-sbt.org/sbt-pgp/index.html

  // TODO https://github.com/sbt/sbt-release

  // See plugins.sbt
}
