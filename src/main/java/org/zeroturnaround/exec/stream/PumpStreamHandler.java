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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Copies standard output and error of subprocesses to standard output and error
 * of the parent process. If output or error stream are set to null, any feedback
 * from that stream will be lost.
 */
public class PumpStreamHandler implements ExecuteStreamHandler {
 
  private static final Logger log = LoggerFactory.getLogger(PumpStreamHandler.class);

  protected Thread outputThread;

  protected Thread errorThread;

  protected Thread inputThread;

  protected final OutputStream out;

  protected final OutputStream err;

  protected final InputStream input;

  protected InputStreamPumper inputStreamPumper;

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
  public PumpStreamHandler(OutputStream outAndErr) {
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
  public PumpStreamHandler(OutputStream out, OutputStream err) {
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
  public PumpStreamHandler(OutputStream out, OutputStream err, InputStream input) {
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
  public void setProcessOutputStream(InputStream is) {
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
  public void setProcessErrorStream(InputStream is) {
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
  public void setProcessInputStream(OutputStream os) {
    if (input != null) {
      if (input == System.in) {
        inputThread = createSystemInPump(input, os);
      }
      else {
        inputThread = createPump(input, os, true);
      }
    }
    else {
      try {
        os.close();
      }
      catch (IOException e) {
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
    if (inputThread != null) {
      if (inputStreamPumper != null) {
        inputStreamPumper.stopProcessing();
      }
      // #33 Interrupt reading from a PipedInputStream to unblock the pumping thread
      inputThread.interrupt();
      log.trace("Joining input thread {}...", inputThread);
      try {
        inputThread.join();
        inputThread = null;
      }
      catch (InterruptedException e) {
        // ignore
      }
    }

    if (outputThread != null) {
      log.trace("Joining output thread {}...", outputThread);
      try {
        outputThread.join();
        outputThread = null;
      }
      catch (InterruptedException e) {
        // ignore
      }
    }

    if (errorThread != null) {
      log.trace("Joining error thread {}...", errorThread);
      try {
        errorThread.join();
        errorThread = null;
      }
      catch (InterruptedException e) {
        // ignore
      }
    }

    flush();
  }

  public void flush() {
    if (out != null) {
      log.trace("Flushing output stream {}...", out);
      try {
        out.flush();
      }
      catch (IOException e) {
        log.error("Got exception while flushing the output stream", e);
      }
    }

    if (err != null && err != out) {
      log.trace("Flushing error stream {}...", err);
      try {
        err.flush();
      }
      catch (IOException e) {
        log.error("Got exception while flushing the error stream", e);
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
  protected void createProcessOutputPump(InputStream is, OutputStream os) {
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
  protected void createProcessErrorPump(InputStream is, OutputStream os) {
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
  protected Thread createPump(InputStream is, OutputStream os) {
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
  protected Thread createPump(InputStream is, OutputStream os, boolean closeWhenExhausted) {
    Thread result = new Thread(new StreamPumper(is, os, closeWhenExhausted));
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
  protected Thread createSystemInPump(InputStream is, OutputStream os) {
    inputStreamPumper = new InputStreamPumper(is, os);
    final Thread result = new Thread(inputStreamPumper);
    result.setDaemon(true);
    return result;
  }

}
