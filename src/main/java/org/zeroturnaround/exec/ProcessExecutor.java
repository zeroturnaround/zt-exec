/*
 * Copyright (C) 2014 ZeroTurnaround <support@zeroturnaround.com>
 * Contains fragments of code from Apache Commons Exec, rights owned
 * by Apache Software Foundation (ASF).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.zeroturnaround.exec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.zeroturnaround.exec.close.ProcessCloser;
import org.zeroturnaround.exec.close.StandardProcessCloser;
import org.zeroturnaround.exec.close.TimeoutProcessCloser;
import org.zeroturnaround.exec.listener.*;
import org.zeroturnaround.exec.stop.DestroyProcessStopper;
import org.zeroturnaround.exec.stop.NopProcessStopper;
import org.zeroturnaround.exec.stop.ProcessStopper;
import org.zeroturnaround.exec.stream.*;
import org.zeroturnaround.exec.stream.slf4j.Slf4jDebugOutputStream;
import org.zeroturnaround.exec.stream.slf4j.Slf4jInfoOutputStream;
import org.zeroturnaround.exec.stream.slf4j.Slf4jStream;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.*;



/**
 * Helper for executing sub processes.
 * <p>
 * It's implemented as a wrapper of {@link ProcessBuilder} complementing it with additional features such as:
 * </p>
 * <ul>
 *   <li>Handling process streams (copied from Commons Exec library).</li>
 *   <li>Destroying process on VM exit (copied from Commons Exec library).</li>
 *   <li>Checking process exit code.</li>
 *   <li>Setting a timeout for running the process and automatically stopping it in case of timeout.</li>
 *   <li>Either waiting for the process to finish ({@link #execute()}) or returning a {@link Future} ({@link #start()}.</li>
 *   <li>Reading the process output stream into a buffer ({@link #readOutput(boolean)}, {@link ProcessResult}).</li>
 * </ul>
 *
 * <p>
 * The default configuration for executing a process is following:
 * </p>
 * <ul>
 *   <li>Process is not automatically destroyed on VM exit.</li>
 *   <li>Error stream is redirected to its output stream. Use {@link #redirectErrorStream(boolean)} to override it.</li>
 *   <li>Output stream is pumped to a {@link NullOutputStream}, Use {@link #streams(ExecuteStreamHandler)}, {@link #redirectOutput(OutputStream)},
 *   or any of the <code>redirectOutputAs*</code> methods.to override it.</li>
 *   <li>Any exit code is allowed. Use {@link #exitValues(Integer...)} to override it.</li>
 *   <li>In case of timeout or cancellation {@link Process#destroy()} is invoked.</li>
 * </ul>
 *
 * @author Rein Raudjärv
 * @see ProcessResult
 */
public class ProcessExecutor {

  private static final Logger log = LoggerFactory.getLogger(ProcessExecutor.class);

  private static final boolean IS_OS_WINDOWS = System.getProperty("os.name").startsWith("Windows");

  public static final Integer[] DEFAULT_EXIT_VALUES = null;

  private static final Integer NORMAL_EXIT_VALUE = 0;

  public static final boolean DEFAULT_REDIRECT_ERROR_STREAM = true;

  private static final ThreadFactory DEFAULT_THREAD_FACTORY = new ThreadFactory() {
	@Override
	public Thread newThread(Runnable r) {
		return new Thread(r);
	}
  };

  /**
   * Process builder used by this executor.
   */
  private final ProcessBuilder builder = new ProcessBuilder();

  /**
   * Environment variables which are added (removed in case of <code>null</code> values) to the process being started.
   */
  private final Map<String, String> environment = new LinkedHashMap<String, String>();

  /**
   * Set of accepted exit codes or <code>null</code> if all exit codes are allowed.
   */
  private Set<Integer> allowedExitValues;

  /**
   * Timeout for running a process. If the process is running too long a {@link TimeoutException} is thrown and the process is destroyed.
   */
  private Long timeout;
  private TimeUnit timeoutUnit;

  /**
   * Helper for stopping the process in case of timeout or cancellation.
   */
  private ProcessStopper stopper;

  /**
   * Process stream Handler (copied from Commons Exec library). If <code>null</code> streams are not handled.
   */
  private ExecuteStreamHandler streams;

  /**
   * Timeout for closing process' standard streams. In case this timeout is reached we just log a warning but don't throw an error.
   */
  private Long closeTimeout;
  private TimeUnit closeTimeoutUnit;

  /**
   * <code>true</code> if the process output should be read to a buffer and returned by {@link ProcessResult#output()}.
   */
  private boolean readOutput;

  /**
   * Process event handlers.
   */
  private final CompositeProcessListener listeners = new CompositeProcessListener();

  /**
   * Helper for logging messages about starting and waiting for the processes.
   */
  private MessageLogger messageLogger = MessageLoggers.DEBUG;

  private ThreadFactory threadFactory = null;

  {
    // Run in case of any constructor
    exitValues(DEFAULT_EXIT_VALUES);
    stopper(DestroyProcessStopper.INSTANCE);
    threadFactory(null);
    redirectOutput(null);
    redirectError(null);
    destroyer(null);
    redirectErrorStream(DEFAULT_REDIRECT_ERROR_STREAM);
  }

