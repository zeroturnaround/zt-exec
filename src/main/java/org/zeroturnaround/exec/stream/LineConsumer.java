package org.zeroturnaround.exec.stream;

/**
 * This is equivalent to {@code java.util.function.Consumer} while staying compatible with
 * Java versions earlier than 8.
 */
public interface LineConsumer {
  void accept(String line);
}
