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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.exec.stream.ExecuteStreamHandler;

/**
 * Stops {@link ExecuteStreamHandler} from pumping the streams and closes them.
 */
public class StandardProcessCloser implements ProcessCloser {

  private static final Logger log = LoggerFactory.getLogger(StandardProcessCloser.class);

  protected final ExecuteStreamHandler streams;

  public StandardProcessCloser(ExecuteStreamHandler streams) {
    this.streams = streams;
  }

  public void close(Process process) throws IOException, InterruptedException {
    if (streams != null) {
      streams.stop();
    }
    closeStreams(process);
  }

  /**
   * Close the streams belonging to the given Process.
   */
  private void closeStreams(final Process process) throws IOException {
    IOException caught = null;

    try {
      process.getOutputStream().close();
    }
    catch (IOException e) {
      if (e.getMessage().equals("Stream closed")) {
        /**
         * OutputStream's contract for the close() method: If the stream is already closed then invoking this method has no effect.
         *
         * When a UNIXProcess exits ProcessPipeOutputStream automatically closes its target FileOutputStream and replaces it with NullOutputStream.
         * However the ProcessPipeOutputStream doesn't close itself at that moment.
         * As ProcessPipeOutputStream extends BufferedOutputStream extends FilterOutputStream closing it flushes the buffer first.
         * In Java 7 closing FilterOutputStream ignores any exception thrown by the target OutputStream. Since Java 8 these exceptions are now thrown.
         *
         * So since Java 8 after UNIXProcess detects the exit and there's something in the output buffer closing this stream throws IOException
         * with message "Stream closed" from NullOutputStream.
         */
        log.trace("Failed to close process output stream:", e);
      }
      else {
        log.error("Failed to close process output stream:", e);
        caught = add(caught, e);
      }
    }

    try {
      process.getInputStream().close();
    }
    catch (IOException e) {
      log.error("Failed to close process input stream:", e);
      caught = add(caught, e);
    }

    try {
      process.getErrorStream().close();
    }
    catch (IOException e) {
      log.error("Failed to close process error stream:", e);
      caught = add(caught, e);
    }

    if (caught != null) {
      throw caught;
    }
  }

  private static IOException add(IOException exception, IOException newException) {
    if (exception == null) {
      return newException;
    }
    ExceptionUtil.addSuppressed(exception, newException);
    return exception;
  }

}