  /**
   * Creates new {@link ProcessExecutor} instance.
   */
  public ProcessExecutor() {
  }

  private ProcessExecutor threadFactory(ThreadFactory tf) {
	if (tf == null) {
	  threadFactory = DEFAULT_THREAD_FACTORY;
	} else {
	  threadFactory = tf;
	}
	return this;
  }

  /**
   * Creates new {@link ProcessExecutor} instance for the given program and its arguments.
   * @param command The list containing the program and its arguments.
   */
  public ProcessExecutor(List<String> command) {
    command(command);
  }

  /**
   * Creates new {@link ProcessExecutor} instance for the given program and its arguments.
   * @param command A string array containing the program and its arguments.
   */
  public ProcessExecutor(String... command) {
    command(command);
  }

  /**
   * Creates new {@link ProcessExecutor} instance for the given program and its arguments.
   * @param command The iterable containing the program and its arguments.
   * @since 1.8
   */
  public ProcessExecutor(Iterable<String> command) {
    command(command);
  }

  /**
   * Returns this process executor's operating system program and arguments.
   * The returned list is a copy.
   *
   * @return this process executor's program and its arguments (not <code>null</code>).
   */
  public List<String> getCommand() {
    return new ArrayList<String>(builder.command());
  }

  /**
   * Sets the program and its arguments which are being executed.
   *
   * @param   command  The list containing the program and its arguments.
   * @return  This process executor.
   */
  public ProcessExecutor command(List<String> command) {
    builder.command(fixArguments(command));
    return this;
  }

  /**
   * Sets the program and its arguments which are being executed.
   *
   * @param   command  A string array containing the program and its arguments.
   * @return  This process executor.
   */
  public ProcessExecutor command(String... command) {
    builder.command(fixArguments(Arrays.asList(command)));
    return this;
  }

  /**
   * Sets the program and its arguments which are being executed.
   *
   * @param   command  The iterable containing the program and its arguments.
   * @return  This process executor.
   * @since 1.8
   */
  public ProcessExecutor command(Iterable<String> command) {
    List<String> list = new ArrayList<String>();
    Iterator<String> it = command.iterator();
    while (it.hasNext()) {
      list.add(it.next());
    }
    return command(list);
  }

  /**
   * Splits string by spaces and passes it to {@link ProcessExecutor#command(String...)}<br>
   *
   * NB: this method do not handle whitespace escaping,
   * <code>"mkdir new\ folder"</code> would be interpreted as
   * <code>{"mkdir", "new\", "folder"}</code> command.
   *
   * @param   commandWithArgs  A string array containing the program and its arguments.
   * @return  This process executor.
   */
  public ProcessExecutor commandSplit(String commandWithArgs) {
    builder.command(commandWithArgs.split("\\s+"));
    return this;
  }

  /**
   * Returns this process executor's working directory.
   *
   * Subprocesses subsequently started by this object will use this as their working directory.
   * The returned value may be {@code null} -- this means to use
   * the working directory of the current Java process, usually the
   * directory named by the system property {@code user.dir},
   * as the working directory of the child process.
   *
   * @return this process executor's working directory
   */
  public File getDirectory() {
    return builder.directory();
  }

  /**
   * Sets this working directory for the process being executed.
   * The argument may be <code>null</code> -- this means to use the
   * working directory of the current Java process, usually the
   * directory named by the system property <code>user.dir</code>,
   * as the working directory of the child process.
   *
   * @param   directory  The new working directory
   * @return  This process executor.
   */
  public ProcessExecutor directory(File directory) {
    builder.directory(directory);
    return this;
  }

  /**
   * Returns this process executor's additional environment variables.
   * The returned value is not a copy.
   *
   * @return this process executor's environment variables (not <code>null</code>).
   */
  public Map<String, String> getEnvironment() {
    return environment;
  }

  /**
   * Adds additional environment variables for the process being executed.
   *
   * @param env environment variables added to the process being executed.
   * @return This process executor.
   */
  public ProcessExecutor environment(Map<String,String> env) {
    environment.putAll(env);
    return this;
  }

  /**
   * Adds a single additional environment variable for the process being executed.
   *
   * @param name name of the environment variable added to the process being executed.
   * @param value value of the environment variable added to the process being executed.
   * @return This process executor.
   *
   * @since 1.7
   */
  public ProcessExecutor environment(String name, String value) {
    environment.put(name, value);
    return this;
  }

  /**
   * Sets this process executor's <code>redirectErrorStream</code> property.
   *
   * <p>If this property is <code>true</code>, then any error output generated by subprocesses will be merged with the standard output.
   * This makes it easier to correlate error messages with the corresponding output.
   * The initial value is <code>true</code>.</p>
   *
   * @param   redirectErrorStream  The new property value
   * @return  This process executor.
   */
  public ProcessExecutor redirectErrorStream(boolean redirectErrorStream) {
    builder.redirectErrorStream(redirectErrorStream);
    return this;
  }

  /**
   * Allows any exit value for the process being executed.
   *
   * @return This process executor.
   */
  public ProcessExecutor exitValueAny() {
    return exitValues((Integer[]) null);
  }

