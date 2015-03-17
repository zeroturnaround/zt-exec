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

/**
 * Process finished with an unexpected result.
 *
 * @author Rein Raudjärv
 * @since 1.8
 */
public class InvalidResultException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  /**
   * Actual exit value and process output.
   */
  private final ProcessResult result;

  /**
   * @param result result of execution (contains also the exit value)
   */
  public InvalidResultException(String message, ProcessResult result) {
    super(message);
    this.result = result;
  }

  /**
   * @return actual process result.
   */
  public ProcessResult getResult() {
    return result;
  }

  /**
   * @return the exit value of the finished process.
   */
  public int getExitValue() {
    return result.getExitValue();
  }

  /**
   * @return actual process result.
   * @deprecated use {@link #getResult()}
   */
  public ProcessResult result() {
    return getResult();
  }

  /**
   * @return the exit value of the finished process.
   * @deprecated use {@link #getExitValue()}
   */
  public int exitValue() {
    return getExitValue();
  }

}
