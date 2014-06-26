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
package org.zeroturnaround.exec.test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.zeroturnaround.exec.stream.ExecuteStreamHandler;


public class SetFailExecuteStreamHandler implements ExecuteStreamHandler {

  public void setProcessInputStream(OutputStream os) throws IOException {
    throw new IOException();
  }

  public void setProcessErrorStream(InputStream is) throws IOException {
    throw new IOException();
  }

  public void setProcessOutputStream(InputStream is) throws IOException {
    throw new IOException();
  }

  public void start() throws IOException {
    // do nothing
  }

  public void stop() {
    // do nothing
  }

}