  /**
   * Allows only <code>0</code> as the exit value for the process being executed.
   *
   * @return This process executor.
   */
  public ProcessExecutor exitValueNormal() {
    return exitValues(NORMAL_EXIT_VALUE);
  }

  /**
   * Sets the allowed exit value for the process being executed.
   *
   * @param exitValue single exit value or <code>null</code> if all exit values are allowed.
   * @return This process executor.
   */
  public ProcessExecutor exitValue(Integer exitValue) {
    return exitValues(exitValue == null ? null : new Integer[] { exitValue } );
  }

  /**
   * Sets the allowed exit values for the process being executed.
   *
   * @param exitValues set of exit values or <code>null</code> if all exit values are allowed.
   * @return This process executor.
   */
  public ProcessExecutor exitValues(Integer... exitValues) {
    allowedExitValues = exitValues == null ? null : new HashSet<Integer>(Arrays.asList(exitValues));
    return this;
  }

  /**
   * Sets the allowed exit values for the process being executed.
   *
   * @param exitValues set of exit values or <code>null</code> if all exit values are allowed.
   * @return This process executor.
   */
  public ProcessExecutor exitValues(int[] exitValues) {
    if (exitValues == null)
      return exitValueAny();
    // Convert int[] -> Integer[]
    Integer[] array = new Integer[exitValues.length];
    for (int i = 0; i < array.length; i++)
      array[i] = exitValues[i];
    return exitValues(array);
  }

  /**
   * Sets a timeout for the process being executed. When this timeout is reached a {@link TimeoutException} is thrown and the process is destroyed.
   * This only applies to <code>execute</code> methods not <code>start</code> methods.
   *
   * @param timeout timeout for running a process.
   * @param unit the time unit of the timeout
   * @return This process executor.
   */
  public ProcessExecutor timeout(long timeout, TimeUnit unit) {
    this.timeout = timeout;
    this.timeoutUnit = unit;
    return this;
  }

  /**
   * Sets the helper for stopping the process in case of timeout or cancellation.
   * <p>
   * By default {@link DestroyProcessStopper} is used which just invokes {@link Process#destroy()}.
   *
   * @param stopper helper for stopping the process (<code>null</code> means {@link NopProcessStopper} - process is not stopped).
   * @return This process executor.
   */
  public ProcessExecutor stopper(ProcessStopper stopper) {
    if (stopper == null) {
      stopper = NopProcessStopper.INSTANCE;
    }
    this.stopper = stopper;
    return this;
  }

  /**
   * @return current stream handler for the process being executed.
   */
  public ExecuteStreamHandler streams() {
    return streams;
  }

  /**
   * Sets a stream handler for the process being executed.
   * This will overwrite any stream redirection that was previously set to use the provided handler.
   *
   * @param streams the stream handler
   * @return This process executor.
   */
  public ProcessExecutor streams(ExecuteStreamHandler streams) {
    validateStreams(streams, readOutput);
    this.streams = streams;
    return this;
  }

  /**
   * Sets a timeout for closing standard streams of the process being executed.
   * When this timeout is reached we log a warning but consider that the process has finished.
   * We also flush the streams so that all output read so far is available.
   * <p>
   * This can be used on Windows in case a process exits quickly but closing the streams blocks forever.
   * </p>
   * <p>
   * Closing timeout must fit into the general execution timeout (see {@link #timeout(long, TimeUnit)}).
   * By default there's no closing timeout.
   *
   * @param timeout timeout for closing streams of a process.
   * @param unit the time unit of the timeout
   * @return This process executor.
   */
  public ProcessExecutor closeTimeout(long timeout, TimeUnit unit) {
    this.closeTimeout = timeout;
    this.closeTimeoutUnit = unit;
    return this;
  }

  /**
   * Sets the input stream to redirect to the process' input stream.
   * If this method is invoked multiple times each call overwrites the previous.
   *
   * @param input input stream that will be written to the process input stream (<code>null</code> means nothing will be written to the process input stream).
   * @return This process executor.
   */
  public ProcessExecutor redirectInput(InputStream input) {
    PumpStreamHandler pumps = pumps();
    // Only set the input stream handler, preserve the same output and error stream handler
    return streams(new PumpStreamHandler(threadFactory, pumps == null ? null : pumps.getOut(), pumps == null ? null : pumps.getErr(), input));
  }

  /**
   * Redirects the process' output stream to given output stream.
   * If this method is invoked multiple times each call overwrites the previous.
   * Use {@link #redirectOutputAlsoTo(OutputStream)} if you want to redirect the output to multiple streams.
   *
   * @param output output stream where the process output is redirected to (<code>null</code> means {@link NullOutputStream} which acts like a <code>/dev/null</code>).
   * @return This process executor.
   */
  public ProcessExecutor redirectOutput(OutputStream output) {
    if (output == null)
      output = NullOutputStream.NULL_OUTPUT_STREAM;
    PumpStreamHandler pumps = pumps();
    // Only set the output stream handler, preserve the same error stream handler
    return streams(new PumpStreamHandler(threadFactory, output, pumps == null ? null : pumps.getErr(), pumps == null ? null : pumps.getInput()));
  }

