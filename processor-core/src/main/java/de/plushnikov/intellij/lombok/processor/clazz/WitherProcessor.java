package de.plushnikov.intellij.lombok.processor.clazz;

import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import de.plushnikov.intellij.lombok.problem.ProblemBuilder;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Annotation;
import java.util.List;

public class WitherProcessor extends AbstractLombokClassProcessor {
    protected WitherProcessor(@NotNull Class<? extends Annotation> supportedAnnotationClass, @NotNull Class<?> supportedClass) {
        super(supportedAnnotationClass, supportedClass);
    }

    @Override
    protected boolean validate(@NotNull PsiAnnotation psiAnnotation, @NotNull PsiClass psiClass, @NotNull ProblemBuilder builder) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    protected void processIntern(PsiClass psiClass, PsiAnnotation psiAnnotation, List<? super PsiElement> target) {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
