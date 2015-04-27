package org.zeroturnaround.exec;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

public class ProcessOutputTest {

  @Test(expected=NullPointerException.class)
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
  public void testCarriageReturn() {
    Assert.assertEquals(Arrays.asList("foo", "bar"), ProcessOutput.getLinesFrom("foo\rbar"));
  }

  @Test
  public void testCarriageReturnAndNewLine() {
    Assert.assertEquals(Arrays.asList("foo", "bar"), ProcessOutput.getLinesFrom("foo\r\nbar"));
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