  /**
   * Redirects the process' error stream to given output stream.
   * If this method is invoked multiple times each call overwrites the previous.
   * Use {@link #redirectErrorAlsoTo(OutputStream)} if you want to redirect the error to multiple streams.
   * <p>
   * Calling this method automatically disables merging the process error stream to its output stream.
   * </p>
   *
   * @param output output stream where the process error is redirected to (<code>null</code> means {@link NullOutputStream} which acts like a <code>/dev/null</code>).
   * @return This process executor.
   */
  public ProcessExecutor redirectError(OutputStream output) {
    if (output == null)
      output = NullOutputStream.NULL_OUTPUT_STREAM;
    PumpStreamHandler pumps = pumps();
    // Only set the error stream handler, preserve the same output stream handler
    streams(new PumpStreamHandler(threadFactory, pumps == null ? null : pumps.getOut(), output, pumps == null ? null : pumps.getInput()));
    redirectErrorStream(false);
    return this;
  }

  /**
   * Redirects the process' output stream also to a given output stream.
   * This method can be used to redirect output to multiple streams.
   *
   * @param output the stream to redirect this output to
   * @return This process executor.
   */
  public ProcessExecutor redirectOutputAlsoTo(OutputStream output) {
    return streams(redirectOutputAlsoTo(pumps(), output));
  }

  /**
   * Redirects the process' error stream also to a given output stream.
   * This method can be used to redirect error to multiple streams.
   * <p>
   * Calling this method automatically disables merging the process error stream to its output stream.
   * </p>
   *
   * @param output the output stream to redirect the error stream to
   * @return This process executor.
   */
  public ProcessExecutor redirectErrorAlsoTo(OutputStream output) {
    streams(redirectErrorAlsoTo(pumps(), output));
    redirectErrorStream(false);
    return this;
  }

  /**
   * @return current PumpStreamHandler (maybe <code>null</code>).
   * @throws IllegalStateException if the current stream handler is not an instance of {@link PumpStreamHandler}.
   *
   * @see #streams()
   */
  public PumpStreamHandler pumps() {
    if (streams == null)
      return null;
    if (!(streams instanceof PumpStreamHandler))
      throw new IllegalStateException("Only PumpStreamHandler is supported.");
    return (PumpStreamHandler) streams;
  }

  /**
   * Redirects the process' output stream also to a given output stream.
   *
   * @return new stream handler created.
   */
  private static PumpStreamHandler redirectOutputAlsoTo(PumpStreamHandler pumps, OutputStream output) {
    if (output == null)
      throw new IllegalArgumentException("OutputStream must be provided.");
    OutputStream current = pumps.getOut();
    if (current != null && !(current instanceof NullOutputStream)) {
      output = new TeeOutputStream(current, output);
    }
    return new PumpStreamHandler(DEFAULT_THREAD_FACTORY, output, pumps.getErr(), pumps.getInput());
  }

  /**
   * Redirects the process' error stream also to a given output stream.
   *
   * @return new stream handler created.
   */
  private static PumpStreamHandler redirectErrorAlsoTo(PumpStreamHandler pumps, OutputStream output) {
    if (output == null)
      throw new IllegalArgumentException("OutputStream must be provided.");
    OutputStream current = pumps.getErr();
    if (current != null && !(current instanceof NullOutputStream)) {
      output = new TeeOutputStream(current, output);
    }
    return new PumpStreamHandler(DEFAULT_THREAD_FACTORY, pumps.getOut(), output, pumps.getInput());
  }

  /**
   * Sets this process executor's <code>readOutput</code> property.
   *
   * <p>If this property is <code>true</code>,
   * the process output should be read to a buffer and returned by {@link ProcessResult#output()}.
   * The initial value is <code>false</code>.</p>
   *
   * @param   readOutput  The new property value
   * @return  This process executor.
   */
  public ProcessExecutor readOutput(boolean readOutput) {
    validateStreams(streams, readOutput);
    this.readOutput = readOutput;
    return this;
  }

  /**
   * Validates that if <code>readOutput</code> is <code>true</code> the output could be read with the given {@link ExecuteStreamHandler} instance.
   */
  private void validateStreams(ExecuteStreamHandler streams, boolean readOutput) {
    if (readOutput && !(streams instanceof PumpStreamHandler))
      throw new IllegalStateException("Only PumpStreamHandler is supported if readOutput is true.");
  }

  /**
   * Logs the process' output to a given {@link Logger} with <code>info</code> level.
   *
   * @param log the logger to process the output to
   * @return This process executor.
   * @deprecated use {@link #redirectOutput(OutputStream)} and {@link Slf4jStream}
   */
  public ProcessExecutor info(Logger log) {
    return redirectOutput(new Slf4jInfoOutputStream(log));
  }

  /**
   * Logs the process' output to a given {@link Logger} with <code>debug</code> level.
   *
   * @param log the logger to process the output to
   * @return This process executor.
   * @deprecated use {@link #redirectOutput(OutputStream)} and {@link Slf4jStream}
   */
  public ProcessExecutor debug(Logger log) {
    return redirectOutput(new Slf4jDebugOutputStream(log));
  }

