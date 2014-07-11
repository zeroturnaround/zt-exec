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
package org.zeroturnaround.exec.test.shutdown;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;

/**
 * Program which regularly writes into a file.
 * By checking whether the file gets updates we know whether this program is still running or it's finished.
 */
class WriterLoop {

  private static final File FILE = new File("writeLoop.data");

  private static final long INTERVAL = 1000;
  private static final long COUNT = 10;

  public static File getFile() {
    return FILE;
  }

  public static void main(String[] args) throws Exception {
    PrintWriter out = new PrintWriter(new FileWriter(FILE), true);
    try {
      out.println("Started");
      for (int i = 0; i < COUNT; i++) {
        out.println(i);
        Thread.sleep(INTERVAL);
      }
      out.println("Finished");
    }
    finally {
      out.close();
    }
  }

}
