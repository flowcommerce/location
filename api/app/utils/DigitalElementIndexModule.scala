package utils

import java.io.{BufferedInputStream, InputStream}
import java.nio.file.{Files, Paths}

import com.amazonaws.services.s3.{AmazonS3, AmazonS3ClientBuilder}
import com.google.inject.name.Named
import com.google.inject.{AbstractModule, Provides, Singleton}
import io.flow.log.RollbarLogger

class DigitalElementIndexModule extends AbstractModule {

  private[this] val fileUri = "^file:(.*)".r
  private[this] val s3Uri = "^s3://([^/]+)/(.*)".r

  override def configure(): Unit = ()

  @Provides
  @Named("DigitalElementIndex")
  @Singleton
  def getIndex(
    @javax.inject.Inject() environmentVariables: EnvironmentVariables,
    @javax.inject.Inject() logger: RollbarLogger
  ): DigitalElementIndex = {
    val is: InputStream = environmentVariables.digitalElementFileUri match {
      case fileUri(path) => new BufferedInputStream(Files.newInputStream(Paths.get(path)))
      case s3Uri(bucket, key) => {
        val s3: AmazonS3 = AmazonS3ClientBuilder.standard().build()
        s3.getObject(bucket, key).getObjectContent()
      }
      case _ => throw new IllegalArgumentException("Invalid digitalElementFileUri.  Must use either s3:// or file:// protocol")
    }
    logger.
      fingerprint(getClass.getName).
      withKeyValue("uri", environmentVariables.digitalElementFileUri).
      info("Building index")
    val start = System.currentTimeMillis()
    val index = DigitalElement.buildIndex(is, ';', '\n')
    is.close()

    val elapsedTime = (System.currentTimeMillis() - start) / 1000
    logger.
      fingerprint(getClass.getName).
      withKeyValue("uri", environmentVariables.digitalElementFileUri).
      withKeyValue("size",index.size).
      withKeyValue("elapsed_seconds", elapsedTime).
      info("Indexed Records")
    index
  }

}