  /**
   * Logs the process' output to a {@link Logger} with given name using <code>info</code> level.
   *
   * @param name the name of the logger to process the output to
   * @return This process executor.
   * @deprecated use {@link #redirectOutput(OutputStream)} and {@link Slf4jStream}
   */
  public ProcessExecutor info(String name) {
    return info(getCallerLogger(name));
  }

  /**
   * Logs the process' output to a {@link Logger} with given name using <code>debug</code> level.
   *
   * @param name the name of the logger to process the output to
   * @return This process executor.
   * @deprecated use {@link #redirectOutput(OutputStream)} and {@link Slf4jStream}
   */
  public ProcessExecutor debug(String name) {
    return debug(getCallerLogger(name));
  }

  /**
   * Logs the process' output to a {@link Logger} of the caller class using <code>info</code> level.
   *
   * @return This process executor.
   * @deprecated use {@link #redirectOutput(OutputStream)} and {@link Slf4jStream}
   */
  public ProcessExecutor info() {
    return info(getCallerLogger(null));
  }

  /**
   * Logs the process' output to a {@link Logger} of the caller class using <code>debug</code> level.
   * @return This process executor.
   * @deprecated use {@link #redirectOutput(OutputStream)} and {@link Slf4jStream}
   */
  public ProcessExecutor debug() {
    return debug(getCallerLogger(null));
  }

  /**
   * Logs the process' output to a given {@link Logger} with <code>info</code> level.
   *
   * @param log the logger to output the message to
   * @return This process executor.
   * @deprecated use {@link #redirectOutput(OutputStream)} and {@link Slf4jStream}
   */
  public ProcessExecutor redirectOutputAsInfo(Logger log) {
    return redirectOutput(new Slf4jInfoOutputStream(log));
  }

  /**
   * Logs the process' output to a given {@link Logger} with <code>debug</code> level.
   *
   * @param log the logger to output the message to
   * @return This process executor.
   * @deprecated use {@link #redirectOutput(OutputStream)} and {@link Slf4jStream}
   */
  public ProcessExecutor redirectOutputAsDebug(Logger log) {
    return redirectOutput(new Slf4jDebugOutputStream(log));
  }

  /**
   * Logs the process' output to a {@link Logger} with given name using <code>info</code> level.
   *
   * @param name the name of the logger to log to
   * @return This process executor.
   * @deprecated use {@link #redirectOutput(OutputStream)} and {@link Slf4jStream}
   */
  public ProcessExecutor redirectOutputAsInfo(String name) {
    return redirectOutputAsInfo(getCallerLogger(name));
  }

  /**
   * Logs the process' output to a {@link Logger} with given name using <code>debug</code> level.
   *
   * @param name the name of the logger to process output to
   * @return This process executor.
   * @deprecated use {@link #redirectOutput(OutputStream)} and {@link Slf4jStream}
   */
  public ProcessExecutor redirectOutputAsDebug(String name) {
    return redirectOutputAsDebug(getCallerLogger(name));
  }

  /**
   * Logs the process' output to a {@link Logger} of the caller class using <code>info</code> level.
   * @return This process executor.
   * @deprecated use {@link #redirectOutput(OutputStream)} and {@link Slf4jStream}
   */
  public ProcessExecutor redirectOutputAsInfo() {
    return redirectOutputAsInfo(getCallerLogger(null));
  }

  /**
   * Logs the process' output to a {@link Logger} of the caller class using <code>debug</code> level.
   * @return This process executor.
   * @deprecated use {@link #redirectOutput(OutputStream)} and {@link Slf4jStream}
   */
  public ProcessExecutor redirectOutputAsDebug() {
    return redirectOutputAsDebug(getCallerLogger(null));
  }

  /**
   * Logs the process' error to a given {@link Logger} with <code>info</code> level.
   *
   * @param log the logger to process output to
   * @return This process executor.
   * @deprecated use {@link #redirectError(OutputStream)} and {@link Slf4jStream}
   */
  public ProcessExecutor redirectErrorAsInfo(Logger log) {
    return redirectError(new Slf4jInfoOutputStream(log));
  }

  /**
   * Logs the process' error to a given {@link Logger} with <code>debug</code> level.
   *
   * @param log the logger to process the error to
   * @return This process executor.
   * @deprecated use {@link #redirectError(OutputStream)} and {@link Slf4jStream}
   */
  public ProcessExecutor redirectErrorAsDebug(Logger log) {
    return redirectError(new Slf4jDebugOutputStream(log));
  }

  /**
   * Logs the process' error to a {@link Logger} with given name using <code>info</code> level.
   *
   * @param name the name of the logger to process the error to
   * @return This process executor.
   * @deprecated use {@link #redirectError(OutputStream)} and {@link Slf4jStream}
   */
  public ProcessExecutor redirectErrorAsInfo(String name) {
    return redirectErrorAsInfo(getCallerLogger(name));
  }

  /**
   * Logs the process' error to a {@link Logger} with given name using <code>debug</code> level.
   *
   * @param name the name of the logger to process the error to
   * @return This process executor.
   * @deprecated use {@link #redirectError(OutputStream)} and {@link Slf4jStream}
   */
  public ProcessExecutor redirectErrorAsDebug(String name) {
    return redirectErrorAsDebug(getCallerLogger(name));
  }

