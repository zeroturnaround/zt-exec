/*
 * Copyright (C) 2026 Neeme Praks
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
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.MDC;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.stream.PumpStreamHandler;
import org.zeroturnaround.exec.stream.PumpThreadSpec;

/**
 * Tests how the pump threads are created: what they are named, that they stay daemons and
 * that they keep the logging context of the caller.
 *
 * @see PumpStreamHandler#getThreadName(String)
 * @see PumpStreamHandler#newThread(PumpThreadSpec)
 */
public class PumpThreadTest {

  /**
   * Records the name of the thread that writes to it.
   */
  private static class ThreadNameCapturingOutputStream extends OutputStream {

    private final AtomicReference<String> threadName = new AtomicReference<String>();

    @Override
    public void write(int b) {
      threadName.compareAndSet(null, Thread.currentThread().getName());
    }

    public String getThreadName() {
      return threadName.get();
    }
  }

  /**
   * Records the name of the thread that reads from it, and is immediately exhausted.
   */
  private static class ThreadNameCapturingInputStream extends InputStream {

    private final AtomicReference<String> threadName = new AtomicReference<String>();

    @Override
    public int read() {
      threadName.compareAndSet(null, Thread.currentThread().getName());
      return -1;
    }

    public String getThreadName() {
      return threadName.get();
    }
  }

  /**
   * Records the logging context of the thread that writes to it.
   */
  private static class MdcCapturingOutputStream extends OutputStream {

    private final String key;

    private final AtomicReference<String> value = new AtomicReference<String>();

    public MdcCapturingOutputStream(String key) {
      this.key = key;
    }

    @Override
    public void write(int b) {
      String current = MDC.get(key);
      if (current != null) {
        value.compareAndSet(null, current);
      }
    }

    public String getValue() {
      return value.get();
    }
  }

  /**
   * Gives the test a look at the pump threads, which are otherwise only visible to
   * subclasses, without needing a process to produce them.
   */
  private static class ThreadExposingHandler extends PumpStreamHandler {

    public ThreadExposingHandler(InputStream input) {
      super(new ByteArrayOutputStream(), new ByteArrayOutputStream(), input);
    }

    public Thread getOutputThread() {
      return outputThread;
    }

    public Thread getErrorThread() {
      return errorThread;
    }

    public Thread getInputThread() {
      return inputThread;
    }
  }

  @Test
  public void testDefaultNamesTellTheRolesApart() throws Exception {
    ThreadNameCapturingOutputStream out = new ThreadNameCapturingOutputStream();
    ThreadNameCapturingOutputStream err = new ThreadNameCapturingOutputStream();
    helloWorld().streams(new PumpStreamHandler(out, err)).redirectErrorStream(false).execute();
    Assert.assertTrue("unexpected output thread name: " + out.getThreadName(),
        out.getThreadName().startsWith("zt-exec-stdout-"));
    Assert.assertTrue("unexpected error thread name: " + err.getThreadName(),
        err.getThreadName().startsWith("zt-exec-stderr-"));
  }

  @Test
  public void testDefaultNamesAreUnique() throws Exception {
    ThreadNameCapturingOutputStream out = new ThreadNameCapturingOutputStream();
    ThreadNameCapturingOutputStream err = new ThreadNameCapturingOutputStream();
    helloWorld().streams(new PumpStreamHandler(out, err)).redirectErrorStream(false).execute();
    Assert.assertNotEquals(out.getThreadName(), err.getThreadName());
  }

  @Test
  public void testGetThreadNameOverrideIsUsed() throws Exception {
    ThreadNameCapturingOutputStream out = new ThreadNameCapturingOutputStream();
    PumpStreamHandler streams = new PumpStreamHandler(out, out) {
      @Override
      protected String getThreadName(String role) {
        return "hello-world-" + role;
      }
    };
    helloWorld().streams(streams).execute();
    Assert.assertEquals("hello-world-stdout", out.getThreadName());
  }

  /**
   * A name assigned by an overridden {@link PumpStreamHandler#newThread(Runnable)} is left
   * alone, since nothing renames the thread after it is constructed.
   */
  @Test
  public void testNewThreadOverrideKeepsItsName() throws Exception {
    ThreadNameCapturingOutputStream out = new ThreadNameCapturingOutputStream();
    PumpStreamHandler streams = new PumpStreamHandler(out, out) {
      @Override
      protected Thread newThread(Runnable task) {
        Thread thread = super.newThread(task);
        thread.setName("named-by-new-thread");
        return thread;
      }
    };
    helloWorld().streams(streams).execute();
    Assert.assertEquals("named-by-new-thread", out.getThreadName());
  }

  @Test
  public void testNewThreadSpecOverrideIsUsed() throws Exception {
    ThreadNameCapturingOutputStream out = new ThreadNameCapturingOutputStream();
    final AtomicReference<PumpThreadSpec> received = new AtomicReference<PumpThreadSpec>();
    PumpStreamHandler streams = new PumpStreamHandler(out, out) {
      @Override
      protected Thread newThread(PumpThreadSpec spec) {
        received.set(spec);
        Thread thread = new Thread(spec.getTask(), "named-by-spec-override");
        thread.setDaemon(true);
        return thread;
      }
    };
    helloWorld().streams(streams).execute();
    Assert.assertEquals("named-by-spec-override", out.getThreadName());
    Assert.assertEquals("stdout", received.get().getRole());
    Assert.assertTrue("unexpected name in spec: " + received.get().getName(),
        received.get().getName().startsWith("zt-exec-stdout-"));
    Assert.assertNotNull(received.get().getTask());
  }

