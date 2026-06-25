package org.zeroturnaround.exec;

import java.util.Map;

import org.slf4j.MDC;

/**
 * Restores the MDC context map for the target action.
 */
public class MDCRunnableAdapter implements Runnable {

  private final Runnable target;

  private final Map<String, String> contextMap;

  public MDCRunnableAdapter(Runnable target, Map<String, String> contextMap) {
    this.target = target;
    this.contextMap = contextMap;
  }

  @Override
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
