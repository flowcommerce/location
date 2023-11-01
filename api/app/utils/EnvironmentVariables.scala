package utils

import io.flow.play.util.Config

@javax.inject.Singleton
class EnvironmentVariables @javax.inject.Inject() (
  config: Config
) {

  val googleApiKey: String = config.requiredString("google.api.key")

  val digitalElementFileUri: String = config.requiredString("digitalelement.file.uri")

}
