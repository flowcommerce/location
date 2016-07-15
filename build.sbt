import play.PlayImport.PlayKeys._
import scoverage.ScoverageSbtPlugin.ScoverageKeys._

name := "location"

scalaVersion in ThisBuild := "2.11.8"

// required because of issue between scoverage & sbt
parallelExecution in Test in ThisBuild := true

lazy val generated = project
  .in(file("generated"))
  .enablePlugins(PlayScala)
  .settings(
    libraryDependencies ++= Seq(
      ws
    )
  )

lazy val api = project
  .in(file("api"))
  .dependsOn(generated)
  .aggregate(generated)
  .enablePlugins(PlayScala)
  .enablePlugins(NewRelic)
  .settings(commonSettings: _*)
  .settings(
    routesImport += "io.flow.location.v0.Bindables._",
    routesGenerator := InjectedRoutesGenerator,
    libraryDependencies ++= Seq(
      ws,
      "io.flow" %% "lib-play" % "0.1.37",
      "io.flow" %% "lib-reference" % "0.0.91",
      "org.scalatestplus" %% "play" % "1.4.0" % "test",
      "com.sanoma.cda" %% "maxmind-geoip2-scala" % "1.5.1",
      "com.google.maps" % "google-maps-services" % "0.1.15"
    )
  )

lazy val commonSettings: Seq[Setting[_]] = Seq(
  name <<= name("location-" + _),
  libraryDependencies ++= Seq(
    specs2 % Test,
    "org.scalatest" %% "scalatest" % "2.2.6" % Test
  ),
  scalacOptions += "-feature",
  coverageHighlighting := true,
  resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases",
  resolvers += "Artifactory" at "https://flow.artifactoryonline.com/flow/libs-release/",
  credentials += Credentials(
    "Artifactory Realm",
    "flow.artifactoryonline.com",
    System.getenv("ARTIFACTORY_USERNAME"),
    System.getenv("ARTIFACTORY_PASSWORD")
  )
)
version := "0.0.17"
