package object utils {

  type DigitalElementIndex = IndexedSeq[DigitalElementIndexRecord]

  implicit class DigitalElementIndexWithLookup(index: DigitalElementIndex) {
    def lookup(ip: Long): Option[DigitalElementIndexRecord] =
      Collections.searchWithBoundary(index, ip)((a,b) => a.rangeStart <= b)
        .filter(ip <= _.rangeEnd)
  }

}
