package org.zeroturnaround.exec.stream;

import org.junit.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class LogOutputStreamTest {
  private void writeDataTo(OutputStream out) throws IOException {
    out.write("Hello,\nworld!".getBytes());
    out.close();
  }

  @Test
  public void anonymous() throws IOException {
    final List<String> lines = new ArrayList<String>();
    writeDataTo(new LogOutputStream() {
      @Override
      protected void processLine(String line) {
        lines.add(line);
      }
    });
    assertEquals(Arrays.asList("Hello,", "world!"), lines);
  }

  @Test
  public void lambda() throws IOException {
    final List<String> lines = new ArrayList<String>();
    writeDataTo(LogOutputStream.create(new LogOutputStream.LineConsumer() {
      @Override
      public void accept(String line) {
        lines.add(line);
      }
    }));
    assertEquals(Arrays.asList("Hello,", "world!"), lines);
  }
}
