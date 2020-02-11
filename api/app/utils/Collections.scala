package utils

import scala.annotation.tailrec
import scala.collection.IndexedSeqView

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
    searchWithBoundaryRec(elems.view, boundary, candidate)(compare)

  @tailrec
  private def searchWithBoundaryRec[A, B](elems: IndexedSeqView[A], boundary: B, candidate: Option[A])(implicit compare: (A, B) => Boolean): Option[A] =
    if (elems.isEmpty)
      candidate
    else if (elems.sizeIs == 1 && elems.headOption.exists(compare(_, boundary)))
      elems.headOption
    else {
      val mid = elems.length / 2
      if (compare(elems(mid), boundary))
        searchWithBoundaryRec(elems.slice(mid + 1, elems.length), boundary, Some(elems(mid)))
      else
        searchWithBoundaryRec(elems.slice(0, mid), boundary, candidate)
    }

}
