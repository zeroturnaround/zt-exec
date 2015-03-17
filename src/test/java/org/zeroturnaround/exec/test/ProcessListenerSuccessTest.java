package org.zeroturnaround.exec.test;

import org.junit.Assert;
import org.junit.Test;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;
import org.zeroturnaround.exec.listener.ProcessListener;

public class ProcessListenerSuccessTest {

  @Test
  public void testJavaVersion() throws Exception {
    ProcessListenerImpl listener = new ProcessListenerImpl();
    ProcessResult result = new ProcessExecutor("java", "-version").addListener(listener).execute();
    int exit = result.getExitValue();
    Assert.assertEquals(0, exit);
    Assert.assertNotNull(listener.executor);
    Assert.assertNotNull(listener.process);
    Assert.assertNotNull(listener.result);
    Assert.assertEquals(result,  listener.result);
  }

  static class ProcessListenerImpl extends ProcessListener {

    ProcessExecutor executor;

    Process process;

    ProcessResult result;

    @Override
    public void beforeStart(ProcessExecutor executor) {
      Assert.assertNotNull(executor);

      Assert.assertNull(this.executor);
      Assert.assertNull(this.process);
      Assert.assertNull(this.result);

      this.executor = executor;
    }

    @Override
    public void afterStart(Process process, ProcessExecutor executor) {
      Assert.assertNotNull(process);
      Assert.assertNotNull(executor);

      Assert.assertNotNull(this.executor);
      Assert.assertNull(this.process);
      Assert.assertNull(this.result);

      Assert.assertEquals(this.executor, executor);
      this.process = process;
    }

    @Override
    public void afterFinish(Process process, ProcessResult result) {
      Assert.assertNotNull(process);
      Assert.assertNotNull(result);

      Assert.assertNotNull(this.executor);
      Assert.assertNotNull(this.process);
      Assert.assertNull(this.result);

      Assert.assertEquals(this.process, process);
      this.result = result;
    }

    @Override
    public void afterStop(Process process) {
      Assert.assertNotNull(process);

      Assert.assertNotNull(this.executor);
      Assert.assertNotNull(this.process);
      Assert.assertNotNull(this.result);

      Assert.assertEquals(this.process, process);
    }

  }

}
