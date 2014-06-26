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

import java.util.concurrent.Future;

/**
 * Represents a process that has started. It may or may not have finished.
 *
 * @author Rein Raudjärv
 */
public class StartedProcess {

  /**
   * The sub process started.
   */
  private final Process process;

  /**
   * The asynchronous result of the started process.
   */
  private final Future<ProcessResult> future;

  public StartedProcess(Process process, Future<ProcessResult> future) {
    this.process = process;
    this.future = future;
  }

  /**
   * @return the started process.
   */
  public Process getProcess() {
    return process;
  }

  /**
   * @return asynchronous result of the started process.
   */
  public Future<ProcessResult> getFuture() {
    return future;
  }

  /**
   * @return the started process.
   * @deprecated use {@link #getProcess()} instead.
   */
  public Process process() {
    return getProcess();
  }

  /**
   * @return asynchronous result of the started process.
   * @deprecated use {@link #getFuture()} instead.
   */
  public Future<ProcessResult> future() {
    return getFuture();
  }

}
