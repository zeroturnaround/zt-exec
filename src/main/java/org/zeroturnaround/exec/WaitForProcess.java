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
package org.zeroturnaround.exec;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.exec.listener.ProcessListener;
import org.zeroturnaround.exec.stop.ProcessStopper;
import org.zeroturnaround.exec.stream.ExecuteStreamHandler;


/**
 * Handles the executed process.
 *
 * @author Rein Raudjärv
 */
class WaitForProcess implements Callable<ProcessResult> {

  private static final Logger log = LoggerFactory.getLogger(WaitForProcess.class);

  /**
   * In case {@link InvalidExitValueException} or {@link TimeoutException} is thrown and we have read the process output
   * we include the output up to this length in the error message. If the output is longer we truncate it.
   */
  private static final int MAX_OUTPUT_SIZE_IN_ERROR_MESSAGE = 5000;

  private final Process process;

  /**
   * Set of main attributes used to start the process.
   */
  private final ProcessAttributes attributes;

  /**
   * Helper for stopping the process in case of interruption.
   */
  private final ProcessStopper stopper;

  /**
   * Invoke {@link ExecuteStreamHandler#stop()} when the process has stopped (skipped if <code>null</code>).
   */
  private final ExecuteStreamHandler streams;

  /**
   * Buffer where the process output is redirected to or <code>null</code> if it's not used.
   */
  private final ByteArrayOutputStream out;

  /**
   * Process event listener (not <code>null</code>).
   */
  private final ProcessListener listener;

  /**
   * Helper for logging messages about starting and waiting for the processes.
   */
  private final MessageLogger messageLogger;

  /**
   * Thread which executes this operation.
   */
  private volatile Thread workerThread;

  public WaitForProcess(Process process, ProcessAttributes attributes, ProcessStopper stopper, ExecuteStreamHandler streams, ByteArrayOutputStream out, ProcessListener listener, MessageLogger messageLogger) {
    this.process = process;
    this.attributes = attributes;
    this.stopper = stopper;
    this.streams = streams;
    this.out = out;
    this.listener = listener;
    this.messageLogger = messageLogger;
  }

  /**
   * @return the sub process.
   */
  public Process getProcess() {
    return process;
  }

  public ProcessResult call() throws IOException, InterruptedException {
    try {
      workerThread = Thread.currentThread();
      int exit;
      boolean finished = false;
      try {
        exit = process.waitFor();
        finished = true;
        messageLogger.message(log, "{} stopped with exit code {}", this, exit);
      }
      finally {
        if (!finished) {
          messageLogger.message(log, "Stopping {}...", this);
          stopper.stop(process);
        }

        if (streams != null) {
          streams.stop();
        }
        closeStreams(process);
      }
      ProcessOutput output = getCurrentOutput();
      ProcessResult result = new ProcessResult(exit, output);
      checkExit(result);
      return result;
    }
    finally {
      // Invoke listeners - regardless process finished or got cancelled
      listener.afterStop(process);
      workerThread = null;
    }
  }

  private ProcessOutput getCurrentOutput() {
    return out == null ? null : new ProcessOutput(out.toByteArray());
  }

  /**
   * Check the process exit value.
   */
  private void checkExit(ProcessResult result) {
    Set<Integer> allowedExitValues = attributes.getAllowedExitValues();
    if (allowedExitValues != null && !allowedExitValues.contains(result.getExitValue())) {
      StringBuilder sb = new StringBuilder();
      sb.append("Unexpected exit value: ").append(result.getExitValue());
      sb.append(", allowed exit values: ").append(allowedExitValues);
      addExceptionMessageSuffix(sb, result.hasOutput() ? result.getOutput() : null);
      throw new InvalidExitValueException(sb.toString(), result);
    }
  }

  /**
   * Adds a suffix for an error message including:
   * <ul>
   *   <li>executed command</li>
   *   <li>working directory (unless it's inherited from parent)</li>
   *   <li>environment (unless it's the same with the parent)</li>
   *   <li>output read so far (unless it's not read)</li>
   * </ul>
   * @param sb where the suffix is appended to.
   */
  public void addExceptionMessageSuffix(StringBuilder sb) {
    addExceptionMessageSuffix(sb, getCurrentOutput());
  }

  private void addExceptionMessageSuffix(StringBuilder sb, ProcessOutput output) {
    sb.append(", executed command ").append(attributes.getCommand());
    if (attributes.getDirectory() != null) {
      sb.append(" in directory ").append(attributes.getDirectory());
    }
    if (!attributes.getEnvironment().isEmpty()) {
      sb.append(" with environment ").append(attributes.getEnvironment());
    }
    if (output != null) {
      int length = output.getBytes().length;
      String out = output.getString();
      if (out.length() <= MAX_OUTPUT_SIZE_IN_ERROR_MESSAGE) {
        sb.append(", output was ").append(length).append(" bytes:\n").append(out.trim());
      }
      else {
        sb.append(", output was ").append(length).append(" bytes (truncated):\n");
        int halfLimit = MAX_OUTPUT_SIZE_IN_ERROR_MESSAGE / 2;
        sb.append(out.substring(0, halfLimit)).append("\n...\n").append(out.substring(out.length() - halfLimit).trim());
      }
    }
  }

  /**
   * @return current stacktrace of the worker thread, <code>null</code> if this operation is currently not running.
   */
  public StackTraceElement[] getStackTrace() {
    Thread t = workerThread;
    return t == null ? null : t.getStackTrace();
  }

  /**
   * Close the streams belonging to the given Process.
   */
  private void closeStreams(final Process process) throws IOException {
    IOException caught = null;

    try {
      process.getInputStream().close();
    }
    catch (IOException e) {
      log.error("Failed to close process input stream:", e);
      caught = e;
    }

    try {
      process.getOutputStream().close();
    }
    catch (IOException e) {
      log.error("Failed to close process output stream:", e);
      caught = e;
    }

    try {
      process.getErrorStream().close();
    }
    catch (IOException e) {
      log.error("Failed to close process error stream:", e);
      caught = e;
    }

    if (caught != null) {
      throw caught;
    }
  }

  @Override
  public String toString() {
    return process.toString();
  }

}
