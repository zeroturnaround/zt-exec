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
package org.zeroturnaround.exec.stop;

/**
 * {@link ProcessStopper} implementation that does nothing - it keeps the process running.
 */
public class NopProcessStopper implements ProcessStopper {

  /**
   * Singleton instance of the {@link NopProcessStopper}.
   */
  public static final ProcessStopper INSTANCE = new NopProcessStopper();

  public void stop(Process process) {
    // don't stop the process
  }

}
