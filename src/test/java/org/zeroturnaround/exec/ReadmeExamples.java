package org.zeroturnaround.exec;

import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.LoggerFactory;
import org.zeroturnaround.exec.stream.slf4j.Slf4jStream;

/**
 * Examples of the readme.
 */
class ReadmeExamples {

  void justExecute() throws Exception {
    new ProcessExecutor().command("java", "-version").execute();
  }

  int getExitCode() throws Exception {
    int exit = new ProcessExecutor().command("java", "-version")
        .execute().getExitValue();
    return exit;
  }

  String getOutput() throws Exception {
    String output = new ProcessExecutor().command("java", "-version")
        .readOutput(true).execute()
        .outputUTF8();
    return output;
  }

  void pumpOutputToLogger() throws Exception {
    new ProcessExecutor().command("java", "-version")
    .redirectOutput(Slf4jStream.of(LoggerFactory.getLogger(getClass().getName() + ".MyProcess")).asInfo()).execute();
  }

  void pumpOutputToLoggerShorter() throws Exception {
    new ProcessExecutor().command("java", "-version")
    .redirectOutput(Slf4jStream.of("MyProcess").asInfo()).execute();
  }

  void pumpOutputToLoggerOfCaller() throws Exception {
    new ProcessExecutor().command("java", "-version")
    .redirectOutput(Slf4jStream.ofCaller().asInfo()).execute();
  }

  String pumpOutputToLoggerAndGetOutput() throws Exception {
    String output = new ProcessExecutor().command("java", "-version")
        .redirectOutput(Slf4jStream.of(getClass()).asInfo())
        .readOutput(true).execute().outputUTF8();
    return output;
  }

  String pumpErrorToLoggerAndGetOutput() throws Exception {
    String output = new ProcessExecutor().command("java", "-version")
        .redirectError(Slf4jStream.of(getClass()).asInfo())
        .readOutput(true).execute()
        .outputUTF8();
    return output;
  }

  void executeWithTimeout() throws Exception {
    try {
      new ProcessExecutor().command("java", "-version")
            .timeout(60, TimeUnit.SECONDS).execute();
    }
    catch (TimeoutException e) {
      // process is automatically destroyed
    }
  }

  void pumpOutputToStream(OutputStream out) throws Exception {
    new ProcessExecutor().command("java", "-version")
          .redirectOutput(out).execute();
  }

  void destroyProcessOnJvmExit() throws Exception {
    new ProcessExecutor().command("java", "-version").destroyOnExit().execute();
  }

  void executeWithEnvironmentVariable() throws Exception {
    new ProcessExecutor().command("java", "-version")
    .environment("foo", "bar")
    .execute();
  }

  void executeWithEnvironment(Map<String, String> env) throws Exception {
    new ProcessExecutor().command("java", "-version")
    .environment(env)
    .execute();
  }

  void checkExitCode() throws Exception {
    try {
      new ProcessExecutor().command("java", "-version")
            .exitValues(3).execute();
    }
    catch (InvalidExitValueException e) {
      System.out.println("Process exited with " + e.getExitValue());
    }
  }

  void checkExitCodeAndGetOutput() throws Exception {
    String output;
    boolean success = false;
    try {
      output = new ProcessExecutor().command("java", "-version")
                    .readOutput(true).exitValues(3)
                    .execute().outputUTF8();
      success = true;
    }
    catch (InvalidExitValueException e) {
      System.out.println("Process exited with " + e.getExitValue());
      output = e.getResult().outputUTF8();
    }
  }

  void startInBackground() throws Exception {
    Future<ProcessResult> future = new ProcessExecutor()
          .command("java", "-version")
          .start().getFuture();
    //do some stuff
    future.get(60, TimeUnit.SECONDS);
  }

  String startInBackgroundAndGetOutput() throws Exception {
    Future<ProcessResult> future = new ProcessExecutor()
          .command("java", "-version")
          .readOutput(true)
          .start().getFuture();
    //do some stuff
    String output = future.get(60, TimeUnit.SECONDS).outputUTF8();
    return output;
  }

}
