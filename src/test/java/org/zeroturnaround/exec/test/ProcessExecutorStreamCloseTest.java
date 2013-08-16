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
 * Tests that redirect target stream are not closed.
 *
 * @author Rein Raudjärv
 *
 * @see ProcessExecutor
 * @see HelloWorld
 */
public class ProcessExecutorStreamCloseTest {

  @Test
  public void testRedirectOutputNotClosed() throws Exception {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    RememberCloseOutputStream close = new RememberCloseOutputStream(out);
    helloWorld().redirectOutput(close).redirectErrorStream(false).execute();
    Assert.assertEquals("Hello ", new String(out.toByteArray()));
    Assert.assertFalse(close.isClosed());
  }

  @Test
  public void testRedirectErrorNotClosed() throws Exception {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    RememberCloseOutputStream close = new RememberCloseOutputStream(out);
    helloWorld().redirectError(close).execute();
    Assert.assertEquals("world!", new String(out.toByteArray()));
    Assert.assertFalse(close.isClosed());
  }

  private ProcessExecutor helloWorld() {
    return new ProcessExecutor("java", "-cp", "target/test-classes", HelloWorld.class.getName());
  }

}
