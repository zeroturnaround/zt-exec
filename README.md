ZT Process Executor
================

### Continuous Integration
[![Build Status](https://travis-ci.org/zeroturnaround/zt-exec.png)](https://travis-ci.org/zeroturnaround/zt-exec)

### Quick Overview

The project was created to merge similar functionality of projects at [ZeroTurnaround](http://zeroturnaround.com/) into a single codebase.
It's designed to be powerful but still simple to use. By using a single class **ProcessExecutor**
the user gets the functionality from both **java.lang.ProcessBuilder** and [Apache Commons Exec](http://commons.apache.org/proper/commons-exec/).

### Installation
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.zeroturnaround/zt-exec/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.zeroturnaround/zt-exec)

The project artifacts are available in [Maven Central Repository](https://search.maven.org/artifact/org.zeroturnaround/zt-exec/).

To include it in your maven project then you have to specify the dependency.

```xml
...
<dependency>
    <groupId>org.zeroturnaround</groupId>
    <artifactId>zt-exec</artifactId>
    <version>1.12</version>
</dependency>
...
```

## Motivation

There are many approaches to take when running external processes from Java. There are the **JRE** options such as the **Runtime.exec()** and **ProcessBuilder**. Also there is the [Apache Commons Exec](http://commons.apache.org/proper/commons-exec/). Nevertheless we created yet another process library (**YAPL**). 

Some of the reasons for this crazy endeavour

* Improved handling of streams
 * Reading/writing to streams
 * Redirecting stderr to stdout
* Improved handling of timeouts
* Improved checking of exit codes
* Improved API
 * One liners for quite complex usecases
 * One liners to get process output into a String
 * Access to the **Process** object available
 * Support for async processes ( **Future** ) 
* Improved logging with [SLF4J API](http://www.slf4j.org/)
* Support for multiple processes

**Can zt-exec also kill processes?**

No. There is [zt-process-killer](https://github.com/zeroturnaround/zt-process-killer) for that.

## Examples

* Output is pumped to NullOutputStream

```java
new ProcessExecutor().command("java", "-version").execute();
```

<hr/>

* Returning the exit code
* Output is pumped to NullOutputStream

```java
int exit = new ProcessExecutor().command("java", "-version")
                  .execute().getExitValue();
```

<hr/>

* Return output as UTF8 String

```java
String output = new ProcessExecutor().command("java", "-version")
                  .readOutput(true).execute()
                  .outputUTF8();    
```

<hr/>

* Pumping the output to a logger

```java
new ProcessExecutor().command("java", "-version")
      .redirectOutput(Slf4jStream.of(LoggerFactory.getLogger(getClass().getName() + ".MyProcess")).asInfo()).execute();
```

<hr/>

* Pumping the output to a logger (short form for previous)

```java
new ProcessExecutor().command("java", "-version")
      .redirectOutput(Slf4jStream.of("MyProcess").asInfo()).execute();
```

<hr/>

* Pumping the output to the logger of the caller class

```java
new ProcessExecutor().command("java", "-version")
      .redirectOutput(Slf4jStream.ofCaller().asInfo()).execute();
```

<hr/>

* Pumping the output to a logger
* Returning output as UTF8 String

```java
String output = new ProcessExecutor().command("java", "-version")
        .redirectOutput(Slf4jStream.of(getClass()).asInfo())
        .readOutput(true).execute().outputUTF8();
```

<hr/>

* Pumping the stderr to a logger
* Returning the output as UTF8 String

```java
String output = new ProcessExecutor().command("java", "-version")
        .redirectError(Slf4jStream.of(getClass()).asInfo())
        .readOutput(true).execute()
        .outputUTF8();
```

<hr/>

* Running with a timeout of **60** seconds
* Output pumped to NullOutputStream

```java
try {
  new ProcessExecutor().command("java", "-version")
        .timeout(60, TimeUnit.SECONDS).execute();
}
catch (TimeoutException e) {
  // process is automatically destroyed
}
```

<hr/>

* Pumping output to another OutputStream

```java
OutputStream out = ...;
new ProcessExecutor().command("java", "-version")
      .redirectOutput(out).execute();
```

<hr/>

* Handling output line-by-line while process is running

```java
new ProcessExecutor().command("java", "-version")
    .redirectOutput(new LogOutputStream() {
      @Override
      protected void processLine(String line) {
        ...
      }
    })
    .execute();
```

<hr/>

* Destroy the running process when VM exits
* Output pumped to NullOutputStream

```java
new ProcessExecutor().command("java", "-version").destroyOnExit().execute();
```

<hr/>

* Run process with a specific environment variable
* Output pumped to NullOutputStream

```java
new ProcessExecutor().command("java", "-version")
    .environment("foo", "bar").execute();
```

<hr/>

* Run process with a specific environment
* Output pumped to NullOutputStream

```java
Map<String, String> env = ...
new ProcessExecutor().command("java", "-version")
    .environment(env).execute();
```

<hr/>

* Throw exception when wrong exit code
* Output is pumped to NullOutputStream

```java
try {
  new ProcessExecutor().command("java", "-version")
        .exitValues(3).execute();
}
catch (InvalidExitValueException e) {
  System.out.println("Process exited with " + e.getExitValue());
}
```

<hr/>

* Throw exception when wrong exit code
* Return output as UTF8 String 

```java
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
```

<hr/>

* Starting process in the background
* Output is pumped to NullOutputStream

```java
Future<ProcessResult> future = new ProcessExecutor()
                                    .command("java", "-version")
                                    .start().getFuture();
// do some stuff
future.get(60, TimeUnit.SECONDS);
```

<hr/>

* Start process in the background
* Return output as UTF8 String

```java
Future<ProcessResult> future = new ProcessExecutor()
                                    .command("java", "-version")
                                    .readOutput(true)
                                    .start().getFuture();
// do some stuff
String output = future.get(60, TimeUnit.SECONDS).outputUTF8();
```

