package org.zeroturnaround.exec;

import java.util.Set;
import java.util.concurrent.TimeoutException;

/**
 * Helper for checking the exit code of the finished process.
 */
class InvalidExitUtil {

  /**
   * In case {@link InvalidExitValueException} or {@link TimeoutException} is thrown and we have read the process output
   * we include the output up to this length in the error message. If the output is longer we truncate it.
   */
  private static final int MAX_OUTPUT_SIZE_IN_ERROR_MESSAGE = 5000;

  /**
   * Check the process exit value.
   */
  public static void checkExit(ProcessAttributes attributes, ProcessResult result) {
    Set<Integer> allowedExitValues = attributes.getAllowedExitValues();
    if (allowedExitValues != null && !allowedExitValues.contains(result.getExitValue())) {
      StringBuilder sb = new StringBuilder();
      sb.append("Unexpected exit value: ").append(result.getExitValue());
      sb.append(", allowed exit values: ").append(allowedExitValues);
      addExceptionMessageSuffix(attributes, sb, result.hasOutput() ? result.getOutput() : null);
      throw new InvalidExitValueException(sb.toString(), result);
    }
  }

  public static void addExceptionMessageSuffix(ProcessAttributes attributes, StringBuilder sb, ProcessOutput output) {
    sb.append(", executed command ").append(attributes.getCommand());
    if (attributes.getDirectory() != null) {
      sb.append(" in directory ").append(attributes.getDirectory());
    }
    if (!attributes.getEnvironment().isEmpty()) {
      sb.append(" with environment ").append(attributes.getEnvironment());
    }
    if (output != null) {
      int length = output.getBytes().length;
      String out = output.getString();
      if (out.length() <= MAX_OUTPUT_SIZE_IN_ERROR_MESSAGE) {
        sb.append(", output was ").append(length).append(" bytes:\n").append(out.trim());
      }
      else {
        sb.append(", output was ").append(length).append(" bytes (truncated):\n");
        int halfLimit = MAX_OUTPUT_SIZE_IN_ERROR_MESSAGE / 2;
        sb.append(out.substring(0, halfLimit)).append("\n...\n").append(out.substring(out.length() - halfLimit).trim());
      }
    }
  }

}
