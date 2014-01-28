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
 * Standard output of a finished process.
 *
 * @author Rein Raudjärv
 * @see ProcessExecutor
 */
public class ProcessOutput {

  /**
   * Process output (not <code>null</code>).
   */
  private final byte[] data;

  public ProcessOutput(byte[] data) {
    this.data = data;
  }

  /**
   * @return binary output of the finished process.
   */
  public byte[] getBytes() {
    return data;
  }

  /**
   * @return output of the finished process converted to a String using platform's default encoding.
   */
  public String getString() {
    return new String(getBytes());
  }

  /**
   * @return output of the finished process converted to UTF-8 String.
   */
  public String getUTF8() {
    return getString("UTF-8");
  }

  /**
   * @return output of the finished process converted to a String.
   *
   * @param charset The name of a supported char set.
   */
  public String getString(String charset) {
    try {
      return new String(getBytes(), charset);
    }
    catch (UnsupportedEncodingException e) {
      throw new IllegalStateException(e.getMessage());
    }
  }

}
