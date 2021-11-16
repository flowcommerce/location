package controllers

import akka.actor.ActorSystem
import io.flow.location.v0.models
import io.flow.location.v0.models.json._
import io.flow.reference.{Countries, data}
import io.flow.reference.v0.models.Country
import play.api.libs.json._
import play.api.mvc._

import scala.concurrent.Future

@javax.inject.Singleton
class CountryDefaults @javax.inject.Inject() (
  override val controllerComponents: ControllerComponents,
  helpers: Helpers,
  system: ActorSystem,
) extends BaseController {
  private[this] implicit val ec = system.dispatchers.lookup("controller-context")
  private[this] val DefaultCurrency = "usd" // Remove once every country has one defined
  private[this] val DefaultLanguage = "en"  // Remove once every country has at least one language in reference data

  def get(
    country: Option[String],
    ip: Option[String]
  ) = Action.async { _ =>
    helpers.getLocations(country = country, ip = ip).map {
      case Left(_) => data.Countries.all
      case Right(locations) => locations.flatMap(_.country).flatMap(Countries.find)
    }.map { countries =>
      Ok(
        Json.toJson(
          countries.map { c =>
            countryDefaults(c)
          }
        )
      )
    }
  }

  def getByCountry(
    country: String
  ) = Action.async { _ =>
    Future.successful (
      Countries.find(country) match {
        case None => NotFound
        case Some(c) => {
          Ok(Json.toJson(countryDefaults(c)))
        }
      }
    )
  }

  private[this] def countryDefaults(c: Country) = models.CountryDefaults(
    country = c.iso31663,
    currency = c.defaultCurrency.getOrElse(DefaultCurrency),
    language = c.languages.headOption.getOrElse(DefaultLanguage)
  )

}
