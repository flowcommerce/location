package utils

import io.flow.play.util.Config
import play.api.{Environment, Mode}

@javax.inject.Singleton
class EnvironmentVariables @javax.inject.Inject() (
  config: Config,
  playEnv: Environment
) {

  val googleApiKey = playEnv.mode match {
    case Mode.Test => "testGoogleApiKey"
    case _ => config.requiredString("google.api.key")
  }

}
