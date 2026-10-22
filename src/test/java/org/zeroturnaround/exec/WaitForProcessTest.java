package org.zeroturnaround.exec;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Assert;
import org.junit.Test;
import org.zeroturnaround.exec.close.ProcessCloser;
import org.zeroturnaround.exec.listener.ProcessListener;
import org.zeroturnaround.exec.stop.ProcessStopper;

public class WaitForProcessTest {

  private static class FakeProcess extends Process {

    private final int exitValue;
    private final InterruptedException waitFailure;

    FakeProcess(int exitValue, InterruptedException waitFailure) {
      this.exitValue = exitValue;
      this.waitFailure = waitFailure;
    }

    @Override
    public OutputStream getOutputStream() {
      return new ByteArrayOutputStream();
    }

    @Override
    public InputStream getInputStream() {
      return new ByteArrayInputStream(new byte[0]);
    }

    @Override
    public InputStream getErrorStream() {
      return new ByteArrayInputStream(new byte[0]);
    }

    @Override
    public int waitFor() throws InterruptedException {
      if (waitFailure != null) {
        throw waitFailure;
      }
      return exitValue;
    }

    @Override
    public int exitValue() {
      return exitValue;
    }

    @Override
    public void destroy() {
    }
  }

  private static class RememberCloser implements ProcessCloser {

    private final IOException failure;
    final AtomicBoolean closed = new AtomicBoolean();

    RememberCloser(IOException failure) {
      this.failure = failure;
    }

    @Override
    public void close(Process process) throws IOException {
      closed.set(true);
      if (failure != null) {
        throw failure;
      }
    }
  }

  private static class RememberStopper implements ProcessStopper {

    private final RuntimeException failure;
    final AtomicBoolean stopped = new AtomicBoolean();

    RememberStopper(RuntimeException failure) {
      this.failure = failure;
    }

    @Override
    public void stop(Process process) {
      stopped.set(true);
      if (failure != null) {
        throw failure;
      }
    }
  }

  private static class AfterStopListener extends ProcessListener {

    private final RuntimeException failure;
    final AtomicBoolean stopped = new AtomicBoolean();

    AfterStopListener(RuntimeException failure) {
      this.failure = failure;
    }

    @Override
    public void afterStop(Process process) {
      stopped.set(true);
      if (failure != null) {
        throw failure;
      }
    }
  }

  private static WaitForProcess newWaitForProcess(Process process, Set<Integer> allowedExitValues,
      ProcessStopper stopper, ProcessCloser closer, ProcessListener listener) {
    ProcessAttributes attributes = new ProcessAttributes(
        Collections.singletonList("fake"), null, Collections.<String, String>emptyMap(), allowedExitValues);
    return new WaitForProcess(process, attributes, stopper, closer, null, listener, MessageLoggers.NOP);
  }

  @Test
  public void shouldReturnExitValueAndCloseAndNotifyAfterStop() throws Exception {
    RememberStopper stopper = new RememberStopper(null);
    RememberCloser closer = new RememberCloser(null);
    AfterStopListener listener = new AfterStopListener(null);

    FakeProcess process = new FakeProcess(3, null);
    WaitForProcess waitForProcess = newWaitForProcess(process, null, stopper, closer, listener);

    ProcessResult result = waitForProcess.call();

    Assert.assertEquals(3, result.getExitValue());
    Assert.assertFalse(stopper.stopped.get());
    Assert.assertTrue(closer.closed.get());
    Assert.assertTrue(listener.stopped.get());
  }

  @Test
  public void shouldKeepInterruptWhenClosingFailsAfterInterrupt() {
    InterruptedException interrupt = new InterruptedException("interrupted");
    IOException closeFailure = new IOException("close failed");
    RememberStopper stopper = new RememberStopper(null);
    RememberCloser closer = new RememberCloser(closeFailure);
    AfterStopListener listener = new AfterStopListener(null);

    try {
      newWaitForProcess(new FakeProcess(0, interrupt), null, stopper, closer, listener).call();
      Assert.fail("Was expecting an exception!");
    }
    catch (Exception expected) {
      Assert.assertSame(interrupt, expected);
      Assert.assertArrayEquals(new Throwable[] { closeFailure }, expected.getSuppressed());
    }

    Assert.assertTrue(stopper.stopped.get());
    Assert.assertTrue(closer.closed.get());
    Assert.assertTrue(listener.stopped.get());
  }

