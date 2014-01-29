package org.zeroturnaround.exec.stream.slf4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.exec.stream.CallerLoggerUtil;

/**
 * Creates output streams that write to {@link Logger}s.
 *
 * @author Rein Raudjärv
 */
public class Slf4jStream {

  private final Logger log;

  private Slf4jStream(Logger log) {
    this.log = log;
  }

  /**
   * @param log logger which an output stream redirects to.
   * @return Slf4jStream with the given logger.
   */
  public static Slf4jStream of(Logger log) {
    return new Slf4jStream(log);
  }

  /**
   * @param name logger's name (full or short).
   *    In case of short name (no dots) the given name is prefixed by caller's class name and a dot.
   * @return Slf4jStream with the given logger.
   */
  public static Slf4jStream of(String name) {
    return of(LoggerFactory.getLogger(CallerLoggerUtil.getName(name, 1)));
  }

  /**
   * @return Slf4jStream with the logger of caller of this method.
   */
  public static Slf4jStream ofCaller() {
    return of(LoggerFactory.getLogger(CallerLoggerUtil.getName(null, 1)));
  }

  /**
   * @return output stream that writes <code>trace</code> level.
   */
  public Slf4jOutputStream asTrace() {
    return new Slf4jTraceOutputStream(log);
  }

  /**
   * @return output stream that writes <code>debug</code> level.
   */
  public Slf4jOutputStream asDebug() {
    return new Slf4jDebugOutputStream(log);
  }

  /**
   * @return output stream that writes <code>info</code> level.
   */
  public Slf4jOutputStream asInfo() {
    return new Slf4jInfoOutputStream(log);
  }

  /**
   * @return output stream that writes <code>warn</code> level.
   */
  public Slf4jOutputStream asWarn() {
    return new Slf4jWarnOutputStream(log);
  }

  /**
   * @return output stream that writes <code>error</code> level.
   */
  public Slf4jOutputStream asError() {
    return new Slf4jErrorOutputStream(log);
  }

}
