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

import org.slf4j.Logger;

/**
 * Contains {@link MessageLogger} instances for various log levels.
 */
public class MessageLoggers {

  public static final MessageLogger NOP = new MessageLogger() {
    public void message(Logger log, String format, Object... arguments) {
      // do nothing
    }
  };

  public static final MessageLogger TRACE = new MessageLogger() {
    public void message(Logger log, String format, Object... arguments) {
      log.trace(format, arguments);
    }
  };

  public static final MessageLogger DEBUG = new MessageLogger() {
    public void message(Logger log, String format, Object... arguments) {
      log.debug(format, arguments);
    }
  };

  public static final MessageLogger INFO = new MessageLogger() {
    public void message(Logger log, String format, Object... arguments) {
      log.info(format, arguments);
    }
  };

  private MessageLoggers() {
    // hide
  }

}
