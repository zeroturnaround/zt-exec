package org.zeroturnaround.exec.test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;
import org.zeroturnaround.exec.StartedProcess;

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
  
  @Test
  public void inputWillPreventCloseWhenProcessStops() throws Exception {
    // setup inputstream that will block on a read()
    PipedOutputStream pos = new PipedOutputStream();
    PipedInputStream pis = new PipedInputStream(pos);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();

    ProcessExecutor exec = new ProcessExecutor("java", "-cp", "target/test-classes",
        PrintArguments.class.getName());
    exec.redirectInput(pis).redirectOutput(baos);

    StartedProcess startedProcess = exec.start();
    
    ProcessResult result = startedProcess.getFuture().get(5000L, TimeUnit.MILLISECONDS);
    
    Assert.assertTrue(result != null);
  }
}
