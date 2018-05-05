package utils

import io.flow.play.util.Config
import play.api.{Environment, Mode}

@javax.inject.Singleton
class EnvironmentVariables @javax.inject.Inject() (
  config: Config
) {

  val digitalElementFileUri = config.requiredString("digitalelement.file.uri")

}
