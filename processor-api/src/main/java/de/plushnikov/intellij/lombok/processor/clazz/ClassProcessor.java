package de.plushnikov.intellij.lombok.processor.clazz;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import de.plushnikov.intellij.lombok.processor.Processor;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @author Plushnikov Michail
 */
public interface ClassProcessor<T extends PsiElement> extends Processor<T> {
  void process(@NotNull PsiClass psiClass, @NotNull T psiElement, @NotNull List<? super PsiElement> target);
}
