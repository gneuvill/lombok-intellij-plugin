package de.plushnikov.intellij.lombok.util;

import com.intellij.psi.*;
import com.intellij.psi.impl.source.PsiExtensibleClass;
import fj.F;
import fj.F2;
import gnu.trove.THashMap;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import static fj.data.Array.array;

/**
 * @author Plushnikov Michail
 */
public class PsiClassUtil {

  private static final F<PsiElement, PsiMethod> PSI_ELEMENT_TO_METHOD_FUNCTION = new F<PsiElement, PsiMethod>() {
    public PsiMethod f(PsiElement psiElement) {
      return (PsiMethod) psiElement;
    }
  };

  private static final F<PsiElement, PsiField> PSI_ELEMENT_TO_FIELD_FUNCTION = new F<PsiElement, PsiField>() {
    public PsiField f(PsiElement psiElement) {
      return (PsiField) psiElement;
    }
  };


  private static final F2<Class<?>, PsiElement, Boolean> psiElInstanceOf =
     new F2<Class<?>, PsiElement, Boolean>() {
      public Boolean f(Class<?> clazz, PsiElement psiElement) {
        return clazz.isInstance(psiElement);
      }
    };

  /**
   * Workaround to get all of original Methods of the psiClass, without calling PsiAugmentProvider infinitely
   *
   * @param psiClass psiClass to collect all of methods from
   * @return all intern methods of the class
   */
  @NotNull
  public static Collection<PsiMethod> collectClassMethodsIntern(@NotNull PsiClass psiClass) {
    if (psiClass instanceof PsiExtensibleClass) {
      return ((PsiExtensibleClass) psiClass).getOwnMethods();
    } else {
      return array(psiClass.getChildren())
          .filter(psiElInstanceOf.f(PsiMethod.class))
          .map(PSI_ELEMENT_TO_METHOD_FUNCTION)
          .toCollection();
    }
  }


  /**
   * Workaround to get all of original Fields of the psiClass, without calling PsiAugmentProvider infinitely
   *
   * @param psiClass psiClass to collect all of fields from
   * @return all intern fields of the class
   */
  @NotNull
  public static Collection<PsiField> collectClassFieldsIntern(@NotNull PsiClass psiClass) {
    if (psiClass instanceof PsiExtensibleClass) {
      return ((PsiExtensibleClass) psiClass).getOwnFields();
    } else {
      return array(psiClass.getChildren())
          .filter(psiElInstanceOf.f(PsiField.class))
          .map(PSI_ELEMENT_TO_FIELD_FUNCTION)
          .toCollection();
    }
  }

  @NotNull
  public static Collection<PsiMethod> collectClassConstructorIntern(@NotNull PsiClass psiClass) {
    final Collection<PsiMethod> psiMethods = collectClassMethodsIntern(psiClass);

    Collection<PsiMethod> classConstructors = new ArrayList<PsiMethod>(3);
    for (PsiMethod psiMethod : psiMethods) {
      if (psiMethod.isConstructor()) {
        classConstructors.add(psiMethod);
      }
    }
    return classConstructors;
  }

  @NotNull
  public static Collection<PsiMethod> collectClassStaticMethodsIntern(@NotNull PsiClass psiClass) {
    final Collection<PsiMethod> psiMethods = collectClassMethodsIntern(psiClass);

    Collection<PsiMethod> staticMethods = new ArrayList<PsiMethod>(5);
    for (PsiMethod psiMethod : psiMethods) {
      if (psiMethod.hasModifierProperty(PsiModifier.STATIC)) {
        staticMethods.add(psiMethod);
      }
    }
    return staticMethods;
  }

  public static boolean hasSuperClass(@NotNull final PsiClass psiClass) {
    final PsiClass superClass = psiClass.getSuperClass();
    return null != superClass && !CommonClassNames.JAVA_LANG_OBJECT.equals(superClass.getQualifiedName());
  }

  public static boolean hasMultiArgumentConstructor(@NotNull final PsiClass psiClass) {
    boolean result = false;
    final Collection<PsiMethod> definedConstructors = collectClassConstructorIntern(psiClass);
    for (PsiMethod psiMethod : definedConstructors) {
      if (psiMethod.getParameterList().getParametersCount() > 0) {
        result = true;
        break;
      }
    }
    return result;
  }

  /**
   * Creates a PsiType for a PsiClass enriched with generic substitution information if available
   */
  @NotNull
  public static PsiType getTypeWithGenerics(@NotNull PsiClass psiClass) {
    PsiType result;
    final PsiElementFactory factory = JavaPsiFacade.getElementFactory(psiClass.getProject());
    final PsiTypeParameter[] classTypeParameters = psiClass.getTypeParameters();
    if (classTypeParameters.length > 0) {
      Map<PsiTypeParameter, PsiType> substitutionMap = new THashMap<PsiTypeParameter, PsiType>();
      for (PsiTypeParameter typeParameter : classTypeParameters) {
        substitutionMap.put(typeParameter, factory.createType(typeParameter));
      }
      result = factory.createType(psiClass, factory.createSubstitutor(substitutionMap));
    } else {
      result = factory.createType(psiClass);
    }
    return result;
  }
}
