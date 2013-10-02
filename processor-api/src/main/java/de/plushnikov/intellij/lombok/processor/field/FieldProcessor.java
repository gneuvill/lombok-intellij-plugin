package de.plushnikov.intellij.lombok.processor.field;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import fr.neuville.lombok.processor.Processor;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @author Plushnikov Michail
 */
public interface FieldProcessor<T extends PsiElement> extends Processor<T> {
  void process(@NotNull PsiField psiField, @NotNull T psiElement, @NotNull List<? super PsiElement> target);
}
