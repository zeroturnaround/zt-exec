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

import org.junit.Assert;
import org.junit.Test;
import org.zeroturnaround.exec.ProcessExecutor;


/**
 * Tests redirecting stream.
 *
 * @author Rein Raudjärv
 *
 * @see ProcessExecutor
 * @see HelloWorld
 */
public class ProcessExecutorHelloWorldTest {

  @Test
  public void testReadOutputAndError() throws Exception {
    String output = helloWorld().readOutput(true).execute().outputUTF8();
    Assert.assertEquals("Hello world!", output);
  }

  @Test
  public void testReadOutputOnly() throws Exception {
    String output = helloWorld().readOutput(true).redirectErrorStream(false).execute().outputUTF8();
    Assert.assertEquals("Hello ", output);
  }

  @Test
  public void testRedirectOutputAndError() throws Exception {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    helloWorld().redirectOutput(out).execute();
    Assert.assertEquals("Hello world!", new String(out.toByteArray()));
  }

  @Test
  public void testRedirectOutputAndErrorMerged() throws Exception {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ByteArrayOutputStream err = new ByteArrayOutputStream();
    helloWorld().redirectOutput(out).redirectError(err).execute();
    Assert.assertEquals("Hello ", new String(out.toByteArray()));
    Assert.assertEquals("world!", new String(err.toByteArray()));
  }

  @Test
  public void testRedirectOutputAndErrorAndReadOutput() throws Exception {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    String output = helloWorld().redirectOutput(out).readOutput(true).execute().outputUTF8();
    Assert.assertEquals("Hello world!", output);
    Assert.assertEquals("Hello world!", new String(out.toByteArray()));
  }

  @Test
  public void testRedirectOutputOnly() throws Exception {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    helloWorld().redirectOutput(out).redirectErrorStream(false).execute();
    Assert.assertEquals("Hello ", new String(out.toByteArray()));
  }

  @Test
  public void testRedirectOutputOnlyAndReadOutput() throws Exception {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    String output = helloWorld().redirectOutput(out).redirectErrorStream(false).readOutput(true).execute().outputUTF8();
    Assert.assertEquals("Hello ", output);
    Assert.assertEquals("Hello ", new String(out.toByteArray()));
  }

  @Test
  public void testRedirectErrorOnly() throws Exception {
    ByteArrayOutputStream err = new ByteArrayOutputStream();
    helloWorld().redirectError(err).redirectErrorStream(false).execute();
    Assert.assertEquals("world!", new String(err.toByteArray()));
  }

  @Test
  public void testRedirectErrorOnlyAndReadOutput() throws Exception {
    ByteArrayOutputStream err = new ByteArrayOutputStream();
    String output = helloWorld().redirectError(err).redirectErrorStream(false).readOutput(true).execute().outputUTF8();
    Assert.assertEquals("Hello ", output);
    Assert.assertEquals("world!", new String(err.toByteArray()));
  }

  @Test
  public void testRedirectOutputAndErrorSeparate() throws Exception {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ByteArrayOutputStream err = new ByteArrayOutputStream();
    helloWorld().redirectOutput(out).redirectError(err).redirectErrorStream(false).execute();
    Assert.assertEquals("Hello ", new String(out.toByteArray()));
    Assert.assertEquals("world!", new String(err.toByteArray()));
  }

  @Test
  public void testRedirectOutputAndErrorSeparateAndReadOutput() throws Exception {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ByteArrayOutputStream err = new ByteArrayOutputStream();
    String output = helloWorld().redirectOutput(out).redirectError(err).redirectErrorStream(false).readOutput(true).execute().outputUTF8();
    Assert.assertEquals("Hello ", output);
    Assert.assertEquals("Hello ", new String(out.toByteArray()));
    Assert.assertEquals("world!", new String(err.toByteArray()));
  }

  private ProcessExecutor helloWorld() {
    return new ProcessExecutor("java", "-cp", "target/test-classes", HelloWorld.class.getName());
  }

}
