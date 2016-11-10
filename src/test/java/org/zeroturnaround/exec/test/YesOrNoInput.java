package org.zeroturnaround.exec.test;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class YesOrNoInput {
  public static void main(String[] args) throws Exception {
    InputStreamReader isr = new InputStreamReader(System.in);
    BufferedReader br = new BufferedReader(isr);

    
    System.out.print("y/n: ");
    String line = br.readLine();
    
    Thread.sleep(500L); // simulate some processing
    
    if("y".equals(line)) {
    	System.exit(0);
    } else if("n".equals(line)) {
    	System.exit(1);
    }
	System.exit(3);
  }
}
