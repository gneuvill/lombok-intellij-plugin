package fr.neuville.lombok

import scalaz.Foldable
import java.util.{ Collection, AbstractCollection, Iterator }

package object processor {

  implicit val collFoldable = new Foldable[Collection] with Foldable.FromFoldr[Collection] {
    def foldRight[A, B](fa: Collection[A], z: => B)(f: (A, => B) => B): B =
      if (fa.isEmpty) z else f(fa.iterator().next(), foldRight(
        new AbstractCollection[A] {
          def size(): Int = fa.size() - 1
          def iterator(): Iterator[A] = fa.iterator()
        },
        z)(f))
  }
}
