package fr.neuville.lombok.util

import com.intellij.psi.{PsiAnnotation, PsiModifierListOwner}
import fj.data.{Option => FJOpt}

object AnnotUtil {

  def findLbkAnnotation(owner: PsiModifierListOwner, annotationName: String): FJOpt[PsiAnnotation] =
    findLbkAnnot(owner, annotationName).fold(FJOpt.none[PsiAnnotation])(FJOpt.some)

  def findLbkAnnot(owner: PsiModifierListOwner, annotationName: String): Option[PsiAnnotation] =
    (for {
      list <- Option(owner.getModifierList).toSeq
      annots <- Option(list.getAnnotations).toSeq
      a <- annots
      if annotNameMatches(a, annotationName)
    } yield a).headOption

  def annotNameMatches(psiAnnot: PsiAnnotation, qualName: String): Boolean = {
    val aName = psiAnnot.getText.substring(1).split("\\(")(0) // example : from '@Foo(Whatever)' returns 'Foo'
    qualName == s"lombok.$aName" ||
      qualName == s"lombok.experimental.$aName" ||
      qualName == s"lombok.extern.slf4j.$aName" ||
      qualName == s"lombok.extern.log4j.$aName" ||
      qualName == s"lombok.extern.java.$aName" ||
      qualName == s"lombok.extern.apachecommons.$aName"
  }

}
