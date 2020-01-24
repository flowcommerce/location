package controllers

import akka.actor.ActorSystem
import javax.inject.{Inject, Singleton}
import play.api.mvc.{AnyContent, ControllerComponents, Request}
import scala.concurrent.Future

import io.flow.location.v0.models
import io.flow.location.v0.controllers.CountryDefaultsController
import io.flow.reference.{data, Countries}
import io.flow.reference.v0.models.Country

@Singleton
class CountryDefaults @Inject() (
  helpers: Helpers,
  system: ActorSystem,
  val controllerComponents: ControllerComponents,
) extends CountryDefaultsController {

  private[this] implicit val ec = system.dispatchers.lookup("country-defaults-controller-context")

  private[this] val DefaultCurrency = "usd" // Remove once every country has one defined
  private[this] val DefaultLanguage = "en"  // Remove once every country has at least one language in reference data

  def get(req: Request[AnyContent], country: Option[String], ip: Option[String]) =
    helpers.getLocations(country= country, ip = ip)
      .map { eitherErrorOrLocation =>
        val countries = eitherErrorOrLocation.fold(
          _ => data.Countries.all,
          _.flatMap(_.country).flatMap(Countries.find))

        val countriesWithDefaults = countries.map(countryDefaults)
        Get.HTTP200(countriesWithDefaults)
      }

  def getByCountry(req: Request[AnyContent], country: String) = Future.successful {
    Countries.find(country).fold[GetByCountry](GetByCountry.HTTP404) { country =>
      val countryWithDefaults = countryDefaults(country)
      GetByCountry.HTTP200(countryWithDefaults)
    }
  }

  private[this] def countryDefaults(c: Country) = models.CountryDefaults(
    country = c.iso31663,
    currency = c.defaultCurrency.getOrElse(DefaultCurrency),
    language = c.languages.headOption.getOrElse(DefaultLanguage)
  )

}
