package de.plushnikov.intellij.lombok.processor.field;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.psi.*;
import de.plushnikov.intellij.lombok.StringUtils;
import de.plushnikov.intellij.lombok.problem.ProblemBuilder;
import de.plushnikov.intellij.lombok.psi.LombokLightMethodBuilder;
import de.plushnikov.intellij.lombok.psi.LombokPsiElementFactory;
import de.plushnikov.intellij.lombok.quickfix.PsiQuickFixFactory;
import de.plushnikov.intellij.lombok.util.LombokProcessorUtil;
import de.plushnikov.intellij.lombok.util.PsiClassUtil;
import de.plushnikov.intellij.lombok.util.PsiMethodUtil;
import fj.*;
import fj.data.NonEmptyList;
import fj.data.Option;
import fj.data.Stream;
import fj.data.Validation;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Wither;
import org.jetbrains.annotations.NotNull;

import java.lang.Class;
import java.lang.annotation.Annotation;
import java.util.List;

import static de.plushnikov.intellij.lombok.util.LombokProcessorUtil.getMethodModifier;
import static de.plushnikov.intellij.lombok.util.PsiAnnotationUtil.isAnnotatedWith;
import static de.plushnikov.intellij.lombok.util.PsiElementUtil.typesAreEquivalent;
import static fj.Function.constant;
import static fj.P.p;
import static fj.data.Array.array;
import static fj.data.Option.*;
import static fj.data.Stream.iterableStream;
import static fj.data.Validation.fail;
import static fj.data.Validation.success;
import static java.lang.String.format;

public class WitherFieldProcessor extends AbstractLombokFieldProcessor {

  private final Semigroup<NonEmptyList<P2<String, Option<LocalQuickFix>>>> sm = Semigroup.nonEmptyListSemigroup();

  public WitherFieldProcessor() {
    super(Wither.class, PsiMethod.class);
  }

  protected WitherFieldProcessor(@NotNull Class<? extends Annotation> supportedAnnotationClass, @NotNull Class<?> supportedClass) {
    super(supportedAnnotationClass, supportedClass);
  }

  @Override
  protected boolean validate(@NotNull PsiAnnotation psiAnnotation, @NotNull PsiField psiField, @NotNull final ProblemBuilder builder) {
    final F<Boolean, F<Boolean, F<Boolean, Boolean>>> constantBool = constant(Function.<Boolean, Boolean>constant());
    return
        validNonStatic(psiField, psiAnnotation).nel()
            .accumapply(sm, validHasConstructor(psiField).nel()
                .accumapply(sm, validIsWitherUnique(psiField, psiAnnotation).nel().map(constantBool)))
            .validation(
                new F<NonEmptyList<P2<String, Option<LocalQuickFix>>>, Boolean>() {
                  public Boolean f(NonEmptyList<P2<String, Option<LocalQuickFix>>> problems) {
                    for (P2<String, Option<LocalQuickFix>> problem : problems)
                      if (problem._2().isSome())
                        builder.addError(problem._1(), problem._2().some());
                      else builder.addWarning(problem._1());
                    return false;
                  }
                },
                constantBool.f(true).f(true)
            );
  }

  @Override
  protected void processIntern(PsiField psiField, PsiAnnotation psiAnnotation, List<? super PsiElement> target) {
    for (PsiField field : fromNull(psiField))
      for (PsiManager manager : fromNull(field.getManager()))
        for(String fieldName : fromString(field.getName()))
          for (PsiType fieldType : fromNull(field.getType()))
            for (PsiClass fieldClass : fromNull(field.getContainingClass()))
              for (PsiType returnType : fromNull(PsiClassUtil.getTypeWithGenerics(fieldClass)))
                for(String methodVisibility : fromNull(getMethodModifier(psiAnnotation))) {
                final LombokLightMethodBuilder method =
                    LombokPsiElementFactory.getInstance().createLightMethod(manager, witherName(fieldName))
                    .withMethodReturnType(returnType)
                    .withContainingClass(fieldClass)
                    .withParameter(fieldName, fieldType)
                    .withNavigationElement(field);
                  target.add(method.withModifier(methodVisibility));
                }
  }

