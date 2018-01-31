import play.sbt.PlayScala._

name := "location"

scalaVersion in ThisBuild := "2.11.12"

lazy val generated = project
  .in(file("generated"))
  .enablePlugins(PlayScala)
  .settings(commonSettings: _*)

lazy val api = project
  .in(file("api"))
  .dependsOn(generated)
  .aggregate(generated)
  .enablePlugins(PlayScala)
  .enablePlugins(NewRelic)
  .enablePlugins(JavaAppPackaging, JavaAgent)
  .settings(commonSettings: _*)
  .settings(
    javaAgents += "org.aspectj" % "aspectjweaver" % "1.8.13",
    javaOptions in Universal += "-Dorg.aspectj.tracing.factory=default",
    javaOptions in Test += "-Dkamon.modules.kamon-system-metrics.auto-start=false",
    javaOptions in Test += "-Dkamon.show-aspectj-missing-warning=no",
    javaOptions in Test += "-Dconfig.file=conf/application.test.conf",
    routesImport += "io.flow.location.v0.Bindables._",
    routesGenerator := InjectedRoutesGenerator,
    libraryDependencies ++= Seq(
      "io.flow" %% "lib-play-play26" % "0.4.35",
      "io.flow" %% "lib-play-graphite-play26" % "0.0.10",
      "io.flow" %% "lib-reference-scala" % "0.1.48",
      "com.amazonaws" % "aws-java-sdk-s3" % "1.11.271",
      "com.google.maps" % "google-maps-services" % "0.2.6",
      "com.sanoma.cda" %% "maxmind-geoip2-scala" % "1.5.1", // Not yet available for scala 2.12
      "org.scalacheck" %% "scalacheck" % "1.13.5" % "test",
      "io.flow" %% "lib-test-utils" % "0.0.4" % Test
    )
  )

lazy val commonSettings: Seq[Setting[_]] = Seq(
  name ~= ("location-" + _),
  libraryDependencies ++= Seq(
    guice,
    ws,
    "com.typesafe.play" %% "play-json-joda" % "2.6.8",
    "com.typesafe.play" %% "play-json" % "2.6.8"
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
