package utils

/**
  * Created by eric on 5/17/17.
  */
object Search {

  /**
    * Simple binary search that finds the max element in an array that is <= boundary
    * @param elems the sorted collection to search
    * @param boundary the upper bound (inclusive)
    * @param candidate the last element we have found that satisfies the boundary condition
    * @tparam A
    * @return The element if found, None otherwise
    */
  def maxWithBoundary[A <% Ordered[A]](elems: IndexedSeq[A], boundary: A, candidate: Option[A] = None): Option[A] = elems match {
    case IndexedSeq() => candidate
    case IndexedSeq(x) if (x <= boundary) => Some(x)
    case _ => {
      val mid = elems.length / 2
      if (elems(mid) <= boundary) {
        maxWithBoundary(elems.slice(mid+1, elems.length), boundary, Some(elems(mid)))
      } else {
        maxWithBoundary(elems.slice(0, mid), boundary, candidate)
      }
    }
  }

}
