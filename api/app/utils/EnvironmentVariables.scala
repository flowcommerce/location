package utils

import io.flow.play.util.Config
import play.api.{Environment, Mode}

@javax.inject.Singleton
class EnvironmentVariables @javax.inject.Inject() (
  config: Config,
  playEnv: Environment
) {

  val googleApiKey: String = playEnv.mode match {
    case Mode.Test => config.requiredString("google.api.key")
    case _ => config.requiredString("google.api.key")
  }

  val digitalElementFileUri: String = config.requiredString("digitalelement.file.uri")

}