  /**
   * Logs the process' error to a {@link Logger} of the caller class using <code>info</code> level.
   * @return This process executor.
   * @deprecated use {@link #redirectError(OutputStream)} and {@link Slf4jStream}
   */
  public ProcessExecutor redirectErrorAsInfo() {
    return redirectErrorAsInfo(getCallerLogger(null));
  }

  /**
   * Logs the process' error to a {@link Logger} of the caller class using <code>debug</code> level.
   * @return This process executor.
   * @deprecated use {@link #redirectError(OutputStream)} and {@link Slf4jStream}
   */
  public ProcessExecutor redirectErrorAsDebug() {
    return redirectErrorAsDebug(getCallerLogger(null));
  }

  /**
   * Creates a {@link Logger} for the {@link ProcessExecutor}'s caller class.
   *
   * @param name name of the logger.
   * @return SLF4J Logger instance.
   */
  private Logger getCallerLogger(String name) {
    return LoggerFactory.getLogger(CallerLoggerUtil.getName(name, 2));
  }

  /**
   * Adds a process destroyer to be notified when the process starts and stops.
   * @param destroyer helper for destroying all processes on certain event such as VM exit (not <code>null</code>).
   *
   * @return This process executor.
   */
  public ProcessExecutor addDestroyer(ProcessDestroyer destroyer) {
    return addListener(new DestroyerListenerAdapter(destroyer));
  }

  /**
   * Sets the process destroyer to be notified when the process starts and stops.
   * <p>
   * This methods always removes any other {@link ProcessDestroyer} registered. Use {@link #addDestroyer(ProcessDestroyer)} to keep the existing ones.
   *
   * @param destroyer helper for destroying all processes on certain event such as VM exit (maybe <code>null</code>).
   *
   * @return This process executor.
   */
  public ProcessExecutor destroyer(ProcessDestroyer destroyer) {
    removeListeners(DestroyerListenerAdapter.class);
    if (destroyer != null)
      addListener(new DestroyerListenerAdapter(destroyer));
    return this;
  }

  /**
   * Sets the started process to be destroyed on VM exit (shutdown hooks are executed).
   * If this VM gets killed the started process may not get destroyed.
   * <p>
   * To undo this command call <code>destroyer(null)</code>.
   *
   * @return This process executor.
   */
  public ProcessExecutor destroyOnExit() {
    return destroyer(ShutdownHookProcessDestroyer.INSTANCE);
  }

  /**
   * Unregister all existing process event handlers and register new one.
   * @param listener process event handler to be set (maybe <code>null</code>).
   *
   * @return This process executor.
   */
  public ProcessExecutor listener(ProcessListener listener) {
    clearListeners();
    if (listener != null)
      addListener(listener);
    return this;
  }

  /**
   * Register new process event handler.
   * @param listener process event handler to be added.
   *
   * @return This process executor.
   */
  public ProcessExecutor addListener(ProcessListener listener) {
    listeners.add(listener);
    return this;
  }

  /**
   * Unregister existing process event handler.
   * @param listener process event handler to be removed.
   *
   * @return This process executor.
   */
  public ProcessExecutor removeListener(ProcessListener listener) {
    listeners.remove(listener);
    return this;
  }

  /**
   * Unregister existing process event handlers of given type or its sub-types.
   * @param listenerType process event handler type.
   *
   * @return This process executor.
   */
  public ProcessExecutor removeListeners(Class<? extends ProcessListener> listenerType) {
    listeners.removeAll(listenerType);
    return this;
  }

  /**
   * Unregister all existing process event handlers.
   *
   * @return This process executor.
   */
  public ProcessExecutor clearListeners() {
    listeners.clear();
    return this;
  }

  /**
   * Changes how most common messages about starting and waiting for processes are actually logged.
   * By default {@link MessageLoggers#DEBUG} is used.
   * However if someone is executing a process every second {@link MessageLoggers#TRACE} may be used e.g.
   *
   * @param messageLogger message logger for certain level.
   *
   * @return This process executor.
   */
  public ProcessExecutor setMessageLogger(MessageLogger messageLogger) {
    this.messageLogger = messageLogger;
    return this;
  }

  /**
   * Executes the sub process. This method waits until the process exits, a timeout occurs or the caller thread gets interrupted.
   * In the latter cases the process gets destroyed as well.
   *
   * @return exit code of the finished process.
   * @throws IOException an error occurred when process was started or stopped.
   * @throws InterruptedException this thread was interrupted.
   * @throws TimeoutException timeout set by {@link #timeout(long, TimeUnit)} was reached.
   * @throws InvalidExitValueException if invalid exit value was returned (@see {@link #exitValues(Integer...)}).
   */
  public ProcessResult execute() throws IOException, InterruptedException, TimeoutException, InvalidExitValueException {
    return waitFor(startInternal());
  }

  /**
   * Executes the sub process. This method waits until the process exits.
   * Value passed to {@link #timeout(long, TimeUnit)} is ignored (use {@link #execute()} for timeout).
   *
   * @return exit code of the finished process.
   * @throws IOException an error occurred when process was started or stopped.
   * @throws InterruptedException this thread was interrupted.
   * @throws InvalidExitValueException if invalid exit value was returned (@see {@link #exitValues(Integer...)}).
   */
  public ProcessResult executeNoTimeout() throws IOException, InterruptedException, InvalidExitValueException {
    return startInternal().call();
  }

