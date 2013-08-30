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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.junit.Assert;
import org.junit.Test;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.stream.PumpStreamHandler;

/**
 * Tests that test redirected input for the process to be run.
 */
public class InputStreamPumperTest {

  @Test
  public void testPumpFromInputToOutput() throws Exception {
    String str = "Tere\nMinu\nUus vihik\n";
    ByteArrayInputStream bais = new ByteArrayInputStream(str.getBytes());
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PumpStreamHandler handler = new PumpStreamHandler(baos, System.err, bais);

    ProcessExecutor exec = new ProcessExecutor("java", "-cp", "target/test-classes",
        PrintInputToOutput.class.getName()).readOutput(true);
    exec.streams(handler);

    String result = exec.execute().outputUTF8();
    Assert.assertEquals(str, result);
  }

}
