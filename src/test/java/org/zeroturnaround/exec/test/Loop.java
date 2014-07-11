package org.zeroturnaround.exec.test;

import java.io.PrintStream;

class Loop {

  private static final long INTERVAL = 1000;
  private static final long COUNT = 10;

  public static void main(String[] args) throws Exception {
    PrintStream out = System.out;
    out.println("Started");
    for (int i = 0; i < COUNT; i++) {
      out.println(i);
      Thread.sleep(INTERVAL);
    }
    out.println("Finished");
  }

}
