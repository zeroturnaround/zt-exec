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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;
import org.zeroturnaround.exec.stream.ExecuteStreamHandler;
import org.zeroturnaround.exec.stream.slf4j.Slf4jStream;


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
    int exit = new ProcessExecutor().command("java", "-version").execute().getExitValue();
    Assert.assertEquals(0, exit);
  }

  @Test
  public void testJavaVersionCommandSplit() throws Exception {
    int exit = new ProcessExecutor().commandSplit("java -version").execute().getExitValue();
    Assert.assertEquals(0, exit);
  }

  @Test
  public void testJavaVersionFuture() throws Exception {
    int exit = new ProcessExecutor().command("java", "-version").start().getFuture().get().getExitValue();
    Assert.assertEquals(0, exit);
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
    ProcessResult result = new ProcessExecutor().command("java", "-version").readOutput(true).start().getFuture().get();
    String str = result.outputUTF8();
    Assert.assertFalse(StringUtils.isEmpty(str));
  }

  @Test
  public void testJavaVersionLogInfo() throws Exception {
    // Just expect no errors - don't check the log file itself
    new ProcessExecutor().command("java", "-version").redirectOutput(Slf4jStream.of("testJavaVersionLogInfo").asInfo()).execute();
  }

  @Test
  public void testJavaVersionLogInfoAndOutput() throws Exception {
    // Just expect no errors - don't check the log file itself
    ProcessResult result = new ProcessExecutor().command("java", "-version").redirectOutput(Slf4jStream.of("testJavaVersionLogInfoAndOutput").asInfo()).readOutput(true).execute();
    String str = result.outputUTF8();
    Assert.assertFalse(StringUtils.isEmpty(str));
  }

  @Test
  public void testJavaVersionLogInfoAndOutputFuture() throws Exception {
    // Just expect no errors - don't check the log file itself
    ProcessResult result = new ProcessExecutor().command("java", "-version").redirectOutput(Slf4jStream.of("testJavaVersionLogInfoAndOutputFuture").asInfo()).readOutput(true).start().getFuture().get();
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
}