  private String witherName(String fieldName) {
    final String suffix = fieldName.startsWith("is") && Character.isUpperCase(fieldName.charAt(2)) ?
        fieldName.substring(2) :
        fieldName;
    return "with" + StringUtils.capitalize(suffix);
  }

  private String secondWitherName(String fieldName) {
    return "with" + StringUtils.capitalize(fieldName);
  }

  private Validation<P2<String, Option<LocalQuickFix>>, Boolean> validNonStatic(PsiField field, PsiAnnotation annotation) {
    if (field.hasModifierProperty(PsiModifier.STATIC))
      return fail(p(
          format("'@%s' on static field is not allowed", annotation.getQualifiedName()),
          some(PsiQuickFixFactory.createModifierListFix(field, PsiModifier.STATIC, false, false))));
    else return success(true);
  }

  private Validation<P2<String, Option<LocalQuickFix>>, Boolean> validHasConstructor(final PsiField field) {
    final F2<PsiField, PsiMethod, Boolean> hasParam = new F2<PsiField, PsiMethod, Boolean>() {
      public Boolean f(final PsiField pfield, PsiMethod pmeth) {
        return array(pmeth.getParameterList().getParameters()).exists(new F<PsiParameter, Boolean>() {
          public Boolean f(PsiParameter param) {
            return typesAreEquivalent(param.getType(), field.getType());
          }
        });
      }
    };

    final boolean hasRightConstructor = fromNull(field.getContainingClass())
        .map(new F<PsiClass, Stream<PsiMethod>>() {
          public Stream<PsiMethod> f(PsiClass psiClass) {
            return iterableStream(PsiClassUtil.collectClassConstructorIntern(psiClass));
          }
        })
        .exists(new F<Stream<PsiMethod>, Boolean>() {
          public Boolean f(Stream<PsiMethod> psiMethods) {
            return psiMethods.exists(hasParam.f(field));
          }
        });

    final F2<Class<? extends Annotation>, PsiClass, Boolean> isAnnotatedWith =
        new F2<Class<? extends Annotation>, PsiClass, Boolean>() {
          public Boolean f(Class<? extends Annotation> clazz, PsiClass psiClass) {
            return isAnnotatedWith(psiClass, clazz);
          }
    };

    final boolean hasAllArgsConstAnot =
        fromNull(field.getContainingClass()).exists(isAnnotatedWith.f(AllArgsConstructor.class));

    final boolean hasRequiredArgsConstAnot =
        fromNull(field.getContainingClass()).exists(isAnnotatedWith.f(RequiredArgsConstructor.class));

    final boolean isFinal = field.hasModifierProperty(PsiModifier.FINAL);

    final boolean hasNonNullAnot = isAnnotatedWith(field, NonNull.class);

    if (hasRightConstructor ||
        hasAllArgsConstAnot ||
        (hasRequiredArgsConstAnot && (isFinal || hasNonNullAnot)))
      return success(true);
    else return fail(p(
        format("Compilation will fail : no constructor with a parameter of type '%s' was found",
            field.getType().getCanonicalText()),
        Option.<LocalQuickFix>none()));
  }

  private Validation<P2<String, Option<LocalQuickFix>>, Boolean> validIsWitherUnique(PsiField field, PsiAnnotation annotation) {
    for (String fieldName : fromString(field.getName()))
      for (PsiClass psiClass : fromNull(field.getContainingClass()))
        if (PsiMethodUtil.hasSimilarMethod(PsiClassUtil.collectClassMethodsIntern(psiClass), witherName(fieldName), 1)
          ||PsiMethodUtil.hasSimilarMethod(PsiClassUtil.collectClassMethodsIntern(psiClass), secondWitherName(fieldName), 1))
          return fail(p(
              format("No '@%s' generated : a method named '%s' taking one parameter already exists",
                  annotation.getQualifiedName(),
                  witherName(fieldName)),
              Option.<LocalQuickFix>none()));
    return success(true);
  }
}
