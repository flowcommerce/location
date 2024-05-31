package utils

import io.flow.play.util.Config

@javax.inject.Singleton
class EnvironmentVariables @javax.inject.Inject() (
  config: Config,
) {

  val googleApiKey: String = config.requiredString("google.api.key")

  val ip2LocationV4FileUri: String = config.requiredString("ip2location.v4.file.uri")

  val ip2LocationV6FileUri: String = config.requiredString("ip2location.v6.file.uri")

}
