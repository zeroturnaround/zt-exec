/*
 * Copyright (C) 2014 ZeroTurnaround <support@zeroturnaround.com>
 * Contains fragments of code from Apache Commons Exec, rights owned
 * by Apache Software Foundation (ASF).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * NOTICE: This file originates from the Apache Commons Exec package.
 * It has been modified to fit our needs.
 *
 * The following is the original header of the file in Apache Commons Exec:
 *
 *   Licensed to the Apache Software Foundation (ASF) under one or more
 *   contributor license agreements.  See the NOTICE file distributed with
 *   this work for additional information regarding copyright ownership.
 *   The ASF licenses this file to You under the Apache License, Version 2.0
 *   (the "License"); you may not use this file except in compliance with
 *   the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package org.zeroturnaround.exec.stream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Copies standard output and error of subprocesses to standard output and error
 * of the parent process. If output or error stream are set to null, any feedback
 * from that stream will be lost.
 */
public class PumpStreamHandler implements ExecuteStreamHandler {

  private static final Logger log = LoggerFactory.getLogger(PumpStreamHandler.class);

  protected static final long JOIN_TIMEOUT_IN_SECONDS = 10;

  private Thread outputThread;

  private Thread errorThread;

  private Thread inputThread;

  private final OutputStream out;

  private final OutputStream err;

  private final InputStream input;

  private InputStreamPumper inputStreamPumper;

  /**
   * Construct a new <CODE>PumpStreamHandler</CODE>.
   */
  public PumpStreamHandler() {
    this(System.out, System.err);
  }

  /**
   * Construct a new <CODE>PumpStreamHandler</CODE>.
   *
   * @param outAndErr
   *            the output/error <CODE>OutputStream</CODE>.
   */
  public PumpStreamHandler(final OutputStream outAndErr) {
    this(outAndErr, outAndErr);
  }

  /**
   * Construct a new <CODE>PumpStreamHandler</CODE>.
   *
   * @param out
   *            the output <CODE>OutputStream</CODE>.
   * @param err
   *            the error <CODE>OutputStream</CODE>.
   */
  public PumpStreamHandler(final OutputStream out, final OutputStream err) {
    this(out, err, null);
  }

  /**
   * Construct a new <CODE>PumpStreamHandler</CODE>.
   *
   * @param out
   *            the output <CODE>OutputStream</CODE>.
   * @param err
   *            the error <CODE>OutputStream</CODE>.
   * @param input
   *            the input <CODE>InputStream</CODE>.
   */
  public PumpStreamHandler(final OutputStream out, final OutputStream err,
      final InputStream input) {

    this.out = out;
    this.err = err;
    this.input = input;
  }

  /**
   * Set the <CODE>InputStream</CODE> from which to read the standard output
   * of the process.
   *
   * @param is
   *            the <CODE>InputStream</CODE>.
   */
  public void setProcessOutputStream(final InputStream is) {
    if (out != null) {
      createProcessOutputPump(is, out);
    }
  }

  /**
   * Set the <CODE>InputStream</CODE> from which to read the standard error
   * of the process.
   *
   * @param is
   *            the <CODE>InputStream</CODE>.
   */
  public void setProcessErrorStream(final InputStream is) {
    if (err != null) {
      createProcessErrorPump(is, err);
    }
  }

  /**
   * Set the <CODE>OutputStream</CODE> by means of which input can be sent
   * to the process.
   *
   * @param os
   *            the <CODE>OutputStream</CODE>.
   */
  public void setProcessInputStream(final OutputStream os) {
    if (input != null) {
      if (input == System.in) {
        inputThread = createSystemInPump(input, os);
      } else {
        inputThread = createPump(input, os, true);
      }        }
    else {
      try {
        os.close();
      } catch (IOException e) {
        log.info("Got exception while closing output stream", e);
      }
    }
  }

  /**
   * Start the <CODE>Thread</CODE>s.
   */
  public void start() {
    if (outputThread != null) {
      outputThread.start();
    }
    if (errorThread != null) {
      errorThread.start();
    }
    if (inputThread != null) {
      inputThread.start();
    }
  }

