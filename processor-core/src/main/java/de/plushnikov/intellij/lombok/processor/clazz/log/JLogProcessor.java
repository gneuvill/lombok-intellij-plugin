package de.plushnikov.intellij.lombok.processor.clazz.log;

import lombok.extern.java.Log;

/**
 * @author Plushnikov Michail
 */
public class JLogProcessor extends LogProcessor {

  private static final String LOGGER_TYPE = "java.util.logging.Logger";
  private static final String LOGGER_INITIALIZER = "java.util.logging.Logger.getLogger(%s.class.getName())";

  public JLogProcessor() {
    super(Log.class, LOGGER_TYPE, LOGGER_INITIALIZER);
  }
}
