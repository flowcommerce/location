package utils

import com.amazonaws.services.s3.{AmazonS3, AmazonS3ClientBuilder}
import com.google.inject.name.Named
import com.google.inject.{AbstractModule, Provides, Singleton}
import io.flow.log.RollbarLogger
import play.api.{Environment, Mode}

import java.io.BufferedInputStream
import java.nio.file.{Files, Paths}

class Ip2LocationIndexModule extends AbstractModule {

  private[this] val s3Uri = "^s3://([^/]+)/(.*)".r

  override def configure(): Unit = ()

  @Provides
  @Named("Ip2LocationIndex")
  @Singleton
  def index(
    @javax.inject.Inject() environmentVariables: EnvironmentVariables,
    @javax.inject.Inject() environment: Environment,
    @javax.inject.Inject() logger: RollbarLogger,
  ): IndexedSeq[Ip2Location] = {
    val baseLogger = logger.fingerprint(getClass.getName).withSendToRollbar(false)

    val (ipv4, ipv6) = environment.mode match {
      case Mode.Dev | Mode.Test =>
        val (ipv4Path, ipv6Path) = ("IP-COUNTRY(in).csv", "IPV6-COUNTRY(in).csv")
        val isV4 = new BufferedInputStream(Files.newInputStream(Paths.get(ipv4Path)))
        val isV6 = new BufferedInputStream(Files.newInputStream(Paths.get(ipv6Path)))
        (isV4, isV6)

      case Mode.Prod =>
        val s3: AmazonS3 = AmazonS3ClientBuilder.standard().build()
        val isV4 = environmentVariables.ip2LocationV4FileUri match {
          case s3Uri(bucket, key) =>
            baseLogger.info("Getting ipv4 file from S3")
            s3.getObject(bucket, key).getObjectContent()
          case _ =>
            throw new IllegalArgumentException("Invalid ip2LocationFileUri. Must use either s3:// or file:// protocol")
        }
        val isV6 = environmentVariables.ip2LocationV6FileUri match {
          case s3Uri(bucket, key) =>
            baseLogger.info("Getting ipv6 file from S3")
            s3.getObject(bucket, key).getObjectContent()
          case _ =>
            throw new IllegalArgumentException("Invalid ip2LocationFileUri. Must use either s3:// or file:// protocol")
        }
        (isV4, isV6)
    }

    Ip2Location
      .buildIndex(ipv4, fieldDelimiter = ',', recordDelimiter = '\n')
      .appendedAll(Ip2Location.buildIndex(ipv6, fieldDelimiter = ',', recordDelimiter = '\n'))
  }

}
