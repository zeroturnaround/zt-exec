package org.zeroturnaround.exec.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Assert;
import org.junit.Test;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.listener.ProcessListener;

public class ProcessExecutorSubmitFailureTest {

  private interface Run {
    void run(ProcessExecutor executor) throws Exception;
  }

  private static ProcessExecutor failingSubmit() {
    return new ProcessExecutor() {
      @Override
      protected <T> Future<T> invokeSubmit(ExecutorService executor, Callable<T> task) {
        throw new IllegalStateException("task not scheduled");
      }
    };
  }

  private static ProcessExecutor failingExecutor() {
    return new ProcessExecutor() {
      @Override
      protected ExecutorService newExecutor(String processName) {
        throw new IllegalStateException("task not scheduled");
      }
    };
  }

  @Test
  public void testStartStopsProcessWhenSubmitFails() throws Exception {
    assertStopsProcess(failingSubmit(), executor -> executor.start());
  }

  @Test
  public void testStartStopsProcessWhenExecutorFails() throws Exception {
    assertStopsProcess(failingExecutor(), executor -> executor.start());
  }

  @Test
  public void testExecuteWithTimeoutStopsProcessWhenSubmitFails() throws Exception {
    assertStopsProcess(failingSubmit(), executor -> executor.timeout(1, TimeUnit.MINUTES).execute());
  }

  @Test
  public void testExecuteWithTimeoutStopsProcessWhenExecutorFails() throws Exception {
    assertStopsProcess(failingExecutor(), executor -> executor.timeout(1, TimeUnit.MINUTES).execute());
  }

  /**
   * Runs a long process with an executor that cannot schedule the task and checks that
   * the failure reaches the caller, the process is stopped
   * and the listener is notified of the start and the stop.
   */
  private void assertStopsProcess(ProcessExecutor executor, Run run) throws Exception {
    final List<String> events = new ArrayList<String>();
    final AtomicReference<Process> started = new AtomicReference<Process>();
    executor.command("java", "-cp", "target/test-classes", Loop.class.getName());
    executor.addListener(new ProcessListener() {
      @Override
      public void afterStart(Process process, ProcessExecutor executor) {
        started.set(process);
        events.add("afterStart");
      }

      @Override
      public void afterStop(Process process) {
        events.add("afterStop");
      }
    });
    try {
      try {
        run.run(executor);
        Assert.fail("IllegalStateException expected");
      }
      catch (IllegalStateException e) {
        Assert.assertEquals("task not scheduled", e.getMessage());
      }
      Assert.assertNotNull(started.get());
      Assert.assertTrue("The process must be stopped", started.get().waitFor(5, TimeUnit.SECONDS));
      Assert.assertEquals(Arrays.asList("afterStart", "afterStop"), events);
    }
    finally {
      if (started.get() != null) {
        started.get().destroy();
      }
    }
  }
}
