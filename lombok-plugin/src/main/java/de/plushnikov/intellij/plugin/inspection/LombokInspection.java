package de.plushnikov.intellij.plugin.inspection;

import com.intellij.codeInsight.daemon.GroupNames;
import com.intellij.codeInspection.BaseJavaLocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.*;
import de.plushnikov.intellij.lombok.extension.LombokProcessorExtensionPoint;
import de.plushnikov.intellij.lombok.problem.LombokProblem;
import fr.neuville.lombok.processor.Processor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author Plushnikov Michail
 */
public class LombokInspection extends BaseJavaLocalInspectionTool {
  private static final Logger LOG = Logger.getInstance(LombokInspection.class.getName());

  private final ConcurrentMap<String, Collection<Processor<PsiElement>>> allProblemHandlers = new ConcurrentHashMap<>();

  public LombokInspection() {
    for (Processor<PsiElement> lombokInspector : LombokProcessorExtensionPoint.EP_NAME.getExtensions()) {
      Collection<Processor<PsiElement>> inspectorCollection = allProblemHandlers.get(lombokInspector.getSupportedElement());
      if (null == inspectorCollection) {
        inspectorCollection = new ArrayList<>(2);
        allProblemHandlers.put(lombokInspector.getSupportedElement(), inspectorCollection);
      }
      inspectorCollection.add(lombokInspector);

      LOG.debug(String.format("LombokInspection registered %s inspector", lombokInspector));
    }
  }

  @NotNull
  @Override
  public String getDisplayName() {
    return "Lombok annotations inspection";
  }

  @NotNull
  @Override
  public String getGroupDisplayName() {
    return GroupNames.BUGS_GROUP_NAME;
  }

  @NotNull
  @Override
  public String getShortName() {
    return "Lombok";
  }

  @Override
  public boolean isEnabledByDefault() {
    return true;
  }

  @NotNull
  @Override
  public PsiElementVisitor buildVisitor(@NotNull final ProblemsHolder holder, final boolean isOnTheFly) {
    return new JavaElementVisitor() {
      @Override
      public void visitReferenceExpression(PsiReferenceExpression expression) {
        // do nothing, just implement
      }

      @Override
      public void visitAnnotation(PsiAnnotation annotation) {
        super.visitAnnotation(annotation);

        final String qualifiedName = annotation.getQualifiedName();
        if (null != qualifiedName && allProblemHandlers.containsKey(qualifiedName)) {
          for (Processor inspector : allProblemHandlers.get(qualifiedName)) {
            Collection<LombokProblem> problems = inspector.verifyElement(annotation);
            for (LombokProblem problem : problems) {
              holder.registerProblem(annotation, problem.getMessage(), problem.getHighlightType(), problem.getQuickFixes());
            }
          }
        }
      }
    };
  }
}
