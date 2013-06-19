Process Executor
================

### Quick Overview

The project was created to merge similar functionality of projects at [ZeroTurnaround](http://zeroturnaround.com/) into a single codebase.
It's designed to be powerful but still simple to use. By using a single class **ProcessExecutor**
the user gets the functionality from both **java.lang.ProcessBuilder** and [Apache Commons Exec](http://commons.apache.org/proper/commons-exec/).

### Installation

TODO Use released version once 1.4 gets released

To include the current snapshot in your Maven project you need to define the repository and the dependency:

```xml
<repository>
  <id>zt-public-snapshots</id>
  <url>http://repos.zeroturnaround.com/nexus/content/repositories/zt-public-snapshots</url>
  <releases>
    <enabled>false</enabled>
  </releases>
</repository>

...
<dependency>
  <groupId>org.zeroturnaround</groupId>
  <artifactId>zt-exec</artifactId>
  <version>1.4-SNAPSHOT</version>
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

## Examples

* Output is pumped to NullOutputStream
```java
new ProcessExecutor().command("java", "-version").execute();
```

* Returning the exit code
* Does not complain about any exit value
* Output is pumped to NullOutputStream

```java
int exit = new ProcessExecutor().command("java", "-version")
                  .exitValueAny().execute().exitValue();
```

* Return output as UTF8 String

```java
String output = new ProcessExecutor().command("java", "-version")
                  .readOutput(true).execute()
                  .outputUTF8();    
```

* Pumping the output to a logger

```java
new ProcessExecutor().command("java", "-version")
      .info(LoggerFactory.getLogger(getClass())).execute();
```

* Pumping the output to a logger
* Returning output as UTF8 String

```java
String output = new ProcessExecutor().command("java", "-version")
        .info(LoggerFactory.getLogger(getClass()))
        .readOutput(true).execute().outputUTF8();
```

#### Running a command and pumping its error stream to a logger (with info level) but returning its output stream as UTF-8 String

* Pumping the stderr to a logger
* Returning the output as UTF8 String
```java
String output = new ProcessExecutor().command("java", "-version")
      .redirectErrorStream(false)
      .redirectErrorAsInfo(LoggerFactory.getLogger(getClass()))
      .readOutput(true).execute()
      .outputUTF8();
```

* Running with a timeout of **60** seconds
* Output pumped to NullOutputStream

```java
new ProcessExecutor().command("java", "-version")
      .timeout(60, TimeUnit.SECONDS).execute();
```

* Pumping output to another OutputStream
```java
OutputStream out = ...;
new ProcessExecutor().command("java", "-version")
      .redirectOutput(out).execute();
```

* Destroy the running process when VM exits
* Output pumped to NullOutputStream

```java
new ProcessExecutor().command("java", "-version").destroyOnExit().execute();
```

* Run process with a specific environment
* Output pumped to NullOutputStream

```java
new ProcessExecutor().command("java", "-version")
    .environment(new HashMap<String, String>() { { put("foo", "bar"); } })
    .execute();
```

* Throw exception when wrong exit code
* Output is pumped to NullOutputStream

```java
try {
  new ProcessExecutor().command("java", "-version")
        .exitValues(3).execute();
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

