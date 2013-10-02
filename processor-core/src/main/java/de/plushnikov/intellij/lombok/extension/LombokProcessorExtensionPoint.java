package de.plushnikov.intellij.lombok.extension;

import com.intellij.openapi.extensions.ExtensionPointName;
import de.plushnikov.intellij.lombok.processor.Processor;

/**
 * Date: 21.07.13 Time: 12:54
 */
public class LombokProcessorExtensionPoint {
  public static final ExtensionPointName<Processor> EP_NAME = ExtensionPointName.create("Lombook Plugin.processor");
}
