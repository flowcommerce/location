import play.PlayImport.PlayKeys._

name := "location"

scalaVersion in ThisBuild := "2.11.11"

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
    javaOptions in Test += "-Dconfig.file=conf/application.test.conf",
    routesImport += "io.flow.location.v0.Bindables._",
    routesGenerator := InjectedRoutesGenerator,
    libraryDependencies ++= Seq(
      ws,
      "io.flow" %% "lib-play" % "0.4.8",
      "io.flow" %% "lib-reference-scala" % "0.1.39",
      "org.scalatestplus" %% "play" % "1.4.0" % "test",
      "org.scalacheck" %% "scalacheck" % "1.13.5" % "test",
      "com.sanoma.cda" %% "maxmind-geoip2-scala" % "1.5.1",
      "com.google.maps" % "google-maps-services" % "0.2.4",
      "com.amazonaws" % "aws-java-sdk-s3" % "1.11.213"
    )
  )

lazy val commonSettings: Seq[Setting[_]] = Seq(
  name ~= ("location-" + _),
  libraryDependencies ++= Seq(
    specs2 % Test
  ),
  scalacOptions += "-feature",
  resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases",
  resolvers += "Artifactory" at "https://flow.artifactoryonline.com/flow/libs-release/",
  credentials += Credentials(
    "Artifactory Realm",
    "flow.artifactoryonline.com",
    System.getenv("ARTIFACTORY_USERNAME"),
    System.getenv("ARTIFACTORY_PASSWORD")
  )
)
version := "0.1.8"
