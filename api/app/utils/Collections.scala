package utils

object Collections {

  /**
    * Simple binary search that finds the closest element (relative to the head of a Sequence) that satisfies a boundary condition
    * @param elems the sorted sequence to search
    * @param boundary boundary value
    * @param candidate the last element we have found that satisfies the boundary condition
    * @param compare a function that will compare each element in elems to the boundary
    * @return The element if found, None otherwise
    */
  def searchWithBoundary[A, B](elems: IndexedSeq[A], boundary: B, candidate: Option[A] = None)(implicit compare: (A, B) => Boolean): Option[A] =
    elems match {
      case IndexedSeq() => candidate
      case IndexedSeq(x) if compare(x, boundary) => Some(x)
      case _ => {
        val mid = elems.length / 2
        if (compare(elems(mid), boundary)) {
          searchWithBoundary(elems.slice(mid+1, elems.length), boundary, Some(elems(mid)))
        } else {
          searchWithBoundary(elems.slice(0, mid), boundary, candidate)
        }
      }
    }

}
