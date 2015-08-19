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

import java.io.IOException;

/**
 * Creating a process failed providing an error code.
 *
 * <p>
 *   Wraps an {@link IOException} like:
 *   <ul>
 *     <li><code>java.io.IOException: Cannot run program "ls": java.io.IOException: error=12, Cannot allocate memory</code></li>
 *     <li><code>java.io.IOException: Cannot run program "ls": error=316, Unknown error: 316</code></li>
 *   </ul>
 * </p>
 */
public class ProcessInitException extends IOException {

  private static final String BEFORE_CODE = ": error=";
  private static final String AFTER_CODE = ", ";
  private static final String NEW_INFIX = " Error=";

  private final int errorCode;

  public ProcessInitException(String message, Throwable cause, int errorCode) {
    super(message, cause);
    this.errorCode = errorCode;
  }

  /**
   * @return error code raised when a process failed to start.
   */
  public int getErrorCode() {
    return errorCode;
  }

  /**
   * Try to wrap a given {@link IOException} into a {@link ProcessInitException}.
   *
   * @param prefix prefix to be added in the message.
   * @param e existing exception possibly containing an error code in its message.
   * @return new exception containing the prefix, error code and its description in the message plus the error code value as a field,
   *  <code>null</code> if we were unable to find an error code from the original message.
   */
  public static ProcessInitException newInstance(String prefix, IOException e) {
    String m = e.getMessage();
    if (m == null) {
      return null;
    }
    int i = m.lastIndexOf(BEFORE_CODE);
    if (i == -1) {
      return null;
    }
    int j = m.indexOf(AFTER_CODE, i);
    if (i == -1) {
      return null;
    }
    int code;
    try {
      code = Integer.parseInt(m.substring(i + BEFORE_CODE.length(), j));
    }
    catch (NumberFormatException n) {
      return null;
    }
    return new ProcessInitException(prefix + NEW_INFIX + m.substring(i + BEFORE_CODE.length()), e, code);
  }

}
