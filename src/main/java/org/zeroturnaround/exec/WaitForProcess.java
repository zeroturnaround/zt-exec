/*
 * Copyright (C) 2013 ZeroTurnaround <support@zeroturnaround.com>
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.exec.listener.ProcessListener;
import org.zeroturnaround.exec.stream.ExecuteStreamHandler;


/**
 * Handles the executed process.
 *
 * @author Rein Raudjärv
 */
class WaitForProcess implements Callable<ProcessResult> {

  private static final Logger log = LoggerFactory.getLogger(WaitForProcess.class);

  private final Process process;

  /**
   * Set of accepted exit codes or <code>null</code> if all exit codes are allowed.
   */
  private final Set<Integer> allowedExitValues;

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

  public WaitForProcess(Process process, Set<Integer> allowedExitValues, ExecuteStreamHandler streams, ByteArrayOutputStream out, ProcessListener listener) {
    this.process = process;
    this.allowedExitValues = allowedExitValues;
    this.streams = streams;
    this.out = out;
    this.listener = listener;
  }

  /**
   * @return the sub process.
   */
  public Process getProcess() {
    return process;
  }

  public ProcessResult call() throws IOException, InterruptedException {
    try {
      int exit;
      boolean finished = false;
      try {
        exit = process.waitFor();
        finished = true;
        log.debug("{} stopped with exit code {}", this, exit);
      }
      finally {
        if (!finished) {
          log.debug("Stopping {}...", this);
          process.destroy();
        }

        if (streams != null)
          streams.stop();
        closeStreams(process);
      }
      ProcessOutput output = out == null ? null : new ProcessOutput(out.toByteArray());
      ProcessResult result = new ProcessResult(exit, output);
      checkExit(result);
      return result;
    }
    finally {
      // Invoke listeners - regardless process finished or got cancelled
      listener.afterStop(process);
    }
  }

  /**
   * Check the process exit value.
   */
  private void checkExit(ProcessResult result) {
    if (allowedExitValues != null && !allowedExitValues.contains(result.exitValue())) {
      throw new InvalidExitValueException(result, allowedExitValues);
    }
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
