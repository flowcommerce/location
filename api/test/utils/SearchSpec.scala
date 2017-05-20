package utils

import org.scalatest.{Matchers, WordSpec}
import org.scalacheck.Prop.{BooleanOperators, forAll}
import org.scalacheck._
import Gen._
import Arbitrary.arbitrary
import org.scalacheck.util.Buildable
import org.scalatest.prop.Checkers._

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer


object SearchSpec {


}

case class MaxWithBoundaryFixture(elems: IndexedSeq[Long], boundary: Long)

object MaxWithBoundaryProperties extends Properties("MaxWithBoundaries") {

  implicit def buildableIndexedSeq[T <% Ordered[T]] = new Buildable[T,IndexedSeq[T]] {
    def builder = new mutable.Builder[T,IndexedSeq[T]] {
      val ab = new ArrayBuffer[T]()
      def +=(x: T) = {
        ab.append(x)
        this
      }
      def clear() = ab.clear()
      def result() = ab.sorted
    }
  }

  val genNum = Gen.choose((Long.MinValue/2), (Long.MaxValue/2))
  val genelems = nonEmptyBuildableOf[IndexedSeq[Long], Long](genNum)

  val emptyCollection = for {
    elems <- buildableOfN[IndexedSeq[Long], Long](0, arbitrary[Long])
    boundary <- arbitrary[Long]
  } yield MaxWithBoundaryFixture(elems, boundary)

  val validBoundary = for {
    elems <- genelems
    boundary <- Gen.choose(elems(0),  (Long.MaxValue/2))
  } yield MaxWithBoundaryFixture(elems, boundary)

  val invalidBoundary = for {
    elems <- genelems
    boundary <- Gen.choose((Long.MinValue/2), elems(0))
  } yield MaxWithBoundaryFixture(elems, boundary)

  property("empty collection") = forAll (emptyCollection) {
    (fixture: MaxWithBoundaryFixture) =>
      Search.maxWithBoundary(fixture.elems, fixture.boundary) == None
  }

  property("boundary is less than the smallest element") = forAll (invalidBoundary) {
    (fixture: MaxWithBoundaryFixture) =>
      Search.maxWithBoundary(fixture.elems, fixture.boundary) == None
  }

  property("returned max") = forAll (validBoundary) {
    (fixture: MaxWithBoundaryFixture) => {
        Search.maxWithBoundary(fixture.elems, fixture.boundary) match {
          case None => System.out.println("got none"); false // shouldn't happen
          case Some(max) => {
            val diff: Long = fixture.boundary - max
            // verify that there are no elements in this colletion that are
            // closer to, but not greater than, boundary
            fixture.elems.forall(e => {
              val thisdiff: Long = (fixture.boundary - e)
              (e > fixture.boundary ||  thisdiff >= diff)
            })
          }
        }
      }
  }

}

//class SearchSpec extends WordSpec with Matchers {
//
//  "maxWithBoundary" should {
//
//
//    "find an exact match" in {
//      Search.maxWithBoundary(Array(1, 3, 5, 7), 5) should be (Some(5))
//    }
//
//    "find the floor" in {
//      Search.maxWithBoundary(Array(1, 3, 5, 7), 6) should be (Some(5))
//    }
//
//    "return None when target is lower than all elements" in {
//      Search.maxWithBoundary(Array(2, 3, 5, 7), 1) should be (None)
//    }
//
//    "return last element when target is larger than all elements" in {
//      Search.maxWithBoundary(Array(2, 3, 5, 7), 8) should be (Some(7))
//    }
//
//    "return first element" in {
//      Search.maxWithBoundary(Array(1, 3, 5, 7), 2) should be (Some(1))
//    }
//
//    "work" in {
//      MaxWithBoundaryProperties.check
//    }
//
//  }
//}
