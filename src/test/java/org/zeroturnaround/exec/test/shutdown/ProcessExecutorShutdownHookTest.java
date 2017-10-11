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

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;
import org.junit.Assert;
import org.junit.Test;
import org.zeroturnaround.exec.ProcessExecutor;


/**
 * Tests destroying processes on JVM exit.
 */
public class ProcessExecutorShutdownHookTest {

  private static final long SLEEP_FOR_RECHECKING_FILE = 2000;

  @Test
  public void testDestroyOnExit() throws Exception {
    testDestroyOnExit(WriterLoopStarterBeforeExit.class, true);
  }

  @Test
  public void testDestroyOnExitInShutdownHook() throws Exception {
    testDestroyOnExit(WriterLoopStarterAfterExit.class, false);
  }

  private void testDestroyOnExit(Class<?> starter, boolean fileIsAlwaysCreated) throws Exception {
    File file = WriterLoop.getFile();
    if (file.exists())
      FileUtils.forceDelete(file);
    new ProcessExecutor("java", "-cp", SystemUtils.JAVA_CLASS_PATH, starter.getName()).redirectOutputAsInfo().execute();
    // After WriterLoopStarter has finished we expect that WriterLoop is also finished - no-one is updating the file
    if (fileIsAlwaysCreated || file.exists()) {
      checkFileStaysTheSame(file);
      FileUtils.forceDelete(file);
    }
  }

  private static void checkFileStaysTheSame(File file) throws InterruptedException {
    Assert.assertTrue(file.exists());
    long length = file.length();
    Thread.sleep(SLEEP_FOR_RECHECKING_FILE);
    Assert.assertEquals("File '" + file + "' was still updated.", length, file.length());
  }

}
