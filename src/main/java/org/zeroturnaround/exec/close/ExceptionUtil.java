package org.zeroturnaround.exec.close;

import java.lang.reflect.Method;

class ExceptionUtil {

  /**
   * Throwable.addSuppressed(Throwable) added in Java 7.
   */
  private static final Method METHOD_ADD_SUPPRESSED = findAddSuppressed();

  private static Method findAddSuppressed() {
    try {
      return Throwable.class.getDeclaredMethod("addSuppressed", Throwable.class);
    }
    catch (Exception e) {
      // ignore
    }
    return null;
  }

  /**
   * If supported. appends the specified exception to the exceptions that were suppressed in order to deliver this exception.
   */
  public static void addSuppressed(Throwable t, Throwable suppressed) {
    if (METHOD_ADD_SUPPRESSED != null) {
      try {
        METHOD_ADD_SUPPRESSED.invoke(t, suppressed);
      }
      catch (Exception e) {
        throw new IllegalStateException("Could not add suppressed exception:", e);
      }
    }
  }

}
