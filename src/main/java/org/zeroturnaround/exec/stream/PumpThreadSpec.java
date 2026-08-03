/*
 * Copyright (C) 2026 Neeme Praks
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
package org.zeroturnaround.exec.stream;

/**
 * Everything known about a pump thread before it exists, so that
 * {@link PumpStreamHandler#newThread(PumpThreadSpec)} can construct the thread from it in
 * one go rather than having its properties assigned afterwards.
 */
public class PumpThreadSpec {

  private final Runnable task;

  private final String role;

  private final String name;

  /**
   * @param task the task to be run in the background.
   * @param role which of the process streams the thread pumps.
   * @param name the name to give the thread.
   */
  public PumpThreadSpec(Runnable task, String role, String name) {
    this.task = task;
    this.role = role;
    this.name = name;
  }

  /**
   * @return the task to be run in the background.
   */
  public Runnable getTask() {
    return task;
  }

  /**
   * @return which of the process streams the thread pumps, one of
   *         {@link PumpStreamHandler#STDOUT}, {@link PumpStreamHandler#STDERR} or
   *         {@link PumpStreamHandler#STDIN}, or {@link PumpStreamHandler#PUMP} when a
   *         subclass created the pump without telling which stream it is for.
   */
  public String getRole() {
    return role;
  }

  /**
   * @return the name to give the thread.
   */
  public String getName() {
    return name;
  }

  @Override
  public String toString() {
    return "PumpThreadSpec[role=" + role + ", name=" + name + ", task=" + task + "]";
  }

}
