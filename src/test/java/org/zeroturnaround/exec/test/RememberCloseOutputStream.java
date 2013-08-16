package org.zeroturnaround.exec.test;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class RememberCloseOutputStream extends FilterOutputStream {

  private volatile boolean closed;

  public RememberCloseOutputStream(OutputStream out) {
    super(out);
  }

  @Override
  public void close() throws IOException {
    closed = true;
    super.close();
  }

  public boolean isClosed() {
    return closed;
  }

}
