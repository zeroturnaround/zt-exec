/*
 * Copyright (C) 2014 ZeroTurnaround <support@zeroturnaround.com>
 * Contains fragments of code from Apache Commons Exec, rights owned
 * by Apache Software Foundation (ASF).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.zeroturnaround.exec.close;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.exec.stream.ExecuteStreamHandler;
import org.zeroturnaround.exec.stream.PumpStreamHandler;

/**
 * Same as {@link StandardProcessCloser} but only waits fixed period for the closing.
 * On timeout a warning is logged but no error is thrown.
 * <p>
 * This is used on Windows where sometimes sub process' streams do not close properly.
 */
public class TimeoutProcessCloser extends StandardProcessCloser {

  private static final Logger log = LoggerFactory.getLogger(TimeoutProcessCloser.class);

  private final long timeout;

  private final TimeUnit unit;

  /**
   * Creates new instance of {@link TimeoutProcessCloser}.
   *
   * @param streams helper for pumping the streams.
   * @param timeout how long should we wait for the closing.
   * @param unit unit of the timeout value.
   */
  public TimeoutProcessCloser(ExecuteStreamHandler streams, long timeout, TimeUnit unit) {
    super(streams);
    this.timeout = timeout;
    this.unit = unit;
  }

  public void close(final Process process) throws IOException, InterruptedException {
    ExecutorService service = Executors.newSingleThreadScheduledExecutor();
    Future<Void> future = service.submit(new Callable<Void>() {
      public Void call() throws Exception {
        doClose(process);
        return null;
      }
    });
    // Previously submitted tasks are executed but no new tasks will be accepted.
    service.shutdown();

    try {
      future.get(timeout, unit);
    }
    catch (ExecutionException e) {
      throw new IllegalStateException("Could not close streams of " + process, e.getCause());
    }
    catch (TimeoutException e) {
      log.warn("Could not close streams of {} in {} {}", process, timeout, getUnitsAsString(timeout, unit));
    }
    finally {
      // Ensure that any data received so far is flushed from buffers
      if (streams instanceof PumpStreamHandler) {
        ((PumpStreamHandler) streams).flush();
      }
    }
  }

  protected void doClose(final Process process) throws IOException, InterruptedException {
    super.close(process);
  }

  private static String getUnitsAsString(long d, TimeUnit unit) {
    String result = unit.toString().toLowerCase();
    if (d == 1) {
      result = result.substring(0, result.length() - 1);
    }
    return result;
  }

}
