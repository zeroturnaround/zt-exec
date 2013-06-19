Process Executor
================

### Quick Overview

The project was launched by Rein Raudjärv to merge similar functionality from many LiveRebel sub projects into single codebase.
It's designed to be powerful but still simple to use. By only using single class org.zeroturnaround.exec.ProcessExecutor
user gets the functionality from both java.lang.ProcessBuilder and [Apache Commons Exec](http://commons.apache.org/proper/commons-exec/).

### Installation

TODO

## Background

JRE Library (Runtime.exec(), ProcessBuilder) is lacking:
1. simple stream reading (Java 7 is a bit better) - even if user doesn't need streams they still have to be consumed to avoid getting stuck 
2. setting timeout for process running
3. checking exit codes

[Apache Commons Exec](http://commons.apache.org/proper/commons-exec/) resolves them but
1. API is not user-friendly
  1. cannot use one line to execute a process other than trivial case
  2. have to explicitly create instances of other classes
  3. no quick support for reading output into String
2. no ProcessBuilder support which is available since Java 5
  1. cannot redirect error stream to output stream
  2. environemnt Map is converted into Strings
3. no distinguishing between timeout and normal error
4. no direct access to Process object
5. no java.util.concurrent.Future support
6. no logging
7. error messages are printed to System.err
In addition:
8. cannot run more than one process at a time using same executor
9. checking the timeout requires additional thread
10. LogOutputStream has dummy field 'level'
11. need to add new classs for redirecting stream to a logger

To sump up zt-exec features:

1. handling process streams (copied from Commons Exec library) (redirected to null stream by default)
2. redirecting process error to process output stream (enabled by default)
3. destroying process on VM exit (copied from Commons Exec library) (disabled by default)
4. checking process exit code (only 0 is allowed by default)
5. setting a timeout for running the process (disabled by default)
6. either waiting for the process to finish or returning a java.util.concurrent.Future
7. reading the process output stream into a buffer
8. redirecting process output to a given [SLF4J API](http://www.slf4j.org/) logger

In addition:

1. it's simple to use - process can be started with a single line of code
2. same executor can be reused before previous process has stopped.
3. standard java.util.concurrent.Future to represent an asynchronous computation
4. standard java.util.concurrent.TimeoutException is used to indicate a timeout
5. user gets direct access to java.lang.Process object
6. executor logs its events via [SLF4J API](http://www.slf4j.org/) not System.out or System.err

## Examples

#### Running a command (output is pumped to NullOutputStream)
```java
new ProcessExecutor().command("java", "-version").execute();
```

#### Running a command and returning its exit code (by default only 0 is allowed, output is pumped to NullOutputStream)
```java
int exit = new ProcessExecutor().command("java", "-version").exitValueAny().execute().exitValue();
```

#### Running a command and returning its output as UTF-8 String
```java
String output = new ProcessExecutor().command("java", "-version").readOutput(true).execute().outputUTF8();    
```

#### Running a command and pumping its output to a logger (with info level)
```java
new ProcessExecutor().command("java", "-version").info(LoggerFactory.getLogger(getClass())).execute();
```

#### Running a command and pumping its output to a logger (with info level) and also returning its output as UTF-8 String
```java
String output = new ProcessExecutor().command("java", "-version").info(LoggerFactory.getLogger(getClass())).readOutput(true).execute().outputUTF8();
```

#### Running a command and pumping its error stream to a logger (with info level) but returning its output stream as UTF-8 String
```java
String output = new ProcessExecutor().command("java", "-version").redirectErrorStream(false).redirectErrorAsInfo(LoggerFactory.getLogger(getClass())).readOutput(true).execute().outputUTF8();
```

#### Running a command with a timeout of 60 seconds (output is pumped to NullOutputStream)
```java
new ProcessExecutor().command("java", "-version").timeout(60, TimeUnit.SECONDS).execute();
```

#### Running a command and destroying it in case of a shutdown (output is pumped to NullOutputStream)
```java
new ProcessExecutor().command("java", "-version").destroyOnExit().execute();
```

#### Running a command by allowing exit code 3 only (output is pumped to NullOutputStream)
```java
try {
  new ProcessExecutor().command("java", "-version").exitValues(3).execute();
}
catch (InvalidExitValueException e) {
  System.out.println("Process exited with " + e.exitValue());
}
```

#### Running a command and returning its output as UTF-8 String while allowing exit code 3 only
```java
String output;
boolean success = false;
try {
  output = new ProcessExecutor().command("java", "-version").readOutput(true).exitValues(3).execute().outputUTF8();
  success = true;
}
catch (InvalidExitValueException e) {
  System.out.println("Process exited with " + e.exitValue());
  output = e.result().outputUTF8();
}
```

#### Starting a command in background (output is pumped to NullOutputStream)
```java
Future<ProcessResult> future = new ProcessExecutor().command("java", "-version").start();
// do some stuff
future.get(60, TimeUnit.SECONDS);
```

#### Starting a command in background and returning its output as UTF-8 String
```java
Future<ProcessResult> future = new ProcessExecutor().command("java", "-version").readOutput(true).start();
// do some stuff
String output = future.get(60, TimeUnit.SECONDS).outputUTF8();
```

