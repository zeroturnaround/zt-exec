package org.zeroturnaround.exec;

import java.util.Map;

import org.slf4j.MDC;

/**
 * Restores the MDC context map for the target action.
 */
public class MDCRunnableAdapter implements Runnable {

  private final Runnable target;

  private final Map contextMap;

  public MDCRunnableAdapter(Runnable target, Map contextMap) {
    this.target = target;
    this.contextMap = contextMap;
  }

  public void run() {
    MDC.setContextMap(contextMap);
    try {
      target.run();
    }
    finally {
      MDC.clear();
    }
  }

}
