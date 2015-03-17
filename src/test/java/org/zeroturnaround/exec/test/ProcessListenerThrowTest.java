package org.zeroturnaround.exec.test;

import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.zeroturnaround.exec.InvalidOutputException;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;
import org.zeroturnaround.exec.test.ProcessListenerSuccessTest.ProcessListenerImpl;

public class ProcessListenerThrowTest {

  @Test(expected=InvalidOutputException.class)
  public void testJavaVersion() throws Exception {
    new ProcessExecutor("java", "-version").readOutput(true).addListener(new ProcessListenerThrowImpl()).execute();
  }

  @Test(expected=InvalidOutputException.class)
  public void testJavaVersionWithTimeout() throws Exception {
    new ProcessExecutor("java", "-version").readOutput(true).addListener(new ProcessListenerThrowImpl()).timeout(1, TimeUnit.MINUTES).execute();
  }

  private static class ProcessListenerThrowImpl extends ProcessListenerImpl {

    @Override
    public void afterFinish(Process process, ProcessResult result) {
      super.afterFinish(process, result);

      if (result.getOutput().getString().contains("java version")) {
        throw new InvalidOutputException("Test", result);
      }
    }
  }

}
