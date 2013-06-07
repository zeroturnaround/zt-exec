/*
 * Copyright (C) 2013 ZeroTurnaround <support@zeroturnaround.com>
 * Contains fragments of code from Apache Commons Exec, rights owned
 * by Apache Software Foundation (ASF).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * NOTICE: This file originates from the Apache Commons Exec package.
 * It has been modified to fit our needs.
 * 
 * The following is the original header of the file in Apache Commons Exec:  
 * 
 *   Licensed to the Apache Software Foundation (ASF) under one or more
 *   contributor license agreements.  See the NOTICE file distributed with
 *   this work for additional information regarding copyright ownership.
 *   The ASF licenses this file to You under the Apache License, Version 2.0
 *   (the "License"); you may not use this file except in compliance with
 *   the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package org.zeroturnaround.exec.stream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Used by <code>Execute</code> to handle input and output stream of
 * subprocesses.
 */
public interface ExecuteStreamHandler {

  /**
   * Install a handler for the input stream of the subprocess.
   * 
   * @param os
   *            output stream to write to the standard input stream of the
   *            subprocess
   */
  void setProcessInputStream(OutputStream os) throws IOException;

  /**
   * Install a handler for the error stream of the subprocess.
   * 
   * @param is
   *            input stream to read from the error stream from the subprocess
   */
  void setProcessErrorStream(InputStream is) throws IOException;

  /**
   * Install a handler for the output stream of the subprocess.
   * 
   * @param is
   *            input stream to read from the error stream from the subprocess
   */
  void setProcessOutputStream(InputStream is) throws IOException;

  /**
   * Start handling of the streams.
   */
  void start() throws IOException;

  /**
   * Stop handling of the streams - will not be restarted.
   * Will wait for pump threads to complete.
   */
  void stop();
}
