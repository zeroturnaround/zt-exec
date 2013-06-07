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
package org.zeroturnaround.exec.test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.zeroturnaround.exec.InvalidExitValueException;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;
import org.zeroturnaround.exec.stream.ExecuteStreamHandler;


public class ProcessExecutorMainTest {

  @Test(expected=IllegalStateException.class)
  public void testNoCommand() throws Exception {
    new ProcessExecutor().execute();
  }

  @Test(expected=IOException.class)
  public void testNoSuchFile() throws Exception {
    new ProcessExecutor().command("unknown command").execute();
  }

  @Test
  public void testJavaVersion() throws Exception {
    int exit = new ProcessExecutor().command("java", "-version").execute().exitValue();
    Assert.assertEquals(0, exit);
  }

  @Test
  public void testJavaVersionCommandSplit() throws Exception {
    int exit = new ProcessExecutor().commandSplit("java -version").execute().exitValue();
    Assert.assertEquals(0, exit);
  }

  @Test
  public void testJavaVersionFuture() throws Exception {
    int exit = new ProcessExecutor().command("java", "-version").start().future().get().exitValue();
    Assert.assertEquals(0, exit);
  }

  @Test(expected=InvalidExitValueException.class)
  public void testJavaVersionExitValueCheck() throws Exception {
    new ProcessExecutor().command("java", "-version").exitValues(3).execute();
  }

  @Test(expected=InvalidExitValueException.class)
  public void testJavaVersionExitValueCheckTimeout() throws Exception {
    new ProcessExecutor().command("java", "-version").exitValues(3).timeout(60, TimeUnit.SECONDS).execute();
  }

  @Test
  public void testJavaVersionOutput() throws Exception {
    ProcessResult result = new ProcessExecutor().command("java", "-version").readOutput(true).execute();
    String str = result.outputUTF8();
    Assert.assertFalse(StringUtils.isEmpty(str));
  }

  @Test
  public void testJavaVersionOutputTwice() throws Exception {
    ProcessExecutor executor = new ProcessExecutor().command("java", "-version").readOutput(true);
    ProcessResult result = executor.execute();
    String str = result.outputUTF8();
    Assert.assertFalse(StringUtils.isEmpty(str));
    Assert.assertEquals(str, executor.execute().outputUTF8());
  }

  @Test
  public void testJavaVersionOutputFuture() throws Exception {
    ProcessResult result = new ProcessExecutor().command("java", "-version").readOutput(true).start().future().get();
    String str = result.outputUTF8();
    Assert.assertFalse(StringUtils.isEmpty(str));
  }

  @Test
  public void testJavaVersionLogInfo() throws Exception {
    // Just expect no errors - don't check the log file itself
    new ProcessExecutor().command("java", "-version").info("testJavaVersionLogInfo").execute();
  }

  @Test
  public void testJavaVersionLogInfoAndOutput() throws Exception {
    // Just expect no errors - don't check the log file itself
    ProcessResult result = new ProcessExecutor().command("java", "-version").info("testJavaVersionLogInfoAndOutput").readOutput(true).execute();
    String str = result.outputUTF8();
    Assert.assertFalse(StringUtils.isEmpty(str));
  }

  @Test
  public void testJavaVersionLogInfoAndOutputFuture() throws Exception {
    // Just expect no errors - don't check the log file itself
    ProcessResult result = new ProcessExecutor().command("java", "-version").info("testJavaVersionLogInfoAndOutputFuture").readOutput(true).start().future().get();
    String str = result.outputUTF8();
    Assert.assertFalse(StringUtils.isEmpty(str));
  }

  @Test
  public void testJavaVersionNoStreams() throws Exception {
    // Just expect no errors
    new ProcessExecutor().command("java", "-version").streams(null).execute();
  }

  @Test
  public void testProcessDestroyerEvents() throws Exception {
    MockProcessDestroyer mock = new MockProcessDestroyer();
    new ProcessExecutor().command("java", "-version").destroyer(mock).execute();
    Assert.assertNotNull(mock.added);
    Assert.assertEquals(mock.added, mock.removed);
  }

  @Test
  public void testProcessDestroyerEventsOnStreamsFail() throws Exception {
    MockProcessDestroyer mock = new MockProcessDestroyer();
    ExecuteStreamHandler streams = new SetFailExecuteStreamHandler();
    try {
      new ProcessExecutor().command("java", "-version").streams(streams).destroyer(mock).execute();
      Assert.fail("IOException expected");
    }
    catch (IOException e) {
      // Good
    }
    Assert.assertNull(mock.added);
    Assert.assertNull(mock.removed);
  }

  @Test
  public void testProcessExecutorListInit() throws Exception {
    // Use timeout in case we get stuck
    List<String> args = new ArrayList<String>() {
      {
        add("java");
        add("-cp");
        add("target/test-classes");
        add(HelloWorld.class.getName());
      }
    };
    ProcessExecutor exec = new ProcessExecutor(args);
    ProcessResult result = exec.readOutput(true).execute();
    Assert.assertEquals("Hello world!", result.outputUTF8());
  }
  
  @Test
  public void testProcessExecutorCommand() throws Exception {
    // Use timeout in case we get stuck
    List<String> args = new ArrayList<String>() {
      {
        add("java");
        add("-cp");
        add("target/test-classes");
        add(HelloWorld.class.getName());
      }
    };
    ProcessExecutor exec = new ProcessExecutor();
    exec.command(args);
    ProcessResult result = exec.readOutput(true).execute();
    Assert.assertEquals("Hello world!", result.outputUTF8());
  }
  
  @Test
  public void testProcessExecutorSetDirectory() throws Exception {
    // Use timeout in case we get stuck
    List<String> args = new ArrayList<String>() {
      {
        add("java");
        add("-cp");
        add("test-classes");
        add(HelloWorld.class.getName());
      }
    };
    ProcessExecutor exec = new ProcessExecutor().directory(new File("target"));
    exec.command(args);
    ProcessResult result = exec.readOutput(true).execute();
    Assert.assertEquals("Hello world!", result.outputUTF8());
  }
  
  @Test
  public void testProcessExecutorExitValues() throws Exception {
    // Use timeout in case we get stuck
    List<String> args = new ArrayList<String>() {
      {
        add("java");
        add("-cp");
        add("target/test-classes");
        add(ExitLikeABoss.class.getName());
        add("17");
      }
    };
    ProcessExecutor exec = new ProcessExecutor().exitValues(new int[]{17});
    exec.command(args);
    // no exception!
    exec.execute();
    
    // now lets make it throw an exception
    exec = new ProcessExecutor().exitValues(new int[]{15});
    exec.command(args);
    // no exception!
    boolean exceptionHappened = false;
    try {
      exec.execute();
    }
    catch (InvalidExitValueException e) {
      exceptionHappened = true;
    }
    Assert.assertTrue(exceptionHappened);
  }
}
