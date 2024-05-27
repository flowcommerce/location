package utils

import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen._
import org.scalacheck.Prop.forAll
import org.scalacheck._
import org.scalacheck.util.Buildable

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

case class SearchWithBoundaryFixture(elems: IndexedSeq[Long], boundary: Long)

object SearchWithBoundaryProperties extends Properties("SearchWithBoundaries") {

  implicit def buildableIndexedSeq[T: Ordering]: Buildable[T, IndexedSeq[T]] = new Buildable[T, IndexedSeq[T]] {
    def builder = new mutable.Builder[T, IndexedSeq[T]] {
      val ab = new ArrayBuffer[T]()
      override def addOne(x: T) = {
        ab.append(x)
        this
      }
      def clear() = ab.clear()
      def result() = ab.sorted.toIndexedSeq
    }
  }

  val genNum = Gen.choose((Long.MinValue / 2), (Long.MaxValue / 2))
  val genelems = nonEmptyBuildableOf[IndexedSeq[Long], Long](genNum)

  val emptyCollection = for {
    elems <- buildableOfN[IndexedSeq[Long], Long](0, arbitrary[Long])
    boundary <- arbitrary[Long]
  } yield SearchWithBoundaryFixture(elems, boundary)

  val validBoundary = for {
    elems <- genelems
    boundary <- Gen.choose(elems(0), (Long.MaxValue / 2))
  } yield SearchWithBoundaryFixture(elems, boundary)

  val invalidBoundary = for {
    elems <- genelems
    boundary <- Gen.choose((Long.MinValue / 2), elems(0))
  } yield SearchWithBoundaryFixture(elems, boundary)

  property("empty collection") = forAll(emptyCollection) { (fixture: SearchWithBoundaryFixture) =>
    Collections.searchWithBoundary(fixture.elems, fixture.boundary)(_ <= _) == None
  }

  property("boundary is less than the smallest element") = forAll(invalidBoundary) {
    (fixture: SearchWithBoundaryFixture) =>
      Collections.searchWithBoundary(fixture.elems, fixture.boundary)(_ <= _) == None
  }

  property("returned max") = forAll(validBoundary) { (fixture: SearchWithBoundaryFixture) =>
    {
      Collections.searchWithBoundary(fixture.elems, fixture.boundary)(_ <= _) match {
        case None => System.out.println("got none"); false // shouldn't happen
        case Some(max) => {
          val diff: Long = fixture.boundary - max
          // verify that there are no elements in this sequence that are
          // closer to, but not greater than, boundary
          // if that is true, the property is validated
          fixture.elems.forall(e => {
            val thisdiff: Long = (fixture.boundary - e)
            (e > fixture.boundary || thisdiff >= diff)
          })
        }
      }
    }
  }

}
