import play.sbt.PlayScala._

name := "location"

scalaVersion in ThisBuild := "2.12.8"

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
      "io.flow" %% "lib-play-play26" % "0.5.28",
      "io.flow" %% "lib-play-graphite-play26" % "0.0.71",
      "io.flow" %% "lib-reference-scala" % "0.2.19",
      "io.flow" %% "lib-s3-play26" % "0.2.41",
      "com.google.maps" % "google-maps-services" % "0.9.1",
      "org.scalacheck" %% "scalacheck" % "1.14.0" % "test",
      "io.flow" %% "lib-test-utils" % "0.0.29" % Test,
      "io.flow" %% "lib-usage" % "0.0.64",
      "io.flow" %% "lib-log" % "0.0.54"
    )
  )

lazy val commonSettings: Seq[Setting[_]] = Seq(
  name ~= ("location-" + _),
  libraryDependencies ++= Seq(
    guice,
    ws,
    "com.typesafe.play" %% "play-json-joda" % "2.6.10",
    "com.typesafe.play" %% "play-json" % "2.6.13"
  ),
  scalacOptions += "-feature",
  resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases",
  resolvers += "Artifactory" at "https://flow.jfrog.io/flow/libs-release/",
  credentials += Credentials(
    "Artifactory Realm",
    "flow.jfrog.io",
    System.getenv("ARTIFACTORY_USERNAME"),
    System.getenv("ARTIFACTORY_PASSWORD")
  )
)
version := "0.4.65"
