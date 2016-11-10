package org.zeroturnaround.exec.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.SystemUtils;
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

		ProcessExecutor exec = new ProcessExecutor("java", "-cp", "target/test-classes", PrintInputToOutput.class.getName());
		exec.redirectInput(bais).redirectOutput(baos);

		exec.execute();
		Assert.assertEquals(str, baos.toString());
	}

	@Test
	public void testRedirectPipedInputStream() throws Exception {
		// Setup InputStream that will block on a read()
		PipedOutputStream pos = new PipedOutputStream();
		PipedInputStream pis = new PipedInputStream(pos);

		ProcessExecutor exec = new ProcessExecutor("java", "-cp", "target/test-classes", PrintArguments.class.getName());
		exec.redirectInput(pis);
		StartedProcess startedProcess = exec.start();
		// Assert that we don't get a TimeoutException
		startedProcess.getFuture().get(5, TimeUnit.SECONDS);
	}

	@Test
	public void testDataIsFlushedToProcessWithANonEndingInputStream() throws Exception {
		String str = "Tere Minu Uus vihik " + System.nanoTime();

		// Setup InputStream that will block on a read()
		PipedOutputStream pos = new PipedOutputStream();
		PipedInputStream pis = new PipedInputStream(pos);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		ProcessExecutor exec = new ProcessExecutor("java", "-cp", "target/test-classes", PrintInputToOutput.class.getName());
		exec.redirectInput(pis).redirectOutput(baos);
		StartedProcess startedProcess = exec.start();
		pos.write(str.getBytes());
		// PrintInputToOutput must process 3 lines to terminate properly
		pos.write(SystemUtils.LINE_SEPARATOR.getBytes());
		pos.write(SystemUtils.LINE_SEPARATOR.getBytes());
		pos.write(SystemUtils.LINE_SEPARATOR.getBytes());

		// Assert that we don't get a TimeoutException
		startedProcess.getFuture().get(5, TimeUnit.SECONDS);
		Assert.assertEquals(str, baos.toString());
	}

	@Test
	public void inputWillPreventCloseWhenProcessStops() throws Exception {
		// Setup InputStream that will block on a read()
		PipedOutputStream pos = new PipedOutputStream();
		PipedInputStream pis = new PipedInputStream(pos) {
			@Override
			public synchronized int read(byte[] b, int off, int len) throws IOException {
				// simulate an InputStream which handles interruption transparently within
				// (there are such InputStream implementations out there)
				while (true) {
					try {
						return super.read(b, off, len);
					} catch (InterruptedIOException e) {
						// PipedInputStream re-throws InterruptedException as part of interruption as PipedInputStream
						// ignore, try again 
					}
				}
			}
		};
		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		ProcessExecutor exec = new ProcessExecutor("java", "-cp", "target/test-classes", YesOrNoInput.class.getName());
		exec.redirectInput(pis).redirectOutput(baos);

		StartedProcess startedProcess = exec.start();
		pos.write('n');
		pos.write(SystemUtils.LINE_SEPARATOR.getBytes());

		ProcessResult result = startedProcess.getFuture().get(5L, TimeUnit.SECONDS);

		assertNotNull(result);
		assertEquals(1, result.getExitValue());
	}
}
