package fr.neuville.lombok.processor

import com.intellij.psi.{PsiClass, PsiAnnotation, PsiElement}
import de.plushnikov.intellij.lombok.problem.LombokProblem
import com.intellij.openapi.project.Project

trait Processor[T <: PsiElement] {
  def acceptElement(psiElement: T, `type`: Class[_ <: PsiElement]): Boolean

  def getSupportedElement: String

  def getSupportedElementClass: Class[_]

  def verifyElement(psiAnnotation: PsiAnnotation): java.util.Collection[LombokProblem]

  def isEnabled(project: Project): Boolean

  def canProduce(`type`: Class[_ <: PsiElement]): Boolean

  def process(psiClass: PsiClass): java.util.List[_ >: PsiElement]
}
