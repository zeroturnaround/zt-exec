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

import org.apache.commons.lang3.SystemUtils;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;
import org.zeroturnaround.exec.ProcessExecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ProcessExecutorTimeoutTest {

  @Test
  public void testExecuteTimeout() throws Exception {
    try {
      // Use timeout in case we get stuck
      List<String> args = getWriterLoopCommand();
      new ProcessExecutor().command(args).timeout(1, TimeUnit.SECONDS).execute();
      Assert.fail("TimeoutException expected.");
    }
    catch (TimeoutException e) {
      Assert.assertThat(e.getMessage(), CoreMatchers.containsString("1 second"));
      Assert.assertThat(e.getMessage(), CoreMatchers.containsString(Loop.class.getName()));
    }
  }

  @Test
  public void testStartTimeout() throws Exception {
    try {
      // Use timeout in case we get stuck
      List<String> args = getWriterLoopCommand();
      new ProcessExecutor().command(args).start().getFuture().get(1, TimeUnit.SECONDS);
      Assert.fail("TimeoutException expected.");
    }
    catch (TimeoutException e) {
      Assert.assertNull(e.getMessage());
    }
  }

  private List<String> getWriterLoopCommand() {
    List<String> args = new ArrayList<String>() {
      {
        add("java");
        add("-cp");
        add("target/test-classes");
        add(Loop.class.getName());
      }
    };
    return args;
  }

  /*
   * This is a test copied from https://github.com/zeroturnaround/zt-exec/issues/56
   */
  @Test
  public void testExecuteTimeoutIssue56_1() throws Exception {
    try {
      List<String> commands = new ArrayList<String>();
      if (SystemUtils.IS_OS_WINDOWS) {
        // native sleep command is not available on Windows platform
        // mock using standard ping to localhost instead
        // (Windows ping does 4 requests which takes about 3 seconds)
        commands.add("ping");
        commands.add("127.0.0.1");
      }
      else {
        commands.add("sleep");
        commands.add("3");
      }
      new ProcessExecutor()
          .command(commands)
          .timeout(1, TimeUnit.SECONDS)
          .execute();
      Assert.fail("TimeoutException expected.");
    }
    catch (TimeoutException e) {
      Assert.assertThat(e.getMessage(), CoreMatchers.containsString("1 second"));
    }
  }

  /*
   * This is a test copied from https://github.com/zeroturnaround/zt-exec/issues/56
   */
  @Test
  public void testStartTimeoutIssue56_2() throws Exception {
    try {
      List<String> commands = new ArrayList<String>();
      if (SystemUtils.IS_OS_WINDOWS) {
        // native sleep command is not available on Windows platform
        // mock using standard ping to localhost instead
        // (Windows ping does 4 requests which takes about 3 seconds)
        commands.add("ping");
        commands.add("127.0.0.1");
      }
      else {
        commands.add("sleep");
        commands.add("3");
      }
      new ProcessExecutor()
          .command(commands)
          .start()
          .getFuture()
          .get(1, TimeUnit.SECONDS);
      Assert.fail("TimeoutException expected.");
    }
    catch (TimeoutException e) {
      Assert.assertNull(e.getMessage());
    }
  }

}
