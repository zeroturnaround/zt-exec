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
 * Process finished with a forbidden exit value.
 *
 * @author Rein Raudjärv
 *
 * @see ProcessExecutor#exitValues(Integer...)
 */
public class InvalidExitValueException extends InvalidResultException {

  private static final long serialVersionUID = 1L;

  /**
   * @param result result of execution (contains also the exit value)
   */
  public InvalidExitValueException(String message, ProcessResult result) {
    super(message, result);
  }

}
