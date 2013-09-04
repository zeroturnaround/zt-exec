package org.zeroturnaround.exec.test;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class PrintInputToOutput {
  public static void main(String[] args) throws Exception {
    InputStreamReader isr = new InputStreamReader(System.in);
    BufferedReader br = new BufferedReader(isr);

    String line = null;
    int count = 0;
    while ((line = br.readLine()) != null) {
      System.out.print(line);
      count++;
      if (count == 3)
        break;
    }
  }
}
