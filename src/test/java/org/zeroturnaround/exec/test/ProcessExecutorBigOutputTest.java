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

import java.io.ByteArrayOutputStream;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;
import org.zeroturnaround.exec.ProcessExecutor;


/**
 * Tests reading large output that doesn't fit into a buffer between this process and sub process.
 *
 * @author Rein Raudjärv
 *
 * @see ProcessExecutor
 * @see BigOutput
 */
public class ProcessExecutorBigOutputTest {

  @Test
  public void testDevNull() throws Exception {
    bigOutput().execute();
  }

  @Test
  public void testDevNullSeparate() throws Exception {
    bigOutput().redirectErrorStream(false).execute();
  }

  @Test
  public void testReadOutputAndError() throws Exception {
    String output = bigOutput().readOutput(true).execute().outputUTF8();
    Assert.assertEquals(repeat("+-"), output);
  }
  
  @Test
  public void testReadOutputOnly() throws Exception {
    String output = bigOutput().readOutput(true).redirectErrorStream(false).execute().outputUTF8();
    Assert.assertEquals(repeat("+"), output);
  }
  
  @Test
  public void testRedirectOutputOnly() throws Exception {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    bigOutput().redirectOutput(out).redirectErrorStream(false).execute();
    Assert.assertEquals(repeat("+"), new String(out.toByteArray()));
  }
  
  @Test
  public void testRedirectErrorOnly() throws Exception {
    ByteArrayOutputStream err = new ByteArrayOutputStream();
    bigOutput().redirectError(err).redirectErrorStream(false).execute();
    Assert.assertEquals(repeat("-"), new String(err.toByteArray()));
  }

  private ProcessExecutor bigOutput() {
    // Use timeout in case we get stuck
    return new ProcessExecutor("java", "-cp", "target/test-classes", BigOutput.class.getName()).timeout(10, TimeUnit.SECONDS);
  }

  private static String repeat(String s) {
    StringBuffer sb = new StringBuffer(BigOutput.LENGTH * 2);
    for (int i = 0; i < BigOutput.LENGTH; i++)
      sb.append(s);
    return sb.toString();
  }

}
