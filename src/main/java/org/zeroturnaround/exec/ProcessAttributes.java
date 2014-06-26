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

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Immutable set of attributes used to start a process.
 */
class ProcessAttributes {

  /**
   * The external program and its arguments.
   */
  private final List<String> command;

  /**
   * Working directory, <code>null</code> in case of current working directory.
   */
  private final File directory;

  /**
   * Environment variables which are added (removed in case of <code>null</code> values) to the started process.
   */
  private final Map<String,String> environment;

  /**
   * Set of accepted exit codes or <code>null</code> if all exit codes are allowed.
   */
  private final Set<Integer> allowedExitValues;

  public ProcessAttributes(List<String> command, File directory, Map<String, String> environment, Set<Integer> allowedExitValues) {
    this.command = command;
    this.directory = directory;
    this.environment = environment;
    this.allowedExitValues = allowedExitValues;
  }

  public List<String> getCommand() {
    return command;
  }

  public File getDirectory() {
    return directory;
  }

  public Map<String, String> getEnvironment() {
    return environment;
  }

  public Set<Integer> getAllowedExitValues() {
    return allowedExitValues;
  }

}
