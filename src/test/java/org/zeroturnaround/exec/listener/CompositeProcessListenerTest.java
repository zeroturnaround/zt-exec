package org.zeroturnaround.exec.listener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;

public class CompositeProcessListenerTest {

  private static class RememberListener extends ProcessListener {

    private final String name;
    private final List<String> events;
    private final RuntimeException failure;

    RememberListener(String name, List<String> events, RuntimeException failure) {
      this.name = name;
      this.events = events;
      this.failure = failure;
    }

    @Override
    public void beforeStart(ProcessExecutor executor) {
      events.add(name + ".beforeStart");
      if (failure != null) {
        throw failure;
      }
    }

    @Override
    public void afterStart(Process process, ProcessExecutor executor) {
      events.add(name + ".afterStart");
      if (failure != null) {
        throw failure;
      }
    }

    @Override
    public void afterFinish(Process process, ProcessResult result) {
      events.add(name + ".afterFinish");
      if (failure != null) {
        throw failure;
      }
    }

    @Override
    public void afterStop(Process process) {
      events.add(name + ".afterStop");
      if (failure != null) {
        throw failure;
      }
    }
  }

  @Test
  public void shouldNotifyAllListenersAfterStop() {
    List<String> events = new ArrayList<String>();
    CompositeProcessListener composite = new CompositeProcessListener();
    composite.add(new RememberListener("first", events, null));
    composite.add(new RememberListener("second", events, null));

    composite.afterStop(null);

    Assert.assertEquals(Arrays.asList("first.afterStop", "second.afterStop"), events);
  }

  @Test
  public void shouldNotifyAllListenersAfterStopWhenOneFails() {
    List<String> events = new ArrayList<String>();
    RuntimeException firstFailure = new IllegalStateException("first failed");
    RuntimeException thirdFailure = new IllegalStateException("third failed");
    CompositeProcessListener composite = new CompositeProcessListener();
    composite.add(new RememberListener("first", events, firstFailure));
    composite.add(new RememberListener("second", events, null));
    composite.add(new RememberListener("third", events, thirdFailure));

    try {
      composite.afterStop(null);
      Assert.fail("Was expecting an exception!");
    }
    catch (IllegalStateException expected) {
      Assert.assertSame(firstFailure, expected);
      Assert.assertArrayEquals(new Throwable[] { thirdFailure }, expected.getSuppressed());
    }

    Assert.assertEquals(Arrays.asList("first.afterStop", "second.afterStop", "third.afterStop"), events);
  }

  @Test
  public void shouldThrowFailureOnceWhenSameFailureIsThrownTwiceAfterStop() {
    List<String> events = new ArrayList<String>();
    RuntimeException failure = new IllegalStateException("failed");
    CompositeProcessListener composite = new CompositeProcessListener();
    composite.add(new RememberListener("first", events, failure));
    composite.add(new RememberListener("second", events, failure));

    try {
      composite.afterStop(null);
      Assert.fail("Was expecting an exception!");
    }
    catch (IllegalStateException expected) {
      Assert.assertSame(failure, expected);
      Assert.assertEquals(0, expected.getSuppressed().length);
    }

    Assert.assertEquals(Arrays.asList("first.afterStop", "second.afterStop"), events);
  }

  @Test
  public void shouldStopNotifyingListenersAfterFinishWhenOneFails() {
    List<String> events = new ArrayList<String>();
    RuntimeException failure = new IllegalStateException("rejected the result");
    CompositeProcessListener composite = new CompositeProcessListener();
    composite.add(new RememberListener("first", events, failure));
    composite.add(new RememberListener("second", events, null));

    try {
      composite.afterFinish(null, null);
      Assert.fail("Was expecting an exception!");
    }
    catch (IllegalStateException expected) {
      Assert.assertSame(failure, expected);
    }

    Assert.assertEquals(Arrays.asList("first.afterFinish"), events);
  }

  @Test
  public void shouldStopNotifyingListenersBeforeStartWhenOneFails() {
    List<String> events = new ArrayList<String>();
    RuntimeException failure = new IllegalStateException("vetoed the start");
    CompositeProcessListener composite = new CompositeProcessListener();
    composite.add(new RememberListener("first", events, failure));
    composite.add(new RememberListener("second", events, null));

    try {
      composite.beforeStart(null);
      Assert.fail("Was expecting an exception!");
    }
    catch (IllegalStateException expected) {
      Assert.assertSame(failure, expected);
    }

    Assert.assertEquals(Arrays.asList("first.beforeStart"), events);
  }

  @Test
  public void shouldNotifyAllListenersAfterStartWhenOneFails() {
    List<String> events = new ArrayList<String>();
    RuntimeException firstFailure = new IllegalStateException("first failed");
    RuntimeException thirdFailure = new IllegalStateException("third failed");
    CompositeProcessListener composite = new CompositeProcessListener();
    composite.add(new RememberListener("first", events, firstFailure));
    composite.add(new RememberListener("second", events, null));
    composite.add(new RememberListener("third", events, thirdFailure));

    try {
      composite.afterStart(null, null);
      Assert.fail("Was expecting an exception!");
    }
    catch (IllegalStateException expected) {
      Assert.assertSame(firstFailure, expected);
      Assert.assertArrayEquals(new Throwable[] { thirdFailure }, expected.getSuppressed());
    }

    Assert.assertEquals(Arrays.asList("first.afterStart", "second.afterStart", "third.afterStart"), events);
  }

  @Test
  public void shouldThrowErrorAtOnceAfterStart() {
    List<String> events = new ArrayList<String>();
    CompositeProcessListener composite = new CompositeProcessListener();
    composite.add(new ProcessListener() {
      @Override
      public void afterStart(Process process, ProcessExecutor executor) {
        events.add("first.afterStart");
        throw new AssertionError("error");
      }
    });
    composite.add(new RememberListener("second", events, null));

    try {
      composite.afterStart(null, null);
      Assert.fail("Was expecting an error!");
    }
    catch (AssertionError expected) {
      Assert.assertEquals("error", expected.getMessage());
    }

    Assert.assertEquals(Arrays.asList("first.afterStart"), events);
  }
}
