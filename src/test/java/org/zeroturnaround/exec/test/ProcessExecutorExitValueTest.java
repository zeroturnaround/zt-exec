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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.zeroturnaround.exec.InvalidExitValueException;
import org.zeroturnaround.exec.ProcessExecutor;


public class ProcessExecutorExitValueTest {

  @Test(expected=InvalidExitValueException.class)
  public void testJavaVersionExitValueCheck() throws Exception {
    new ProcessExecutor().command("java", "-version").exitValues(3).execute();
  }

  @Test(expected=InvalidExitValueException.class)
  public void testJavaVersionExitValueCheckTimeout() throws Exception {
    new ProcessExecutor().command("java", "-version").exitValues(3).timeout(60, TimeUnit.SECONDS).execute();
  }

  @Test(expected=InvalidExitValueException.class)
  public void testNonZeroExitValueByDefault() throws Exception {
    new ProcessExecutor(exitLikeABoss(17)).execute();
  }

  @Test
  public void testCustomExitValueValid() throws Exception {
    new ProcessExecutor(exitLikeABoss(17)).exitValues(17).execute();
  }

  @Test(expected=InvalidExitValueException.class)
  public void testCustomExitValueInvalid() throws Exception {
    new ProcessExecutor(exitLikeABoss(17)).exitValues(15).execute();
  }

  private static List<String> exitLikeABoss(int exitValue) {
    List<String> result = new ArrayList<String>();
    result.add("java");
    result.add("-cp");
    result.add("target/test-classes");
    result.add(ExitLikeABoss.class.getName());
    result.add(String.valueOf(exitValue));
    return result;
  }
}
