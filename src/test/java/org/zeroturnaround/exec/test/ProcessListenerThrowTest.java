package org.zeroturnaround.exec.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Assert;
import org.junit.Test;
import org.zeroturnaround.exec.InvalidOutputException;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;
import org.zeroturnaround.exec.listener.ProcessListener;
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

  @Test
  public void testAfterStartFailureStopsProcessAndNotifiesAllListeners() throws Exception {
    MockProcessDestroyer destroyer = new MockProcessDestroyer();

    Throwable thrown = startWithFailingAfterStart(destroyer, () -> {
      throw new IllegalStateException("after start failed");
    });

    Assert.assertTrue(thrown instanceof IllegalStateException);
    Assert.assertEquals("after start failed", thrown.getMessage());
    Assert.assertNotNull(destroyer.added);
    Assert.assertEquals(destroyer.added, destroyer.removed);
  }

  @Test
  public void testAfterStartErrorStopsProcess() throws Exception {
    Throwable thrown = startWithFailingAfterStart(new MockProcessDestroyer(), () -> {
      throw new AssertionError("after start failed");
    });

    Assert.assertTrue(thrown instanceof AssertionError);
    Assert.assertEquals("after start failed", thrown.getMessage());
  }

  /**
   * Starts a process with a listener that fails in afterStart, checks that the process is stopped
   * and that the listener is notified, and returns the failure that the caller got.
   */
  private Throwable startWithFailingAfterStart(MockProcessDestroyer destroyer, final Runnable failure)
      throws Exception {
    final List<String> events = new ArrayList<String>();
    final AtomicReference<Process> started = new AtomicReference<Process>();
    ProcessListener failing = new ProcessListener() {
      @Override
      public void afterStart(Process process, ProcessExecutor executor) {
        started.set(process);
        events.add("failing.afterStart");
        failure.run();
      }

      @Override
      public void afterStop(Process process) {
        events.add("failing.afterStop");
      }
    };
    Throwable thrown = null;
    try {
      new ProcessExecutor()
          .command("java", "-cp", "target/test-classes", Loop.class.getName())
          .addListener(failing)
          .destroyer(destroyer)
          .start();
    }
    catch (RuntimeException | Error e) {
      thrown = e;
    }
    try {
      Assert.assertNotNull("A failure was expected", thrown);
      Assert.assertNotNull(started.get());
      Assert.assertTrue("The process must be stopped", started.get().waitFor(5, TimeUnit.SECONDS));
      Assert.assertEquals(Arrays.asList("failing.afterStart", "failing.afterStop"), events);
    }
    finally {
      if (started.get() != null) {
        started.get().destroy();
      }
    }
    return thrown;
  }

  private static class ProcessListenerThrowImpl extends ProcessListenerImpl {

    @Override
    public void afterFinish(Process process, ProcessResult result) {
      super.afterFinish(process, result);

      if (result.getOutput().getString().contains("openjdk version") || result.getOutput().getString().contains("java version")) {
        throw new InvalidOutputException("Test", result);
      }
    }
  }
}
