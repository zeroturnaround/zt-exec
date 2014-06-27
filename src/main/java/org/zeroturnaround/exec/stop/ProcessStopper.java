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

import java.util.concurrent.Future;

/**
 * Abstraction for stopping sub processes.
 * <p>
 * This is used in case a process runs too long (timeout is reached) or it's cancelled via {@link Future#cancel(boolean)}.
 */
public interface ProcessStopper {

  /**
   * Stops a given sub process.
   * It does not wait for the process to actually stop and it has no guarantee that the process terminates.
   *
   * @param process sub process being stopped (not <code>null</code>).
   */
  void stop(Process process);

}
