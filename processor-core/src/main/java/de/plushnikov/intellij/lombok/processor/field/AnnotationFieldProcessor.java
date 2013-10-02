package de.plushnikov.intellij.lombok.processor.field;

import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import de.plushnikov.intellij.lombok.problem.LombokProblem;
import de.plushnikov.intellij.lombok.problem.ProblemBuilder;
import de.plushnikov.intellij.lombok.problem.ProblemEmptyBuilder;
import de.plushnikov.intellij.lombok.problem.ProblemNewBuilder;
import de.plushnikov.intellij.lombok.processor.AnnotationProcessor;
import de.plushnikov.intellij.lombok.util.PsiAnnotationUtil;
import de.plushnikov.intellij.lombok.util.PsiClassUtil;
import fr.neuville.lombok.util.AnnotUtil;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Base lombok processor class for field annotations
 *
 * @author Plushnikov Michail
 */
public abstract class AnnotationFieldProcessor extends AnnotationProcessor implements FieldProcessor<PsiAnnotation> {

  protected AnnotationFieldProcessor(@NotNull Class<? extends Annotation> supportedAnnotationClass, @NotNull Class<?> supportedClass) {
    super(supportedAnnotationClass, supportedClass);
  }

  @NotNull
  @Override
  public List<? super PsiElement> process(@NotNull PsiClass psiClass) {
    List<? super PsiElement> result = new ArrayList<PsiElement>();
    for (PsiField psiField : PsiClassUtil.collectClassFieldsIntern(psiClass)) {
//      PsiAnnotation psiAnnotation = AnnotationUtil.findAnnotation(psiField, Collections.singleton(getSupportedElement()), true);
//      if (null != psiAnnotation) {
//        process(psiField, psiAnnotation, result);
//      }
      for (PsiAnnotation psiAnnotation : AnnotUtil.findLbkAnnotation(psiField, getSupportedElement()))
        process(psiField, psiAnnotation, result);
    }
    return result;
  }

  @Override
  public Collection<LombokProblem> verifyElement(@NotNull PsiAnnotation psiAnnotation) {
    Collection<LombokProblem> result = Collections.emptyList();

    PsiField psiField = PsiTreeUtil.getParentOfType(psiAnnotation, PsiField.class);
    if (null != psiField) {
      result = new ArrayList<LombokProblem>(1);
      validate(psiAnnotation, psiField, new ProblemNewBuilder(result));
    }

    return result;
  }

  protected abstract boolean validate(@NotNull PsiAnnotation psiAnnotation, @NotNull PsiField psiField, @NotNull ProblemBuilder builder);

  public final void process(@NotNull PsiField psiField, @NotNull PsiAnnotation psiAnnotation, @NotNull List<? super PsiElement> target) {
    if (validate(psiAnnotation, psiField, ProblemEmptyBuilder.getInstance())) {
      processIntern(psiField, psiAnnotation, target);
    }
  }

  protected abstract void processIntern(PsiField psiField, PsiAnnotation psiAnnotation, List<? super PsiElement> target);

  protected void copyAnnotations(final PsiField fromPsiElement, final PsiModifierList toModifierList, final Pattern... patterns) {
    final Collection<String> annotationsToCopy = PsiAnnotationUtil.collectAnnotationsToCopy(fromPsiElement, patterns);
    for (String annotationFQN : annotationsToCopy) {
      toModifierList.addAnnotation(annotationFQN);
    }
  }

}
