package fr.neuville.lombok.processor

import scalaz._
import Scalaz._
import std.anyVal.booleanInstance.{conjunction, disjunction}

import java.util.{List => JList}
import java.lang.{Class => JClass}
import java.lang.annotation.{Annotation => JAnnotation}

import com.intellij.psi._
import de.plushnikov.intellij.lombok.problem.ProblemBuilder
import de.plushnikov.intellij.lombok.quickfix.PsiQuickFixFactory
import de.plushnikov.intellij.lombok.psi.{LombokLightMethodBuilder, LombokPsiElementFactory}
import de.plushnikov.intellij.lombok.util._
import LombokProcessorUtil._
import PsiElementUtil._
import PsiClassUtil._
import PsiMethodUtil._

import lombok.{NonNull, RequiredArgsConstructor, AllArgsConstructor}
import de.plushnikov.intellij.lombok.processor.field.AnnotationFieldProcessor
import lombok.experimental.Wither
import de.plushnikov.intellij.lombok.processor.clazz.AnnotationClassProcessor

import fr.neuville.lombok.{Problem, Error, Warning}
import Problem._
import de.plushnikov.intellij.lombok.LombokUtils
import scalaz.syntax.ApplicativeBuilder

final class WitherProc(val annotationClass: JClass[_ <: JAnnotation],
                       val genClass: JClass[_]) extends AnnotationClassProcessor(annotationClass, genClass) {

  private val fieldProc = new WitherFieldProc(annotationClass, genClass)

  def this() = this(classOf[Wither], classOf[PsiMethod])

  override def validate(psiAnnotation: PsiAnnotation, psiClass: PsiClass, builder: ProblemBuilder): Boolean =
    (validOnRightType(psiAnnotation, psiClass) |@| validHasVisibility(psiAnnotation))((_, _) => true)
      .fold(((_: NonEmptyList[Problem]) foreach addToPbBuilder(builder)) andThen (_ => false), _ => true)

  override def processIntern(psiClass: PsiClass, psiAnnotation: PsiAnnotation, target: JList[_ >: PsiElement]): Unit =
    for {
      tg <- Option(target).toSeq
      method <- witherMethods(psiClass, psiAnnotation)
    } tg add method

  def witherMethods(clazz: PsiClass, anot: PsiAnnotation): Seq[LombokLightMethodBuilder] = {
    implicit val M = conjunction
    val canCreateWither: PsiField => Boolean = fieldProc.canCreateWither(anot)(_)(_ => Unit)
    (for {
      acLevel <- Option(getMethodModifier(anot)).toSeq
      field <- clazz.getFields.toSeq
      if (canCreateWither |+| hasNoWither |+| hasNoFront$)(field)
    } yield fieldProc.witherMethod(field, acLevel)).flatten
  }

  private def validOnRightType(anot: PsiAnnotation, clazz: PsiClass): ValidationNel[Problem, Boolean] =
    (!clazz.isAnnotationType && !clazz.isEnum && !clazz.isInterface) ?
      true.successNel[Problem] |
      Error(s"${anot.getQualifiedName} is only supported on a class or field type").failNel

  private def validHasVisibility(anot: PsiAnnotation): ValidationNel[Problem, Boolean] =
    LombokProcessorUtil.getMethodModifier(anot).some.isDefined ?
      true.successNel[Problem] |
      Error(s"${anot.getQualifiedName} value attribute must be defined").failNel

  private def hasNoWither(field: PsiField) =
    !field.getModifierList.getAnnotations.toVector.map(acceptElement(_, classOf[PsiMethod])).suml(disjunction)

  private def hasNoFront$(field: PsiField) = !(field.getName startsWith LombokUtils.LOMBOK_INTERN_FIELD_MARKER)
}

/**
 * Inspect and validate @Setter lombok annotation on a field
 * Creates setter method for this field
 */
