package org.zeroturnaround.exec.test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.junit.Assert;
import org.junit.Test;
import org.zeroturnaround.exec.ProcessExecutor;

/**
 *
 */
public class ProcessExecutorInputStreamTest {
  @Test
  public void testWithInputAndRedirectOutput() throws Exception {
    String str = "Tere Minu Uus vihik";
    ByteArrayInputStream bais = new ByteArrayInputStream(str.getBytes());
    ByteArrayOutputStream baos = new ByteArrayOutputStream();

    ProcessExecutor exec = new ProcessExecutor("java", "-cp", "target/test-classes",
        PrintInputToOutput.class.getName());
    exec.redirectInput(bais).redirectOutput(baos);

    exec.execute();
    Assert.assertEquals(str, baos.toString());
  }
}