  @Test
  public void shouldCloseWhenStoppingFailsAfterInterrupt() {
    InterruptedException interrupt = new InterruptedException("interrupted");
    RuntimeException stopFailure = new IllegalStateException("stop failed");
    RememberStopper stopper = new RememberStopper(stopFailure);
    RememberCloser closer = new RememberCloser(null);
    AfterStopListener listener = new AfterStopListener(null);

    try {
      newWaitForProcess(new FakeProcess(0, interrupt), null, stopper, closer, listener).call();
      Assert.fail("Was expecting an exception!");
    }
    catch (Exception expected) {
      Assert.assertSame(interrupt, expected);
      Assert.assertArrayEquals(new Throwable[] { stopFailure }, expected.getSuppressed());
    }

    Assert.assertTrue(closer.closed.get());
    Assert.assertTrue(listener.stopped.get());
  }

  @Test
  public void shouldKeepInvalidExitFailureWhenAfterStopFails() {
    RuntimeException afterStopFailure = new IllegalStateException("after stop failed");
    AfterStopListener listener = new AfterStopListener(afterStopFailure);

    try {
      WaitForProcess waitForProcess = newWaitForProcess(
          new FakeProcess(1, null), Collections.singleton(0),
          new RememberStopper(null), new RememberCloser(null), listener);
      waitForProcess.call();
      Assert.fail("Was expecting an exception!");
    }
    catch (Exception expected) {
      Assert.assertTrue(expected instanceof InvalidExitValueException);
      Assert.assertArrayEquals(new Throwable[] { afterStopFailure }, expected.getSuppressed());
    }

    Assert.assertTrue(listener.stopped.get());
  }

  @Test
  public void shouldClearWorkerThreadWhenAfterStopFails() throws Exception {
    RuntimeException afterStopFailure = new IllegalStateException("after stop failed");
    AfterStopListener listener = new AfterStopListener(afterStopFailure);
    WaitForProcess waitForProcess = newWaitForProcess(
        new FakeProcess(0, null), null, new RememberStopper(null), new RememberCloser(null), listener);

    try {
      waitForProcess.call();
      Assert.fail("Was expecting an exception!");
    }
    catch (IllegalStateException expected) {
      Assert.assertSame(afterStopFailure, expected);
    }

    Assert.assertNull(waitForProcess.getStackTrace());
  }

  @Test
  public void shouldStopCloseAndNotifyWhenAborted() {
    RememberStopper stopper = new RememberStopper(null);
    RememberCloser closer = new RememberCloser(null);
    AfterStopListener listener = new AfterStopListener(null);
    FakeProcess process = new FakeProcess(0, null);
    WaitForProcess waitForProcess = newWaitForProcess(process, null, stopper, closer, listener);
    IllegalStateException failure = new IllegalStateException("start failed");

    waitForProcess.abort(failure);

    Assert.assertTrue(stopper.stopped.get());
    Assert.assertTrue(closer.closed.get());
    Assert.assertTrue(listener.stopped.get());
    Assert.assertEquals(0, failure.getSuppressed().length);
  }

  @Test
  public void shouldAddFailuresToAbortFailureAsSuppressed() {
    RuntimeException stopFailure = new IllegalStateException("stop failed");
    IOException closeFailure = new IOException("close failed");
    RuntimeException afterStopFailure = new IllegalStateException("after stop failed");
    RememberStopper stopper = new RememberStopper(stopFailure);
    RememberCloser closer = new RememberCloser(closeFailure);
    AfterStopListener listener = new AfterStopListener(afterStopFailure);
    FakeProcess process = new FakeProcess(0, null);
    WaitForProcess waitForProcess = newWaitForProcess(process, null, stopper, closer, listener);
    IllegalStateException failure = new IllegalStateException("start failed");

    waitForProcess.abort(failure);

    Assert.assertArrayEquals(
        new Throwable[] { stopFailure, closeFailure, afterStopFailure }, failure.getSuppressed());
    Assert.assertTrue(closer.closed.get());
    Assert.assertTrue(listener.stopped.get());
  }

  @Test
  public void shouldKeepInterruptFlagWhenClosingIsInterruptedAfterAbort() {
    InterruptedException interrupt = new InterruptedException("interrupted");
    ProcessCloser closer = process -> {
      throw interrupt;
    };
    AfterStopListener listener = new AfterStopListener(null);
    WaitForProcess waitForProcess = newWaitForProcess(
        new FakeProcess(0, null), null, new RememberStopper(null), closer, listener);
    IllegalStateException failure = new IllegalStateException("start failed");

    try {
      waitForProcess.abort(failure);

      Assert.assertTrue(Thread.currentThread().isInterrupted());
      Assert.assertArrayEquals(new Throwable[] { interrupt }, failure.getSuppressed());
      Assert.assertTrue(listener.stopped.get());
    }
    finally {
      Thread.interrupted();
    }
  }
}
