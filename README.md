Process Executor
================

### Quick Overview

The project was created to merge similar functionality of projects at [ZeroTurnaround](http://zeroturnaround.com/) into a single codebase.
It's designed to be powerful but still simple to use. By using a single class **ProcessExecutor**
the user gets the functionality from both **java.lang.ProcessBuilder** and [Apache Commons Exec](http://commons.apache.org/proper/commons-exec/).

### Installation

The project artifacts are available in [Maven Central Repository](http://search.maven.org/#browse%7C895841167).

To include it in your maven project then you have to specify the dependency.

```xml
...
<dependency>
    <groupId>org.zeroturnaround</groupId>
    <artifactId>zt-exec</artifactId>
    <version>1.4-SNAPSHOT</version>
    <type>jar</type>
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

<hr/>

* Returning the exit code
* Does not complain about any exit value
* Output is pumped to NullOutputStream

```java
int exit = new ProcessExecutor().command("java", "-version")
                  .exitValueAny().execute().exitValue();
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
      .redirectOutputAsInfo(LoggerFactory.getLogger(getClass().getName() + ".MyProcess")).execute();
```

<hr/>

* Pumping the output to a logger (short form for previous)

```java
new ProcessExecutor().command("java", "-version")
      .redirectOutputAsInfo("MyProcess").execute();
```

<hr/>

* Pumping the output to the logger of the caller class

```java
new ProcessExecutor().command("java", "-version")
      .redirectOutputAsInfo().execute();
```

<hr/>

* Pumping the output to a logger
* Returning output as UTF8 String

```java
String output = new ProcessExecutor().command("java", "-version")
        .redirectOutputAsInfo(LoggerFactory.getLogger(getClass()))
        .readOutput(true).execute().outputUTF8();
```

<hr/>

* Pumping the stderr to a logger
* Returning the output as UTF8 String

```java
String output = new ProcessExecutor().command("java", "-version")
      .redirectErrorAsInfo(LoggerFactory.getLogger(getClass()))
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

* Destroy the running process when VM exits
* Output pumped to NullOutputStream

```java
new ProcessExecutor().command("java", "-version").destroyOnExit().execute();
```

<hr/>

* Run process with a specific environment
* Output pumped to NullOutputStream

```java
new ProcessExecutor().command("java", "-version")
    .environment(new HashMap<String, String>() { { put("foo", "bar"); } })
    .execute();
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
  System.out.println("Process exited with " + e.exitValue());
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
  System.out.println("Process exited with " + e.exitValue());
  output = e.result().outputUTF8();
}
```

<hr/>

* Starting process in the background
* Output is pumped to NullOutputStream

```java
Future<ProcessResult> future = new ProcessExecutor()
                                    .command("java", "-version")
                                    .start();
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
                                    .start();
// do some stuff
String output = future.get(60, TimeUnit.SECONDS).outputUTF8();
```

