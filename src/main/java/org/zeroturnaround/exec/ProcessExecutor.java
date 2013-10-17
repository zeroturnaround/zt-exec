/*
 * Copyright (C) 2013 ZeroTurnaround <support@zeroturnaround.com>
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.io.output.NullOutputStream;
import org.apache.commons.io.output.TeeOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.exec.listener.CompositeProcessListener;
import org.zeroturnaround.exec.listener.DestroyerListenerAdapter;
import org.zeroturnaround.exec.listener.ProcessDestroyer;
import org.zeroturnaround.exec.listener.ProcessListener;
import org.zeroturnaround.exec.listener.ShutdownHookProcessDestroyer;
import org.zeroturnaround.exec.stream.CallerLoggerUtil;
import org.zeroturnaround.exec.stream.ExecuteStreamHandler;
import org.zeroturnaround.exec.stream.PumpStreamHandler;
import org.zeroturnaround.exec.stream.slf4j.Slf4jDebugOutputStream;
import org.zeroturnaround.exec.stream.slf4j.Slf4jInfoOutputStream;



/**
 * Helper for executing sub processes.
 * <p>
 * It's implemented as a wrapper of {@link ProcessBuilder} complementing it with additional features such as:
 * <ul>
 *   <li>Handling process streams (copied from Commons Exec library).</li>
 *   <li>Destroying process on VM exit (copied from Commons Exec library).</li>
 *   <li>Checking process exit code.</li>
 *   <li>Setting a timeout for running the process.</li>
 *   <li>Either waiting for the process to finish ({@link #execute()}) or returning a {@link Future} ({@link #start()}.</li>
 *   <li>Reading the process output stream into a buffer ({@link #readOutput(boolean)}, {@link ProcessResult}).</li>
 * </ul>
 * <p>
 * The default configuration for executing a process is following:
 * <ul>
 *   <li>Process is not automatically destroyed on VM exit.</li>
 *   <li>Error stream is redirected to its output stream. Use {@link #redirectErrorStream(boolean)} to override it.</li>
 *   <li>Output stream is pumped to a {@link NullOutputStream}, Use {@link #streams(ExecuteStreamHandler)}, {@link #redirectOutput(OutputStream)},
 *   or any of the <code>redirectOutputAs*</code> methods.to override it.</li>
 *   <li>Any exit code is allowed. Use {@link #exitValues(Integer...)} to override it.
 * </li>
 * </p>
 *
 * @author Rein Raudjärv
 * @see ProcessResult
 */
public class ProcessExecutor {

  private static final Logger log = LoggerFactory.getLogger(ProcessExecutor.class);

  public static final Integer[] DEFAULT_EXIT_VALUES = null;

  private static final Integer NORMAL_EXIT_VALUE = 0;

  public static final boolean DEFAULT_REDIRECT_ERROR_STREAM = true;

  /**
   * Process builder used by this executor.
   */
  private final ProcessBuilder builder = new ProcessBuilder();

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
   * Process stream Handler (copied from Commons Exec library). If <code>null</code> streams are not handled.
   */
  private ExecuteStreamHandler streams;

  /**
   * <code>true</code> if the process output should be read to a buffer and returned by {@link ProcessResult#output()}.
   */
  private boolean readOutput;

  /**
   * Process event handlers.
   */
  private final CompositeProcessListener listeners = new CompositeProcessListener();

