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
package org.zeroturnaround.exec.listener;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.zeroturnaround.exec.ProcessExecutor;


/**
 * Composite process event handler.
 *
 * @author Rein Raudjärv
 */
public class CompositeProcessListener extends ProcessListener implements Cloneable {

  private final List<ProcessListener> children = new CopyOnWriteArrayList<ProcessListener>();

  public CompositeProcessListener() {
    // no children
  }

  public CompositeProcessListener(List<ProcessListener> children) {
    this.children.addAll(children);
  }

  /**
   * Add new listener.
   *
   * @param listener listener to be added.
   */
  public void add(ProcessListener listener) {
    children.add(listener);
  }

  /**
   * Remove existing listener.
   *
   * @param listener listener to be removed.
   */
  public void remove(ProcessListener listener) {
    children.remove(listener);
  }

  /**
   * Remove all existing listeners.
   */
  public void clear() {
    children.clear();
  }

  public CompositeProcessListener clone() {
    return new CompositeProcessListener(children);
  }

  @Override
  public void beforeStart(ProcessExecutor executor) {
    for (ProcessListener child : children) {
      child.beforeStart(executor);
    }
  }

  @Override
  public void afterStart(Process process, ProcessExecutor executor) {
    for (ProcessListener child : children) {
      child.afterStart(process, executor);
    }
  }

  @Override
  public void afterStop(Process process) {
    for (ProcessListener child : children) {
      child.afterStop(process);
    }
  }

}
