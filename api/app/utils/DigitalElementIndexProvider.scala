package utils

import java.io.{BufferedInputStream, InputStream}
import java.nio.file.{Files, Paths}

import com.amazonaws.services.s3.{AmazonS3, AmazonS3ClientBuilder}
import play.api.Logger

@javax.inject.Singleton
class DigitalElementIndexProvider @javax.inject.Inject() (environmentVariables: EnvironmentVariables) {

  val fileUri = "^file:(.*)".r
  val s3Uri = "^s3://([^/]+)/(.*)".r

  def getIndex(): IndexedSeq[DigitalElementIndexRecord] = {
    val is: InputStream = environmentVariables.digitalElementFileUri match {
      case fileUri(path) => new BufferedInputStream(Files.newInputStream(Paths.get(path)))
      case s3Uri(bucket, key) => {
        val s3: AmazonS3 = AmazonS3ClientBuilder.standard().build()
        Logger.info(s"getting ${key} from bucket ${bucket}")
        s3.getObject(bucket, key).getObjectContent()
      }
      case _ => throw new IllegalArgumentException("Invalid digitalElementFileUri.  Must use either s3:// or file:// protocol")
    }
    Logger.info(s"Building index from ${environmentVariables.digitalElementFileUri}")
    val start = System.currentTimeMillis()
    val index = DigitalElement.buildIndex(is, ';', '\n')
    Logger.info(s"Indexed ${index.size} records in ${(System.currentTimeMillis() - start) / 1000} seconds")
    index
  }

}