final class WitherFieldProc(val annotationClass: JClass[_ <: JAnnotation],
                            val genClass: JClass[_]) extends AnnotationFieldProcessor(annotationClass, genClass) {

  def this() = this(classOf[Wither], classOf[PsiMethod])

  override def validate(anot: PsiAnnotation, field: PsiField, pbb: ProblemBuilder): Boolean =
    canCreateWither(anot)(field)(_ foreach addToPbBuilder(pbb))

  override def processIntern(psiField: PsiField, psiAnnotation: PsiAnnotation, target: JList[_ >: PsiElement]): Unit =
    for {
      acLevel <- Option(getMethodModifier(psiAnnotation))
      method <- witherMethod(psiField, acLevel)
      tg <- Option(target)
    } tg add method

  def canCreateWither(anot: PsiAnnotation)(field: PsiField)(treatErrors: NonEmptyList[_ <: Problem] => Unit): Boolean =
    (validNonStatic(field, anot) |@|
      validHasConstructor(field) |@|
      validIsWitherUnique(field, anot))((_, _, _) => true)
      .fold(treatErrors andThen (_ => false), _ => true)

  def witherMethod(psiField: PsiField, accessLevel: String): Option[LombokLightMethodBuilder] =
    for {
      field <- Option(psiField)
      mgr <- Option(field.getManager)
      name <- Option(field.getName)
      ftype <- Option(field.getType)
      clazz <- Option(field.getContainingClass)
      returnType <- Option(getTypeWithGenerics(clazz))
    } yield
      LombokPsiElementFactory.getInstance()
        .createLightMethod(mgr, witherName(name))
        .withModifier(accessLevel)
        .withMethodReturnType(returnType)
        .withContainingClass(clazz)
        .withParameter(name, ftype)
        .withNavigationElement(field)

  def witherName(fieldName: String): String = {
    val suffix = (fieldName.startsWith("is") && fieldName(2).isUpper) ? fieldName.substring(2) | fieldName
    s"with${suffix(0).toUpper}${suffix.substring(1)}"
  }

  private def validNonStatic(field: PsiField, annotation: PsiAnnotation): ValidationNel[Problem, Boolean] =
    if (field.hasModifierProperty(PsiModifier.STATIC))
      Error(s"${annotation.getQualifiedName} on static field is not allowed",
        PsiQuickFixFactory.createModifierListFix(field, PsiModifier.STATIC, false, false)).failNel
    else
      true.success

  private def validHasConstructor(field: PsiField): ValidationNel[Problem, Boolean] = {
    val hasParam: PsiField => PsiMethod => Boolean = f => m =>
      m.getParameterList.getParameters exists (p => typesAreEquivalent(p.getType, f.getType))

    val hasRightConstructor = Option(field.getContainingClass)
      .map(psiClass => collectClassConstructorIntern(psiClass))
      .exists(_.any(hasParam(field)))

    val isAnnotatedWith: JClass[_ <: JAnnotation] => PsiClass => Boolean =
      clazz => psiClass => PsiAnnotationUtil.isAnnotatedWith(psiClass, clazz)

    val hasAllArgsConstAnot =
      Option(field.getContainingClass).exists(isAnnotatedWith(classOf[AllArgsConstructor]))

    val hasRequiredArgsConstAnot =
      Option(field.getContainingClass).exists(isAnnotatedWith(classOf[RequiredArgsConstructor]))

    val isFinal = field.hasModifierProperty(PsiModifier.FINAL)

    val hasNonNullAnot = PsiAnnotationUtil.isAnnotatedWith(field, classOf[NonNull])

    if (hasRightConstructor ||
      hasAllArgsConstAnot ||
      (hasRequiredArgsConstAnot && (isFinal || hasNonNullAnot)))
      true.success
    else
      Warning(s"""Compilation will fail : no constructor
      with a parameter of type ${field.getType.getCanonicalText} was found""").failNel
  }

  private def validIsWitherUnique(field: PsiField, annotation: PsiAnnotation): ValidationNel[Problem, Boolean] = {
    val result =
      for {
        fieldName <- Option(field.getName)
        psiClass <- Option(field.getContainingClass)
        if hasSimilarMethod(collectClassMethodsIntern(psiClass), witherName(fieldName), 1)
      } yield
        Warning(s"""No ${annotation.getQualifiedName} generated : a method
        named ${witherName(fieldName)} taking one parameter already exists""").failNel
    result getOrElse true.success
  }
}