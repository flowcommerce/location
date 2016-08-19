package utils

import com.google.maps.{GeoApiContext, TimeZoneApi}
import com.google.maps.model.LatLng
import io.flow.common.v0.models.Address
import io.flow.reference.v0.models.Timezone

@javax.inject.Singleton
class GoogleTimeZone @javax.inject.Inject() (
  environmentVariables: EnvironmentVariables
) {

  val context = new GeoApiContext().setApiKey(environmentVariables.googleApiKey)

  def getTimezoneByAddress(address: Address): Either[String, Timezone] = {
    (address.latitude, address.longitude) match {
      case (Some(lat), Some(lng)) => {
        // returns java.util.TimeZone, which has getID()
        val tz = TimeZoneApi.getTimeZone(context, new LatLng(lat.toDouble, lng.toDouble)).await()

        // put together our timezone
        Right(
          Timezone(
            name = tz.getID(), // America/New_York
            description = tz.getDisplayName(), // Eastern Standard Time
            offset = tz.getRawOffset()
          )
        )
      }
      case _ => Left("Both latitude and longitude of an address should be provided to get a timezone")
    }
  }

}