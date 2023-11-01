package object utils {

  type DigitalElementIndex = IndexedSeq[DigitalElementIndexRecord]

  implicit class DigitalElementIndexWithLookup(index: DigitalElementIndex) {
    def lookup(ip: BigInt): Option[DigitalElementIndexRecord] =
      Collections
        .searchWithBoundary(index, ip)((a, b) => a.rangeStart <= b)
        .filter(ip <= _.rangeEnd)

    def lookup(ip: DigitalElement.ValidatedIpAddress): Option[DigitalElementIndexRecord] = lookup(ip.intValue)
  }

}
