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
package org.zeroturnaround.exec.stream;

/**
 * Constructs name for the caller logger.
 *
 * @author Rein Raudjärv
 */
public abstract class CallerLoggerUtil {

  /**
   * Returns full name for the caller class' logger.
   *
   * @param name name of the logger. In case of full name (it contains dots) same value is just returned.
   * In case of short names (no dots) the given name is prefixed by caller's class name and a dot.
   * In case of <code>null</code> the caller's class name is just returned.
   * @return full name for the caller class' logger.
   */
  public static String getName(String name) {
    return getName(name, 1);
  }

  /**
   * Returns full name for the caller class' logger.
   *
   * @param name name of the logger. In case of full name (it contains dots) same value is just returned.
   * In case of short names (no dots) the given name is prefixed by caller's class name and a dot.
   * In case of <code>null</code> the caller's class name is just returned.
   * @param level no of call stack levels to get the caller (0 means the caller of this method).
   * @return full name for the caller class' logger.
   */
  public static String getName(String name, int level) {
    level++;
    String fullName;
    if (name == null)
      fullName = getCallerClassName(level);
    else if (name.contains("."))
      fullName = name;
    else
      fullName = getCallerClassName(level) + "." + name;
    return fullName;
  }

  /**
   * @return caller class name of the given level.
   */
  private static String getCallerClassName(int level) {
    return Thread.currentThread().getStackTrace()[level + 2].getClassName();
  }

}