  /**
   * Check the exit value of given process result. This can be used by unit tests.
   *
   * @param result process result which maybe constructed by a unit test.
   * @throws InvalidExitValueException if the given exit value was rejected.
   */
  public void checkExitValue(ProcessResult result) throws InvalidExitValueException {
    InvalidExitUtil.checkExit(getAttributes(), result);
  }

  /**
   * Start the sub process. This method does not wait until the process exits.
   * Value passed to {@link #timeout(long, TimeUnit)} is ignored.
   * Use {@link Future#get()} to wait for the process to finish.
   * Invoke <code>future.cancel(true);</code> to destroy the process.
   *
   * @return Future representing the exit value of the finished process.
   * @throws IOException an error occurred when process was started.
   */
  public StartedProcess start() throws IOException {
    WaitForProcess task = startInternal();
    ExecutorService service = newExecutor(task);
    Future<ProcessResult> future = invokeSubmit(service, task);
    // Previously submitted tasks are executed but no new tasks will be accepted.
    // However sub classes could return null as the ExecutorService
    if (service != null) {
      service.shutdown();
    }
    return new StartedProcess(task.getProcess(), future);
  }

  /**
   * Start the process and its stream handlers.
   *
   * @return process the started process.
   * @throws IOException the process or its stream handlers couldn't start (in the latter case we also destroy the process).
   */
  protected final WaitForProcess startInternal() throws IOException {
    // Invoke listeners - they can modify this executor
    listeners.beforeStart(this);

    if (builder.command().isEmpty()) {
      throw new IllegalStateException("Command has not been set.");
    }
    validateStreams(streams, readOutput);

    applyEnvironment();
    messageLogger.message(log, getExecutingLogMessage());
    Process process = invokeStart();
    messageLogger.message(log, "Started {}", process);
    ProcessAttributes attributes = getAttributes();

    ExecuteStreamHandler newStreams = streams;
    ByteArrayOutputStream out = null;
    if (readOutput) {
      PumpStreamHandler pumps = (PumpStreamHandler) streams;
      out = new ByteArrayOutputStream();
      newStreams = redirectOutputAlsoTo(pumps, out);
    }
    return startInternal(process, attributes, newStreams, out);
  }

  /**
   * Capture a snapshot of this process executor's main state.
   */
  private ProcessAttributes getAttributes() {
    return new ProcessAttributes(
          getCommand(),
          getDirectory(),
          new LinkedHashMap<String, String>(environment),
          allowedExitValues == null ? null : new HashSet<Integer>(allowedExitValues));
  }

  private Process invokeStart() throws IOException {
    try {
      return builder.start();
    }
    catch (IOException e) {
      if (e.getClass().equals(IOException.class)) {
        String message = getExecutingErrorMessage();
        ProcessInitException p = ProcessInitException.newInstance(message, e);
        if (p != null) {
          throw p;
        }
        throw new IOException(message, e);
      }
      throw e;
    }
    catch (RuntimeException e) {
      if (e.getClass().equals(IllegalArgumentException.class)) {
        throw new IllegalArgumentException(getExecutingErrorMessage(), e);
      }
      throw e;
    }
  }

  private String getExecutingLogMessage() {
    return "Executing " + getExecutingMessageParams();
  }

  private String getExecutingErrorMessage() {
    return "Could not execute " + getExecutingMessageParams();
  }

  private String getExecutingMessageParams() {
    String result = "" + builder.command();
    if (builder.directory() != null) {
      result += " in " + builder.directory();
    }
    if (!environment.isEmpty()) {
      result += " with environment " + environment;
    }
    result += ".";
    return result;
  }

  private WaitForProcess startInternal(Process process, ProcessAttributes attributes, ExecuteStreamHandler streams, ByteArrayOutputStream out) throws IOException {
    if (streams != null) {
      try {
        streams.setProcessInputStream(process.getOutputStream());
        streams.setProcessOutputStream(process.getInputStream());
        if (!builder.redirectErrorStream())
          streams.setProcessErrorStream(process.getErrorStream());
      }
      catch (IOException e) {
        process.destroy();
        throw e;
      }
      streams.start();
    }

    ProcessCloser closer = newProcessCloser(streams);

    WaitForProcess result = new WaitForProcess(process, attributes, stopper, closer, out, listeners.clone(), messageLogger);
    // Invoke listeners - changing this executor does not affect the started process any more
    listeners.afterStart(process, this);
    return result;
  }

  private ProcessCloser newProcessCloser(ExecuteStreamHandler streams) {
    if (closeTimeout == null) {
      return new StandardProcessCloser(streams);
    }
    return new TimeoutProcessCloser(streams, closeTimeout, closeTimeoutUnit);
  }

