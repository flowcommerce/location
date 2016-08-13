package controllers

import io.flow.common.v0.models.json._
import io.flow.location.v0.models
import io.flow.location.v0.models.json._
import io.flow.location.v0.models.json._
import io.flow.play.util.Validation
import io.flow.reference.data
import io.flow.reference.v0.models.Country
import io.flow.reference.Countries
import play.api.libs.json._
import play.api.mvc._
import scala.concurrent.Future

@javax.inject.Singleton
class CountryDefaults @javax.inject.Inject() (
  helpers: Helpers
) extends Controller {

  import scala.concurrent.ExecutionContext.Implicits.global

  private[this] val DefaultCurrency = "usd" // Remove once every country has one defined
  private[this] val DefaultLanguage = "en"  // Remove once every country has at least one language in reference data

  def get(
    country: Option[String],
    address: Option[String],
    ip: Option[String]
  ) = Action.async { request =>
    Future {
      val countries = helpers.getLocations(country, address, ip) match {
        case Left(_) => {
          data.Countries.all
        }

        case Right(locations) => {
          locations.flatMap(_.country).flatMap(Countries.find(_))
        }
      }

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
  ) = Action.async { request =>
    Future {
      Countries.find(country) match {
        case None => NotFound
        case Some(c) => {
          Ok(Json.toJson(countryDefaults(c)))
        }
      }
    }
  }

  private[this] def countryDefaults(c: Country) = models.CountryDefaults(
    country = c.iso31663,
    currency = c.defaultCurrency.headOption.getOrElse(DefaultCurrency),
    language = c.languages.headOption.getOrElse(DefaultLanguage)
  )
  
}
