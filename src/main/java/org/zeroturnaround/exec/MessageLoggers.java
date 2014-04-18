package org.zeroturnaround.exec;

import org.slf4j.Logger;

/**
 * Contains {@link MessageLogger} instances for various log levels.
 */
public class MessageLoggers {

  public static final MessageLogger NOP = new MessageLogger() {
    public void message(Logger log, String format, Object... arguments) {
      // do nothing
    }
  };

  public static final MessageLogger TRACE = new MessageLogger() {
    public void message(Logger log, String format, Object... arguments) {
      log.trace(format, arguments);
    }
  };

  public static final MessageLogger DEBUG = new MessageLogger() {
    public void message(Logger log, String format, Object... arguments) {
      log.debug(format, arguments);
    }
  };

  public static final MessageLogger INFO = new MessageLogger() {
    public void message(Logger log, String format, Object... arguments) {
      log.info(format, arguments);
    }
  };

  private MessageLoggers() {
    // hide
  }

}