  /**
   * Overriding a <code>createPump</code> method does not lose the role of the thread.
   */
  @Test
  public void testOverriddenCreatePumpKeepsTheRole() throws Exception {
    ThreadNameCapturingOutputStream out = new ThreadNameCapturingOutputStream();
    PumpStreamHandler streams = new PumpStreamHandler(out, out) {
      @Override
      protected Thread createPump(InputStream is, OutputStream os) {
        return createPump(is, os, true, true);
      }
    };
    helloWorld().streams(streams).execute();
    Assert.assertTrue("unexpected thread name: " + out.getThreadName(),
        out.getThreadName().startsWith("zt-exec-stdout-"));
  }

  /**
   * A pump serving none of the streams the handler was given has no role to name it after.
   */
  @Test
  public void testPumpServingAnotherStreamGetsGenericName() throws Exception {
    final ThreadNameCapturingOutputStream other = new ThreadNameCapturingOutputStream();
    ThreadNameCapturingOutputStream out = new ThreadNameCapturingOutputStream();
    PumpStreamHandler streams = new PumpStreamHandler(out, out) {
      @Override
      public void setProcessOutputStream(InputStream is) {
        outputThread = createPump(is, other, false, false);
      }
    };
    helloWorld().streams(streams).execute();
    Assert.assertTrue("unexpected thread name: " + other.getThreadName(),
        other.getThreadName().startsWith("zt-exec-pump-"));
  }

  /**
   * Sharing one stream between the output and the error leaves both pumps serving it as
   * the standard output.
   */
  @Test
  public void testSharedOutputAndErrorStreamSharesTheRole() throws Exception {
    ThreadNameCapturingOutputStream shared = new ThreadNameCapturingOutputStream();
    helloWorld().streams(new PumpStreamHandler(shared, shared)).redirectErrorStream(false).execute();
    Assert.assertTrue("unexpected thread name: " + shared.getThreadName(),
        shared.getThreadName().startsWith("zt-exec-stdout-"));
  }

  @Test
  public void testInputPumpIsNamedAfterStandardInput() throws Exception {
    ThreadNameCapturingInputStream input = new ThreadNameCapturingInputStream();
    ThreadNameCapturingOutputStream out = new ThreadNameCapturingOutputStream();
    helloWorld().streams(new PumpStreamHandler(out, out, input)).execute();
    Assert.assertTrue("unexpected input thread name: " + input.getThreadName(),
        input.getThreadName().startsWith("zt-exec-stdin-"));
    Assert.assertTrue("unexpected output thread name: " + out.getThreadName(),
        out.getThreadName().startsWith("zt-exec-stdout-"));
  }

  /**
   * Pumping {@link System#in} uses an {@link org.zeroturnaround.exec.stream.InputStreamPumper},
   * which pumps into the process rather than into one of the streams this handler was given.
   */
  @Test
  public void testSystemInPumpIsNamedAfterStandardInput() throws Exception {
    ThreadExposingHandler streams = new ThreadExposingHandler(System.in);
    streams.setProcessInputStream(new ByteArrayOutputStream());
    Assert.assertTrue("unexpected input thread name: " + streams.getInputThread().getName(),
        streams.getInputThread().getName().startsWith("zt-exec-stdin-"));
  }

  /**
   * The pump threads must not keep the JVM alive.
   */
  @Test
  public void testPumpThreadsAreDaemons() throws Exception {
    ThreadExposingHandler streams = new ThreadExposingHandler(new ByteArrayInputStream(new byte[0]));
    streams.setProcessOutputStream(new ByteArrayInputStream(new byte[0]));
    streams.setProcessErrorStream(new ByteArrayInputStream(new byte[0]));
    streams.setProcessInputStream(new ByteArrayOutputStream());
    Assert.assertTrue("output thread is not a daemon", streams.getOutputThread().isDaemon());
    Assert.assertTrue("error thread is not a daemon", streams.getErrorThread().isDaemon());
    Assert.assertTrue("input thread is not a daemon", streams.getInputThread().isDaemon());
  }

  /**
   * The pump threads log on behalf of the caller, so they run with its logging context.
   */
  @Test
  public void testPumpThreadKeepsTheLoggingContextOfTheCaller() throws Exception {
    MDC.put("zt-exec-test", "context-of-the-caller");
    try {
      MdcCapturingOutputStream out = new MdcCapturingOutputStream("zt-exec-test");
      helloWorld().streams(new PumpStreamHandler(out, out)).execute();
      Assert.assertEquals("context-of-the-caller", out.getValue());
    }
    finally {
      MDC.remove("zt-exec-test");
    }
  }

  private ProcessExecutor helloWorld() {
    return new ProcessExecutor("java", "-cp", "target/test-classes", HelloWorld.class.getName());
  }

}
