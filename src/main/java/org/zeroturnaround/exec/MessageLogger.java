package org.zeroturnaround.exec;

import org.slf4j.Logger;

/**
 * Logs messages at certain level.
 */
public interface MessageLogger {

  /**
   * Log a message at certain level according to the specified format and arguments.
   *
   * @param log       logger to be used.
   * @param format    the format string
   * @param arguments a list of arguments
   */
  void message(Logger log, String format, Object... arguments);

}