  {
    // Run in case of any constructor
    exitValues(DEFAULT_EXIT_VALUES);
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
   * Sets the program and its arguments which are being executed.
   *
   * @param   command  The list containing the program and its arguments.
   * @return  This process executor.
   */
  public ProcessExecutor command(List<String> command) {
    builder.command(command);
    return this;
  }

  /**
   * Sets the program and its arguments which are being executed.
   *
   * @param   command  A string array containing the program and its arguments.
   * @return  This process executor.
   */
  public ProcessExecutor command(String... command) {
    builder.command(command);
    return this;
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
    builder.command(commandWithArgs.split(" "));
    return this;
  }

  /**
   * Sets this working directory for the process being executed.
   * The argument may be <code>null</code> -- this means to use the
   * working directory of the current Java process, usually the
   * directory named by the system property <code>user.dir</code>,
   * as the working directory of the child process.</p>
   *
   * @param   directory  The new working directory
   * @return  This process executor.
   */
  public ProcessExecutor directory(File directory) {
    builder.directory(directory);
    return this;
  }

  /**
   * Adds additional environment variables for the process being executed.
   *
   * @param env environment variables added to the process being executed.
   * @return This process executor.
   */
  public ProcessExecutor environment(Map<String,String> env) {
    builder.environment().putAll(env);
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
   * @return This process executor.
   */
  public ProcessExecutor timeout(long timeout, TimeUnit unit) {
    this.timeout = timeout;
    this.timeoutUnit = unit;
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
   * @return This process executor.
   */
  public ProcessExecutor streams(ExecuteStreamHandler streams) {
    validateStreams(streams, readOutput);
    this.streams = streams;
    return this;
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
    return streams(new PumpStreamHandler(output, pumps == null ? null : pumps.getErr(), pumps == null ? null : pumps.getInput()));
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
    streams(new PumpStreamHandler(pumps == null ? null : pumps.getOut(), output, pumps == null ? null : pumps.getInput()));
    redirectErrorStream(false);
    return this;
  }

  /**
   * Redirects the process' output stream also to a given output stream.
   * This method can be used to redirect output to multiple streams.
   *
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
    return new PumpStreamHandler(output, pumps.getErr(), pumps.getInput());
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
    return new PumpStreamHandler(pumps.getOut(), output, pumps.getInput());
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
   * @return This process executor.
   * @deprecated use {@link #redirectOutputAsInfo(Logger)}
   */
  public ProcessExecutor info(Logger log) {
    return redirectOutput(new Slf4jInfoOutputStream(log));
  }

  /**
   * Logs the process' output to a given {@link Logger} with <code>debug</code> level.
   * @return This process executor.
   * @deprecated use {@link #redirectOutputAsDebug(Logger)}
   */
  public ProcessExecutor debug(Logger log) {
    return redirectOutput(new Slf4jDebugOutputStream(log));
  }

  /**
   * Logs the process' output to a {@link Logger} with given name using <code>info</code> level.
   * @return This process executor.
   * @deprecated use {@link #redirectOutputAsInfo(String)}
   */
  public ProcessExecutor info(String name) {
    return info(getCallerLogger(name));
  }

  /**
   * Logs the process' output to a {@link Logger} with given name using <code>debug</code> level.
   * @return This process executor.
   * @deprecated use {@link #redirectOutputAsDebug(String)}
   */
  public ProcessExecutor debug(String name) {
    return debug(getCallerLogger(name));
  }

  /**
   * Logs the process' output to a {@link Logger} of the caller class using <code>info</code> level.
   * @return This process executor.
   * @deprecated use {@link #redirectOutputAsInfo()}
   */
  public ProcessExecutor info() {
    return info(getCallerLogger(null));
  }

  /**
   * Logs the process' output to a {@link Logger} of the caller class using <code>debug</code> level.
   * @return This process executor.
   * @deprecated use {@link #redirectOutputAsDebug()}
   */
  public ProcessExecutor debug() {
    return debug(getCallerLogger(null));
  }

  /**
   * Logs the process' output to a given {@link Logger} with <code>info</code> level.
   * @return This process executor.
   */
  public ProcessExecutor redirectOutputAsInfo(Logger log) {
    return redirectOutput(new Slf4jInfoOutputStream(log));
  }

  /**
   * Logs the process' output to a given {@link Logger} with <code>debug</code> level.
   * @return This process executor.
   */
  public ProcessExecutor redirectOutputAsDebug(Logger log) {
    return redirectOutput(new Slf4jDebugOutputStream(log));
  }

  /**
   * Logs the process' output to a {@link Logger} with given name using <code>info</code> level.
   * @return This process executor.
   */
  public ProcessExecutor redirectOutputAsInfo(String name) {
    return redirectOutputAsInfo(getCallerLogger(name));
  }

  /**
   * Logs the process' output to a {@link Logger} with given name using <code>debug</code> level.
   * @return This process executor.
   */
  public ProcessExecutor redirectOutputAsDebug(String name) {
    return redirectOutputAsDebug(getCallerLogger(name));
  }

  /**
   * Logs the process' output to a {@link Logger} of the caller class using <code>info</code> level.
   * @return This process executor.
   */
  public ProcessExecutor redirectOutputAsInfo() {
    return redirectOutputAsInfo(getCallerLogger(null));
  }

  /**
   * Logs the process' output to a {@link Logger} of the caller class using <code>debug</code> level.
   * @return This process executor.
   */
  public ProcessExecutor redirectOutputAsDebug() {
    return redirectOutputAsDebug(getCallerLogger(null));
  }

  /**
   * Logs the process' error to a given {@link Logger} with <code>info</code> level.
   * @return This process executor.
   */
  public ProcessExecutor redirectErrorAsInfo(Logger log) {
    return redirectError(new Slf4jInfoOutputStream(log));
  }

  /**
   * Logs the process' error to a given {@link Logger} with <code>debug</code> level.
   * @return This process executor.
   */
  public ProcessExecutor redirectErrorAsDebug(Logger log) {
    return redirectError(new Slf4jDebugOutputStream(log));
  }

  /**
   * Logs the process' error to a {@link Logger} with given name using <code>info</code> level.
   * @return This process executor.
   */
  public ProcessExecutor redirectErrorAsInfo(String name) {
    return redirectErrorAsInfo(getCallerLogger(name));
  }

  /**
   * Logs the process' error to a {@link Logger} with given name using <code>debug</code> level.
   * @return This process executor.
   */
  public ProcessExecutor redirectErrorAsDebug(String name) {
    return redirectErrorAsDebug(getCallerLogger(name));
  }

  /**
   * Logs the process' error to a {@link Logger} of the caller class using <code>info</code> level.
   * @return This process executor.
   */
  public ProcessExecutor redirectErrorAsInfo() {
    return redirectErrorAsInfo(getCallerLogger(null));
  }

  /**
   * Logs the process' error to a {@link Logger} of the caller class using <code>debug</code> level.
   * @return This process executor.
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
   * Sets the process destroyer to be notified when the process starts and stops.
   * @param destroyer helper for destroying all processes on certain event such as VM exit (maybe <code>null</code>).
   *
   * @return This process executor.
   */
  public ProcessExecutor destroyer(ProcessDestroyer destroyer) {
    return listener(destroyer == null ? null : new DestroyerListenerAdapter(destroyer));
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
   * Unregister all existing process event handlers.
   *
   * @return This process executor.
   */
  public ProcessExecutor clearListeners() {
    listeners.clear();
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
    ExecutorService service = Executors.newSingleThreadScheduledExecutor();
    Future<ProcessResult> future = service.submit(task);
    // Previously submitted tasks are executed but no new tasks will be accepted.
    service.shutdown();
    return new StartedProcess(task.getProcess(), future);
  }

  /**
   * Start the process and its stream handlers.
   *
   * @return process the started process.
   * @throws IOException the process or its stream handlers couldn't start (in the latter case we also destroy the process).
   */
  private WaitForProcess startInternal() throws IOException {
    // Invoke listeners - they can modify this executor
    listeners.beforeStart(this);

    if (builder.command().isEmpty())
      throw new IllegalStateException("Command has not been set.");
    validateStreams(streams, readOutput);

    log.debug("Executing {}...", builder.command());
    Process process;
    try {
      process = builder.start();
    }
    catch (IOException e) {
      log.error("Could not start process:", e);
      throw e;
    }
    log.debug("Started {}", process);

    if (readOutput) {
      PumpStreamHandler pumps = (PumpStreamHandler) streams;
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      return startInternal(process, redirectOutputAlsoTo(pumps, out), out);
    }
    else {
      return startInternal(process, streams, null);
    }
  }

  private WaitForProcess startInternal(Process process, ExecuteStreamHandler streams, ByteArrayOutputStream out) throws IOException {
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
    Set<Integer> exitValues = allowedExitValues == null ? null : new HashSet<Integer>(allowedExitValues);
    WaitForProcess result = new WaitForProcess(process, exitValues, streams, out, listeners.clone());
    // Invoke listeners - changing this executor does not affect the started process any more
    listeners.afterStart(process, this);
    return result;
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
      // Use daemon thread as we don't want to postpone the shutdown
      // If #destroyOnExit() is used we wait for the process to be destroyed anyway
      final String name = "WaitForProcess-" + task.getProcess().toString();
      ExecutorService service = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
        public Thread newThread(Runnable r) {
          Thread t = new Thread(r, name);
          t.setDaemon(true);
          return t;
        }
      });
      try {
        result = service.submit(task).get(timeout, timeoutUnit);
      }
      catch (ExecutionException e) {
        Throwable c = e.getCause();
        if (c instanceof IOException)
          throw (IOException) c;
        if (c instanceof InterruptedException)
          throw (InterruptedException) c;
        if (c instanceof InvalidExitValueException)
          throw (InvalidExitValueException) c;
        throw new IllegalStateException("Error occured while waiting for process to finish:", c);
      }
      catch (TimeoutException e) {
        log.debug("{} is running too long", task);
        throw e;
      }
      finally {
        // Interrupt the task if it's still running and release the ExecutorService's resources
        service.shutdownNow();
      }
    }
    return result;
  }

}