  /**
   * Wait until the process stops, a timeout occurs and the caller thread gets interrupted.
   * In the latter cases the process gets destroyed as well.
   */
  private ProcessResult waitFor(WaitForProcess task) throws IOException, InterruptedException, TimeoutException {
    ProcessResult result;
    if (timeout == null) {
      // Use the current thread
      result = task.call();
    }
    else {
      // Fork another thread to invoke Process.waitFor()
      ExecutorService service = newExecutor(task);
      // Copy values to not conflict with further executions
      long _timeout = timeout;
      TimeUnit unit = timeoutUnit;
      try {
        result = invokeSubmit(service, task).get(_timeout, unit);
      }
      catch (ExecutionException e) {
        Throwable c = e.getCause();
        if (c instanceof IOException) {
          throw (IOException) c;
        }
        if (c instanceof InterruptedException) {
          throw (InterruptedException) c;
        }
        if (c instanceof InvalidExitValueException) {
          InvalidExitValueException i = (InvalidExitValueException) c;
          throw new InvalidExitValueException(i.getMessage(), i.getResult());
        }
        if (c instanceof InvalidOutputException) {
          InvalidOutputException i = (InvalidOutputException) c;
          throw new InvalidOutputException(i.getMessage(), i.getResult());
        }
        if (c.getClass().equals(InvalidResultException.class)) {
          InvalidResultException p = (InvalidResultException) c;
          throw new InvalidResultException(p.getMessage(), p.getResult());
        }
        throw new IllegalStateException("Error occured while waiting for process to finish:", c);
      }
      catch (TimeoutException e) {
        messageLogger.message(log, "{} is running too long", task);
        throw newTimeoutException(_timeout, unit, task);
      }
      finally {
        // Interrupt the task if it's still running and release the ExecutorService's resources
        service.shutdownNow();
      }
    }
    return result;
  }

  private ExecutorService newExecutor(WaitForProcess task) {
    return newExecutor(task.getProcess().toString());
  }

  protected ExecutorService newExecutor(String processName) {
    // Use daemon thread as we don't want to postpone the shutdown
    // If #destroyOnExit() is used we wait for the process to be destroyed anyway
    final String name = "WaitForProcess-" + processName;
    ExecutorService service = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
      public Thread newThread(Runnable r) {
        Thread t = new Thread(r, name);
        t.setDaemon(true);
        return t;
      }
    });
    return service;
  }

  /**
   * Override this to customize how the waiting task is started in the background.
   *
   * @param <T> the type of the task
   * @param executor the executor service to submit the task on
   * @param task the task to be submitted
   * @return the future of the task
   */
  protected <T> Future<T> invokeSubmit(ExecutorService executor, Callable<T> task) {
    return executor.submit(wrapTask(task));
  }

  /**
   * Override this to customize how the background task is created.
   *
   * @param <T> the type of the Task
   * @param task the Task to be wrapped
   * @return the wrapped task
   */
  protected <T> Callable<T> wrapTask(Callable<T> task) {
    // Preserve the MDC context of the caller thread.
    Map<?,?> contextMap = MDC.getCopyOfContextMap();
    if (contextMap != null) {
      return new MDCCallableAdapter<T>(task, contextMap);
    }
    return task;
  }

  private TimeoutException newTimeoutException(long timeout, TimeUnit unit, WaitForProcess task) {
    StringBuilder sb = new StringBuilder();
    Process process = task.getProcess();
    Integer exitValue = getExitCodeOrNull(process);
    if (exitValue == null) {
      sb.append("Timed out waiting for ").append(process).append(" to finish");
    }
    else {
      sb.append("Timed out finishing ").append(process);
      sb.append(", exit value: ").append(exitValue);
    }
    sb.append(", timeout: ").append(timeout).append(" ").append(getUnitsAsString(timeout, unit));
    task.addExceptionMessageSuffix(sb);
    TimeoutException result = new TimeoutException(sb.toString());
    if (exitValue != null) {
      StackTraceElement[] stackTrace = task.getStackTrace();
      if (stackTrace != null) {
        Exception cause = new Exception("Stack dump of worker thread.");
        cause.setStackTrace(stackTrace);
        result.initCause(cause);
      }
    }
    return result;
  }

  private static String getUnitsAsString(long d, TimeUnit unit) {
    String result = unit.toString().toLowerCase();
    if (d == 1) {
      result = result.substring(0, result.length() - 1);
    }
    return result;
  }

  private static Integer getExitCodeOrNull(Process process) {
    try {
      return process.exitValue();
    }
    catch (IllegalThreadStateException e) {
      return null;
    }
  }

  private void applyEnvironment() {
    if (environment.isEmpty()) {
      return; // skip
    }
    Map<String, String> env = builder.environment();
    for (Entry<String, String> e : environment.entrySet()) {
      String key = e.getKey();
      String value = e.getValue();
      if (value == null) {
        env.remove(key);
      }
      else {
        env.put(key, value);
      }
    }
  }

  /**
   * Fixes the command line arguments on Windows by replacing empty arguments with <code>""</code>. Otherwise these arguments would be just skipped.
   *
   * @see http://bugs.java.com/view_bug.do?bug_id=7028124
   * @see https://bugs.openjdk.java.net/browse/JDK-6518827
   */
  private static List<String> fixArguments(List<String> command) {
    if (!IS_OS_WINDOWS) {
      return command;
    }
    List<String> result = new ArrayList<String>(command);
    for (ListIterator<String> it = result.listIterator(); it.hasNext(); ) {
      if ("".equals(it.next())) {
        it.set("\"\"");
      }
    }
    return result;
  }

}
