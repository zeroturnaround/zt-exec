package org.zeroturnaround.exec;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

public class ProcessOutputTest {

	@Test(expected = NullPointerException.class)
	public void testNull() {
		ProcessOutput.getLinesFrom(null);
	}

	@Test
	public void testSimple() {
		Assert.assertEquals(Arrays.asList("foo"), ProcessOutput.getLinesFrom("foo"));
	}

	@Test
	public void testNewLine() {
		Assert.assertEquals(Arrays.asList("foo", "bar"), ProcessOutput.getLinesFrom("foo\nbar"));
	}

	@Test
	public void testNewLineWithMultipleLines() {
		Assert.assertEquals(Arrays.asList("foo1", "bar1", "foo2", "bar2"), ProcessOutput.getLinesFrom("foo1\nbar1\nfoo2\nbar2"));
	}

	@Test
	public void testCarriageReturn() {
		Assert.assertEquals(Arrays.asList("foo", "bar"), ProcessOutput.getLinesFrom("foo\rbar"));
	}

	@Test
	public void testCarriageReturnWithMultipleLines() {
		Assert.assertEquals(Arrays.asList("foo1", "bar1", "foo2", "bar2"), ProcessOutput.getLinesFrom("foo1\rbar1\rfoo2\rbar2"));
	}

	@Test
	public void testCarriageReturnAndNewLine() {
		Assert.assertEquals(Arrays.asList("foo", "bar"), ProcessOutput.getLinesFrom("foo\r\nbar"));
	}

	@Test
	public void testCarriageReturnAndNewLineWithMultipleLines() {
		Assert.assertEquals(Arrays.asList("foo1", "bar1", "foo2", "bar2"), ProcessOutput.getLinesFrom("foo1\r\nbar1\r\nfoo2\r\nbar2"));
	}

	@Test
	public void testTwoNewLines() {
		Assert.assertEquals(Arrays.asList("foo", "bar"), ProcessOutput.getLinesFrom("foo\n\nbar"));
	}

	@Test
	public void testNewLineAtTheEnd() {
		Assert.assertEquals(Arrays.asList("foo"), ProcessOutput.getLinesFrom("foo\n"));
	}

}
