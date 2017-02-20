package org.zeroturnaround.exec;

import java.util.Map;
import java.util.concurrent.Callable;

import org.slf4j.MDC;

/**
 * Restores the MDC context map for the target action.
 */
public class MDCCallableAdapter<T> implements Callable<T> {

  private final Callable<T> target;

  private final Map contextMap;

  public MDCCallableAdapter(Callable<T> target, Map contextMap) {
    this.target = target;
    this.contextMap = contextMap;
  }

  public T call() throws Exception {
    MDC.setContextMap(contextMap);
    try {
      return target.call();
    }
    finally {
      MDC.clear();
    }
  }

}
