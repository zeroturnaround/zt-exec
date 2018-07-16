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

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;
import org.zeroturnaround.exec.ProcessInitException;

public class ProcessInitExceptionTest {

  @Test
  public void testNull() throws Exception {
    Assert.assertNull(ProcessInitException.newInstance(null, new IOException()));
  }

  @Test
  public void testEmpty() throws Exception {
    Assert.assertNull(ProcessInitException.newInstance(null, new IOException("")));
  }

  @Test
  public void testSimple() throws Exception {
    ProcessInitException e = ProcessInitException.newInstance(
        "Could not run test.", new IOException("java.io.IOException: Cannot run program \"ls\": java.io.IOException: error=12, Cannot allocate memory"));
    Assert.assertNotNull(e);
    Assert.assertEquals("Could not run test. Error=12, Cannot allocate memory", e.getMessage());
    Assert.assertEquals(12, e.getErrorCode());
  }

  @Test
  public void testBeforeCode() throws Exception {
    ProcessInitException e = ProcessInitException.newInstance(
        "Could not run test.", new IOException("java.io.IOException: Cannot run program \"sleep\": java.io.IOException: CreateProcess error=2, The system cannot find the file specified"));
    Assert.assertNotNull(e);
    Assert.assertEquals("Could not run test. Error=2, The system cannot find the file specified", e.getMessage());
    Assert.assertEquals(2, e.getErrorCode());
  }

}
