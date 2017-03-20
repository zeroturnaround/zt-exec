package org.zeroturnaround.exec.test;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.zeroturnaround.exec.stream.LogOutputStream;

public class LogOutputStreamTest {

	private void testLogOutputStream(String multiLineString, String... expectedLines) throws UnsupportedEncodingException, IOException {
		final List<String> processedLines = new ArrayList<String>();
		LogOutputStream logOutputStream = new LogOutputStream() {

			@Override
			protected void processLine(String line) {
				processedLines.add(line);
			}
		};
		try {
			logOutputStream.write(multiLineString.getBytes("UTF-8"));
		} finally {
			logOutputStream.close();
		}
		Assert.assertEquals(Arrays.asList(expectedLines), processedLines);
	}

	@Test
	public void testSimple() throws UnsupportedEncodingException, IOException {
		testLogOutputStream("foo", "foo");
	}

	@Test
	public void testNewLine() throws UnsupportedEncodingException, IOException {
		testLogOutputStream("foo\nbar", "foo", "bar");
	}

	@Test
	public void testNewLineWithMultipleLines() throws UnsupportedEncodingException, IOException {
		testLogOutputStream("foo1\nbar1\nfoo2\nbar2", "foo1", "bar1", "foo2", "bar2");
	}

	@Test
	public void testCarriageReturn() throws UnsupportedEncodingException, IOException {
		testLogOutputStream("foo\rbar", "foo", "bar");
	}

	@Test
	public void testCarriageReturnWithMultipleLines() throws UnsupportedEncodingException, IOException {
		testLogOutputStream("foo1\rbar1\rfoo2\rbar2", "foo1", "bar1", "foo2", "bar2");
	}

	@Test
	public void testCarriageReturnAndNewLine() throws UnsupportedEncodingException, IOException {
		testLogOutputStream("foo\r\nbar", "foo", "bar");
	}

	@Test
	public void testCarriageReturnAndNewLineWithMultipleLines() throws UnsupportedEncodingException, IOException {
		testLogOutputStream("foo1\r\nbar1\r\nfoo2\r\nbar2", "foo1", "bar1", "foo2", "bar2");
	}

	@Test
	public void testTwoNewLines() throws UnsupportedEncodingException, IOException {
		testLogOutputStream("foo\n\nbar", "foo", "bar");
	}

	@Test
	public void testNewLineAtTheEnd() throws UnsupportedEncodingException, IOException {
		testLogOutputStream("foo\n", "foo");
	}

}
