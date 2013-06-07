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

import java.io.UnsupportedEncodingException;

/**
 * Exit value and output of a finished process.
 *
 * @author Rein Raudjärv
 * @see ProcessExecutor
 */
public class ProcessResult {

  /**
   * Exit value of the finished process.
   */
  private final int exitValue;

  /**
   * Process output or <code>null</code> if it wasn't read.
   */
  private final byte[] output;

  public ProcessResult(int exitCode, byte[] output) {
    this.exitValue = exitCode;
    this.output = output;
  }

  /**
   * @return the exit value of the finished process.
   */
  public int exitValue() {
    return exitValue;
  }

  /**
   * @return binary output of the finished process.
   * You have to invoke {@link ProcessExecutor#readOutput()} to set the process output to be read.
   * 
   * @throws IllegalStateException if the output was not read.
   */
  public byte[] output() {
    if (output == null)
      throw new IllegalStateException("Process output was not read.");
    return output;
  }

  /**
   * @return output of the finished process converted to a String using platform's default encoding.
   * You have to invoke {@link ProcessExecutor#readOutput()} to set the process output to be read.
   *
   * @throws IllegalStateException if the output was not read.
   */
  public String outputString() {
    return new String(output());
  }

  /**
   * @return output of the finished process converted to UTF-8 String.
   * You have to invoke {@link ProcessExecutor#readOutput()} to set the process output to be read.
   * 
   * @throws IllegalStateException if the output was not read.
   */
  public String outputUTF8() {
    return outputString("UTF-8");
  }

  /**
   * @return output of the finished process converted to a String.
   * You have to invoke {@link ProcessExecutor#readOutput()} to set the process output to be read.
   * 
   * @param charset The name of a supported char set.
   * @throws IllegalStateException if the output was not read or the char set was not supported.
   */
  public String outputString(String charset) {
    try {
      return new String(output(), charset);
    }
    catch (UnsupportedEncodingException e) {
      throw new IllegalStateException(e.getMessage());
    }
  }

}
