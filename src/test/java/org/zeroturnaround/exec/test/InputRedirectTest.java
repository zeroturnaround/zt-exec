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

import org.apache.commons.lang.SystemUtils;
import org.junit.Assume;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.exec.ProcessExecutor;

/**
 * Reported in https://github.com/zeroturnaround/zt-exec/issues/30
 */
public class InputRedirectTest {

  private static final Logger log = LoggerFactory.getLogger(InputRedirectTest.class);

  @Test
  public void testRedirectInput() throws Exception {
    String binTrue;
    if (SystemUtils.IS_OS_LINUX) {
      binTrue = "/bin/true";
    }
    else if (SystemUtils.IS_OS_MAC_OSX) {
      binTrue = "/usr/bin/true";
    }
    else {
      Assume.assumeTrue("Unsupported OS " + SystemUtils.OS_NAME, false);
      return; // Skip this test
    }

    // We need to put something in the buffer
    ByteArrayInputStream bais = new ByteArrayInputStream("foo".getBytes());
    ProcessExecutor exec = new ProcessExecutor().command(binTrue);
    // Test that we don't get IOException: Stream closed
    int exit = exec.redirectInput(bais).readOutput(true).execute().getExitValue();
    log.debug("Exit: {}", exit);
  }

}
