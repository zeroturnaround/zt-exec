/*
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
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Assert;
import org.junit.Test;
import org.zeroturnaround.exec.stream.PumpStreamHandler;

public class PumpStreamHandlerInterruptionTest {

  @Test public void interruptsOutputWithPendingInterrupt() throws Exception {
    checkInterruption(false, true, false, true);
  }

  @Test public void interruptsErrorWithPendingInterrupt() throws Exception {
    checkInterruption(false, false, true, true);
  }

  @Test public void interruptsBothOutputsWithPendingInterrupt() throws Exception {
    checkInterruption(false, true, true, true);
  }

  @Test public void preservesInterruptFromInputJoin() throws Exception {
    checkInterruption(true, true, false, true);
  }

  @Test public void interruptsAllStreamsWithPendingInterrupt() throws Exception {
    checkInterruption(true, true, true, true);
  }

  @Test public void interruptsOutputWhileJoining() throws Exception {
    checkInterruption(false, true, false, false);
  }

  @Test public void interruptsBothOutputsWhileJoining() throws Exception {
    checkInterruption(false, true, true, false);
  }

  @Test public void drainsBothOutputsOnNormalStop() {
    byte[] content = new byte[32768];
    Arrays.fill(content, (byte) 'x');
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ByteArrayOutputStream err = new ByteArrayOutputStream();
    PumpStreamHandler handler = new PumpStreamHandler(out, err);
    handler.setProcessOutputStream(new ByteArrayInputStream(content));
    handler.setProcessErrorStream(new ByteArrayInputStream(content));
    handler.start();
    handler.stop();
    Assert.assertArrayEquals(content, out.toByteArray());
    Assert.assertArrayEquals(content, err.toByteArray());
    Assert.assertFalse(Thread.currentThread().isInterrupted());
  }

  @Test public void preservesPendingInterruptWithoutStreams() throws Exception {
    AtomicBoolean interrupted = new AtomicBoolean();
    Thread caller = new Thread(() -> {
      Thread.currentThread().interrupt();
      new PumpStreamHandler(null, null).stop();
      interrupted.set(Thread.currentThread().isInterrupted());
    });
    caller.start();
    caller.join(5000);
    Assert.assertFalse(caller.isAlive());
    Assert.assertTrue(interrupted.get());
  }

  private void checkInterruption(boolean input, boolean output, boolean error,
      boolean pending) throws Exception {
    List<InterruptiblePipe> pipes = new ArrayList<InterruptiblePipe>();
    InterruptiblePipe in = input ? new InterruptiblePipe() : null;
    InterruptiblePipe out = output ? new InterruptiblePipe() : null;
    InterruptiblePipe err = error ? new InterruptiblePipe() : null;
    if (in != null) pipes.add(in);
    if (out != null) pipes.add(out);
    if (err != null) pipes.add(err);
    ExposedHandler handler = new ExposedHandler(in);
    if (in != null) handler.setProcessInputStream(new ByteArrayOutputStream());
    if (out != null) handler.setProcessOutputStream(out);
    if (err != null) handler.setProcessErrorStream(err);
    Thread[] pumps = handler.pumps();
    AtomicBoolean preserved = new AtomicBoolean();
    AtomicReference<Throwable> failure = new AtomicReference<Throwable>();
    CountDownLatch stopped = new CountDownLatch(1);
    Thread caller = new Thread(() -> {
      try {
        if (pending) Thread.currentThread().interrupt();
        handler.stop();
        preserved.set(Thread.currentThread().isInterrupted());
      }
      catch (Throwable t) {
        failure.set(t);
      }
      finally {
        stopped.countDown();
      }
    }, "test-stop-caller");
    try {
      handler.start();
      for (InterruptiblePipe pipe : pipes) {
        Assert.assertTrue("Pump did not enter read", pipe.reading.await(5, TimeUnit.SECONDS));
      }
      caller.start();
      if (!pending) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (caller.getState() != Thread.State.WAITING && caller.isAlive()
            && System.nanoTime() < deadline) Thread.yield();
        Assert.assertEquals("Caller did not enter join", Thread.State.WAITING, caller.getState());
        caller.interrupt();
      }
      Assert.assertTrue("stop blocked on a later pump", stopped.await(5, TimeUnit.SECONDS));
      Assert.assertNull(failure.get());
      Assert.assertTrue("Caller interrupt was swallowed", preserved.get());
      for (InterruptiblePipe pipe : pipes) {
        Assert.assertTrue("Pump did not receive interruption", pipe.interrupted.await(5, TimeUnit.SECONDS));
      }
      for (Thread pump : pumps) {
        if (pump != null) {
          pump.join(5000);
          Assert.assertFalse("Pump still running", pump.isAlive());
        }
      }
    }
    finally {
      // Also release every pipe on the broken implementation, without leaving test threads behind.
      for (InterruptiblePipe pipe : pipes) pipe.writer.close();
      if (caller.isAlive()) caller.interrupt();
      caller.join(5000);
      for (Thread pump : pumps) {
        if (pump != null) {
          pump.interrupt();
          pump.join(5000);
          Assert.assertFalse("Cleanup left a pump running", pump.isAlive());
        }
      }
      for (InterruptiblePipe pipe : pipes) pipe.close();
      Assert.assertFalse("Cleanup left caller running", caller.isAlive());
    }
  }

  private static class ExposedHandler extends PumpStreamHandler {
    ExposedHandler(PipedInputStream input) {
      super(new ByteArrayOutputStream(), new ByteArrayOutputStream(), input);
    }

    Thread[] pumps() {
      return new Thread[] { inputThread, outputThread, errorThread };
    }
  }

  private static class InterruptiblePipe extends PipedInputStream {
    final PipedOutputStream writer = new PipedOutputStream(this);
    final CountDownLatch reading = new CountDownLatch(1);
    final CountDownLatch interrupted = new CountDownLatch(1);

    InterruptiblePipe() throws IOException {
    }

    @Override public synchronized int read(byte[] bytes, int offset, int length) throws IOException {
      reading.countDown();
      try {
        return super.read(bytes, offset, length);
      }
      catch (InterruptedIOException e) {
        interrupted.countDown();
        throw e;
      }
    }
  }
}
