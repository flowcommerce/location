package utils

/**
  * Created by eric on 5/17/17.
  */
object Search {

  /**
    * Simple binary search that finds the max element in an array that is <= boundary
    * @param elems the sorted array to search
    * @param boundary the upper bound (inclusive)
    * @param candidate the last element we have found that satisfies the boundary condition
    * @tparam A
    * @return The element if found, None otherwise
    */
  def maxBoundary[A <% Ordered[A]](elems: Array[A], boundary: A, candidate: Option[A] = None): Option[A] = elems match {
    case Array() => candidate
    case Array(x) if (x <= boundary) => Some(x)
    case _ => {
      val mid = elems.length / 2
      if (elems(mid) <= boundary) {
        maxBoundary(elems.slice(mid+1, elems.length), boundary, Some(elems(mid)))
      } else {
        maxBoundary(elems.slice(0, mid - 1), boundary, candidate)
      }
    }
  }

}
