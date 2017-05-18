package utils

import org.scalatest.{Matchers, WordSpec}

class SearchSpec extends WordSpec with Matchers {

  "floor" should {

    "find an exact match" in {
      Search.maxBoundary(Array(1, 3, 5, 7), 5) should be (Some(5))
    }

    "find the floor" in {
      Search.maxBoundary(Array(1, 3, 5, 7), 6) should be (Some(5))
    }

    "return None when target is lower than all elements" in {
      Search.maxBoundary(Array(2, 3, 5, 7), 1) should be (None)
    }

    "return last element when target is larger than all elements" in {
      Search.maxBoundary(Array(2, 3, 5, 7), 8) should be (Some(7))
    }

    "return first element" in {
      Search.maxBoundary(Array(1, 3, 5, 7), 2) should be (Some(1))
    }

  }
}
