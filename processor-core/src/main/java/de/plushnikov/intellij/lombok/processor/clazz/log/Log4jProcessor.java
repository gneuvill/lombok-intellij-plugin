package de.plushnikov.intellij.lombok.processor.clazz.log;

import lombok.extern.log4j.Log4j;

/**
 * @author Plushnikov Michail
 */
public class Log4jProcessor extends LogProcessor {

  private static final String LOGGER_TYPE = "org.apache.log4j.Logger";
  private static final String LOGGER_INITIALIZER = "org.apache.log4j.Logger.getLogger(%s.class)";

  public Log4jProcessor() {
    super(Log4j.class, LOGGER_TYPE, LOGGER_INITIALIZER);
  }
}
