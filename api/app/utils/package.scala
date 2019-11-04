import com.google.common.collect.RangeMap

package object utils {

  type DigitalElementIndex = RangeMap[BigInt, DigitalElementIndexRecord]

  implicit class DigitalElementIndexWithLookup(val index: DigitalElementIndex) extends AnyVal {
    def lookup(ip: BigInt): Option[DigitalElementIndexRecord] = Option(index.get(ip))
    def lookup(ip: DigitalElement.ValidatedIpAddress): Option[DigitalElementIndexRecord] = lookup(ip.intValue)
  }

}