  /**
   * Stop pumping the streams.
   */
  public void stop() {
    stopInput();

    join(JOIN_TIMEOUT_IN_SECONDS, TimeUnit.SECONDS);

    flush();
  }

  protected void stopInput() {
    if (inputStreamPumper != null) {
      inputStreamPumper.stopProcessing();
    }
  }

  protected void join(long timeout, TimeUnit unit) {
    long finish = System.currentTimeMillis() + unit.toMillis(timeout);

    if (outputThread != null) {
      log.trace("Joining output thread {}...", outputThread);
      try {
        long millis = finish - System.currentTimeMillis();
        if (millis > 0) {
          outputThread.join(millis);
        }
        outputThread = null;
      } catch (InterruptedException e) {
        // ignore
      }
    }

    if (errorThread != null) {
      log.trace("Joining error thread {}...", errorThread);
      try {
        long millis = finish - System.currentTimeMillis();
        if (millis > 0) {
          errorThread.join(millis);
        }
        errorThread = null;
      } catch (InterruptedException e) {
        // ignore
      }
    }

    if (inputThread != null) {
      log.trace("Joining input thread {}...", inputThread);
      try {
        long millis = finish - System.currentTimeMillis();
        if (millis > 0) {
          inputThread.join(millis);
        }
        inputThread = null;
      } catch (InterruptedException e) {
        // ignore
      }
    }
  }

  protected void flush() {
    if (err != null && err != out) {
      log.trace("Flushing error stream {}...", err);
      try {
        err.flush();
      } catch (IOException e) {
        log.error("Got exception while flushing the error stream", e);
      }
    }

    if (out != null) {
      log.trace("Flushing output stream {}...", out);
      try {
        out.flush();
      } catch (IOException e) {
        log.error("Got exception while flushing the output stream", e);
      }
    }
  }

  /**
   * Get the output stream.
   *
   * @return <CODE>OutputStream</CODE>.
   */
  public OutputStream getOut() {
    return out;
  }

  /**
   * Get the error stream.
   *
   * @return <CODE>OutputStream</CODE>.
   */
  public OutputStream getErr() {
    return err;
  }

  /**
   * Get the input stream.
   *
   * @return <CODE>InputStream</CODE>.
   */
  public InputStream getInput() {
    return input;
  }

  /**
   * Create the pump to handle process output.
   *
   * @param is
   *            the <CODE>InputStream</CODE>.
   * @param os
   *            the <CODE>OutputStream</CODE>.
   */
  protected void createProcessOutputPump(final InputStream is,
      final OutputStream os) {
    outputThread = createPump(is, os);
  }

  /**
   * Create the pump to handle error output.
   *
   * @param is
   *            the <CODE>InputStream</CODE>.
   * @param os
   *            the <CODE>OutputStream</CODE>.
   */
  protected void createProcessErrorPump(final InputStream is,
      final OutputStream os) {
    errorThread = createPump(is, os);
  }

  /**
   * Creates a stream pumper to copy the given input stream to the given
   * output stream.
   *
   * @param is the input stream to copy from
   * @param os the output stream to copy into
   * @return the stream pumper thread
   */
  protected Thread createPump(final InputStream is, final OutputStream os) {
    return createPump(is, os, false);
  }

  /**
   * Creates a stream pumper to copy the given input stream to the given
   * output stream.
   *
   * @param is the input stream to copy from
   * @param os the output stream to copy into
   * @param closeWhenExhausted close the output stream when the input stream is exhausted
   * @return the stream pumper thread
   */
  protected Thread createPump(final InputStream is, final OutputStream os,
      final boolean closeWhenExhausted) {
    final Thread result = new Thread(new StreamPumper(is, os,
        closeWhenExhausted));
    result.setDaemon(true);
    return result;
  }


  /**
   * Creates a stream pumper to copy the given input stream to the given
   * output stream.
   *
   * @param is the System.in input stream to copy from
   * @param os the output stream to copy into
   * @return the stream pumper thread
   */
  private Thread createSystemInPump(InputStream is, OutputStream os) {
    inputStreamPumper = new InputStreamPumper(is, os);
    final Thread result = new Thread(inputStreamPumper);
    result.setDaemon(true);
    return result;
  }

}
