import sbt.Keys._

import scalariform.formatter.preferences._

name := "sbt_seed"

val commonSettings: Seq[Setting[_]] = Seq(
  organization := "user.kzhou",
  version := "8.8-SNAPSHOT",
  scalaVersion := Library.ScalaVersion,
  resolvers ++= Seq(
    Resolvers.typesafeReleases
    , Resolvers.sonatypeReleases
    , Resolvers.sonatypeSnapshots
    , Resolvers.scalazBintrayReleases
    , Resolvers.jzy3dReleases
  ),
  scalacOptions ++= Seq(
    // https://github.com/scala/scala/blob/2.11.x/src/compiler/scala/tools/nsc/settings
    "-deprecation" // Emit warning and location for usages of deprecated APIs.
    , "-feature" // Emit warning and location for usages of features that should be imported explicitly.
    , "-unchecked" // Enable additional warnings where generated code depends on assumptions.
    //    , "-Xfatal-warnings" // Fail the compilation if there are any warnings.
    , "-Xlint" // Enable recommended additional warnings.
  ),
  testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-oDF")
)

defaultScalariformSettings // scalariformSettings

ScalariformKeys.preferences := ScalariformKeys.preferences.value
  .setPreference(FormatXml, false)
  .setPreference(DoubleIndentClassDeclaration, false)
  .setPreference(PreserveDanglingCloseParenthesis, true)

addCommandAlias("du", "dependencyUpdates")

lazy val root = {
  Project(id = "root", base = file("."))
    .settings(commonSettings: _*)
    .settings(
      libraryDependencies ++= Seq(
        Library.Jsoup
        , Library.PlayLibrary
        , Library.ScalaTest % Test
      )
    )
    .dependsOn(wip, updater, bsl1, bsl2, bsl3, bsl4, play, playSlick24)
    .aggregate(wip, updater, bsl1, bsl2, bsl3, bsl4, play, playSlick24)
}

lazy val wip = {
  project.settings(commonSettings: _*)
    .settings(
      libraryDependencies ++= Seq(
        Library.SparkCore
        , Library.SparkMLib
        , Library.SparkGraphX
        , Library.SparkCSV
        , Library.Experimental.AkkaStream
        , Library.ScalaTest
        , ws
      ),
      dependencyOverrides ++= Set(
        "com.fasterxml.jackson.core" % "jackson-databind" % "2.4.4"
      )
    )
}

lazy val updater = {
  project.settings(commonSettings: _*)
    .settings(libraryDependencies ++= Library.ALL)
}

lazy val bsl1 = {
  project.settings(commonSettings: _*)
    .settings(libraryDependencies ++= Seq(Library.Experimental.AkkaStream, Library.Experimental.Parboiled2))
}

lazy val bsl2 = {
  project.settings(commonSettings: _*)
    .dependsOn(bsl1 % "test->test;compile->compile")
    .aggregate(bsl1)
}

lazy val bsl3 = {
  project.settings(commonSettings: _*)
    .dependsOn(bsl2 % "test->test;compile->compile")
    .aggregate(bsl2)
}

lazy val bsl4 = {
  project.settings(commonSettings: _*)
    .dependsOn(bsl3 % "test->test;compile->compile")
    .aggregate(bsl3)
}

lazy val play = {
  project.settings(commonSettings: _*)
    .enablePlugins(PlayScala)
    .settings(
      // Play provides two styles of routers, one expects its actions to be injected, the
      // other, legacy style, accesses its actions statically.
      routesGenerator := InjectedRoutesGenerator,
      routesImport ++= Seq(
      ),
      TwirlKeys.templateImports ++= Seq(
      ),
      pipelineStages := Seq(uglify, digest, gzip)
      , libraryDependencies ++= Seq(specs2 % Test)
      , includeFilter in (Assets, LessKeys.less) := "*.less"
      , excludeFilter in (Assets, LessKeys.less) := "_*.less"
    )
    .dependsOn(bsl4)
    .aggregate(bsl4)
}

lazy val playSlick24 = {
  project.settings(commonSettings: _*)
    .enablePlugins(PlayScala)
    .settings(
      // Play provides two styles of routers, one expects its actions to be injected, the
      // other, legacy style, accesses its actions statically.
      routesGenerator := InjectedRoutesGenerator,
      routesImport ++= Seq(
      ),
      TwirlKeys.templateImports ++= Seq(
      ),
      pipelineStages := Seq(uglify, digest, gzip)
      , libraryDependencies ++= Seq(specs2 % Test)
      , includeFilter in(Assets, LessKeys.less) := "*.less"
      , excludeFilter in(Assets, LessKeys.less) := "_*.less"
      , libraryDependencies ++= Seq(
        Library.PlaySlick
        , Library.PlaySlickEvolutions
        , Library.Slf4jNop
        , Library.HikariCP
        , Library.PostgresDriver
        , Library.PlayReactiveMongo
      )
    )
}