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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeoutException;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import org.zeroturnaround.exec.ProcessExecutor;


/**
 * Tests passing command line arguments to a Java process.
 */
public class ProcessExecutorCommandLineTest {

  @Test
  public void testOneArg() throws Exception {
    testArguments("foo");
  }

  @Test
  public void testTwoArgs() throws Exception {
    testArguments("foo", "bar");
  }

  @Test
  public void testSpaces() throws Exception {
    testArguments("foo foo", "bar bar");
  }

  @Test
  public void testQuotes() throws Exception {
    String[] args = new String[]{"\"a\"", "b \"c\" d", "f \"e\"", "\"g\" h"};
    List<String> expected = Arrays.asList("\"a\"", "b \"c\" d", "f \"e\"", "\"g\" h");
    if (System.getProperty("os.name").startsWith("Windows"))
    	expected = Arrays.asList("a", "b c d", "f e", "g h");
    testArguments(expected, args);
  }

  @Test
  public void testSlashes() throws Exception {
    testArguments("/o\\", "\\/.*");
  }

  private void testArguments(String... args) throws IOException, InterruptedException, TimeoutException {
    byte[] bytes = printArguments(args).execute().output();
    List<String> expected = Arrays.asList(args);
    List<String> actual = IOUtils.readLines(new ByteArrayInputStream(bytes));
    Assert.assertEquals(expected, actual);
  }
  
  private void testArguments(List<String> expected, String... args) throws IOException, InterruptedException, TimeoutException {
    byte[] bytes = printArguments(args).execute().output();
    List<String> actual = IOUtils.readLines(new ByteArrayInputStream(bytes));
    Assert.assertEquals(expected, actual);
  }

  private ProcessExecutor printArguments(String... args) {
    List<String> command = new ArrayList<String>();
    command.addAll(Arrays.asList("java", "-cp", "target/test-classes", PrintArguments.class.getName()));
    command.addAll(Arrays.asList(args));
    return new ProcessExecutor(command).readOutput(true);
  }

}
