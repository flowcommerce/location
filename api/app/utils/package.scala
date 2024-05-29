package object utils {

  type DigitalElementIndex = IndexedSeq[DigitalElementIndexRecord]

  implicit class DigitalElementIndexWithLookup(index: DigitalElementIndex) {
    def lookup(ip: BigInt): Option[DigitalElementIndexRecord] =
      Collections
        .searchWithBoundary(index, ip)((a, b) => a.rangeStart <= b)
        .filter(ip <= _.rangeEnd)

    def lookup(ip: DigitalElement.ValidatedIpAddress): Option[DigitalElementIndexRecord] = lookup(ip.intValue)
  }

  implicit class Ip2LocationWithLookup(index: IndexedSeq[Ip2Location]) {
    def lookup(ip: BigInt): Option[Ip2Location] =
      Collections
        .searchWithBoundary(index, ip)((a, b) => a.rangeStart <= b)
        .filter(ip <= _.rangeEnd)
  }

  implicit class OptionalArrayToAnything(optList: Option[Array[String]]) {
    def toArrayCustom: Array[String] = {
      optList match {
        case Some(value) => value
        case None => Array.empty[String]
      }
    }
  }
}
