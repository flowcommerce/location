package utils

import io.flow.play.util.Config

@javax.inject.Singleton
class EnvironmentVariables @javax.inject.Inject() (
  config: Config,
) {

  val googleApiKey: Option[String] = config.optionalString("google.api.key")

  val digitalElementFileUri: Option[String] = config.optionalString("digitalelement.file.uri")

  val ip2LocationV4FileUri: Option[String] = config.optionalString("ip2location.v4.file.uri")

  val ip2LocationV6FileUri: Option[String] = config.optionalString("ip2location.v6.file.uri")

}
