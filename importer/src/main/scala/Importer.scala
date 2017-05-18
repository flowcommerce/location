import anorm._
import java.sql.{Connection, DriverManager}
import scala.io.Source

object Importer extends App {

  Class.forName("org.h2.Driver")

  implicit val connection: Connection = DriverManager.getConnection("jdbc:h2:/Users/eric/src/location/db/digitalelement;AUTO_SERVER=TRUE")
  try {
    SQL(
      """
        create table if not exists ip_locations(
          start_num bigint,
          end_num bigint,
          country varchar(10),
          region varchar(10),
          city varchar(120),
          latitude double,
          longitude double,
          postal_code varchar(10),
          country_code int,
          region_code int,
          city_code int,
          continent_code int,
          two_letter_country char(2),
          gmt_offset varchar(10),
          in_dst boolean
        )
        AS SELECT * FROM CSVREAD('/Users/eric/netacuity/text_file_2/output.csv', null, 'fieldSeparator=;');
        create index idx_start_num on ip_locations(start_num desc);
        create index idx_end_num on ip_locations(end_num);
      """.stripMargin).execute()

//    Source.fromFile("/Users/eric/netacuity/text_file_2/output.csv")
//      .getLines() foreach ((line: String) => {
//      val fields: Array[String] = line.split(';')
//      SQL(
//        """
//            insert into ip_locations(
//              start_num,
//              end_num,
//              country,
//              region,
//              city,
//              latitude,
//              longitude,
//              postal_code,
//              country_code,
//              region_code,
//              city_code,
//              continent_code,
//              two_letter_country,
//              gmt_offset,
//              in_dst)
//            values(
//              {start_num},
//              {end_num},
//              {country},
//              {region},
//              {city},
//              {latitude},
//              {longitude},
//              {postal_code},
//              {country_code},
//              {region_code},
//              {city_code},
//              {continent_code},
//              {two_letter_country},
//              {gmt_offset},
//              {in_dst})""".stripMargin)
//        .on('start_num -> fields(0),
//          'end_num -> fields(1),
//          'country -> fields(2),
//          'region -> fields(3),
//          'city -> fields(4),
//          'latitude -> fields(5),
//          'longitude -> fields(6),
//          'postal_code -> fields(7),
//          'country_code -> fields(8),
//          'region_code -> fields(9),
//          'city_code -> fields(10),
//          'continent_code -> fields(11),
//          'two_letter_country -> fields(12),
//          'gmt_offset -> fields(13),
//          'in_dst -> (if (fields(14) == "y") "true" else "false"))
//        .executeInsert()
//    })
  } catch {
    case e: Exception => {
      System.err.println("Error encountered during import:")
      e.printStackTrace()
    }
  } finally {
    connection.close()
  }
}
