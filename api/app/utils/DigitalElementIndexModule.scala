package utils

import java.io.{BufferedInputStream, ByteArrayInputStream, InputStream}
import java.nio.file.{Files, Paths}
import java.util.zip.GZIPInputStream

import com.amazonaws.services.s3.{AmazonS3, AmazonS3ClientBuilder}
import com.google.inject.name.Named
import com.google.inject.{AbstractModule, Provides, Singleton}
import play.api.Logger

class DigitalElementIndexModule extends AbstractModule {

  val fileUri = "^file:(.*)".r
  val s3Uri = "^s3://([^/]+)/(.*)".r

  override def configure(): Unit = ()

  @Provides
  @Named("DigitalElementIndex")
  @Singleton
  def getIndex(@javax.inject.Inject() environmentVariables: EnvironmentVariables): IndexedSeq[DigitalElementIndexRecord] = {
    val is: InputStream = environmentVariables.digitalElementFileUri match {
      case fileUri(path) => new BufferedInputStream(Files.newInputStream(Paths.get(path)))
      case s3Uri(bucket, key) => {
        val s3: AmazonS3 = AmazonS3ClientBuilder.standard().build()
        s3.getObject(bucket, key).getObjectContent()
      }
      case _ => throw new IllegalArgumentException("Invalid digitalElementFileUri.  Must use either s3:// or file:// protocol")
    }
    Logger.info(s"Building index from ${environmentVariables.digitalElementFileUri}")
    val start = System.currentTimeMillis()
    val index = DigitalElement.buildIndex(is, ';', '\n')
    is.close()
    Logger.info(s"Indexed ${index.size} records in ${(System.currentTimeMillis() - start) / 1000} seconds")
    index
  }

}