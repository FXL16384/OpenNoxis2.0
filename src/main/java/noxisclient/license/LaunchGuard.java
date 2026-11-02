package noxisclient.license;

import java.lang.management.ManagementFactory;
import java.util.Locale;

public final class LaunchGuard {
   private static final String[] BLOCKED_OPTIONS = new String[]{"-javaagent", "-agentlib", "-agentpath", "-xrunjdwp", "-xdebug", "-xx:+startattachlistener"};
   private static final String[] JVM_OPTION_ENV_KEYS = new String[]{"JAVA_TOOL_OPTIONS", "_JAVA_OPTIONS", "JDK_JAVA_OPTIONS"};

   private LaunchGuard() {
   }

   public static void verifySupportedRuntime() {
      for (String argument : ManagementFactory.getRuntimeMXBean().getInputArguments()) {
         if (isUnsupportedArgument(argument)) {
            reject();
         }
      }

      for (String envKey : JVM_OPTION_ENV_KEYS) {
         if (containsUnsupportedOption(System.getenv(envKey))) {
            reject();
         }
      }
   }

   public static void verifyNoJavaAgent() {
      verifySupportedRuntime();
   }

   static boolean containsUnsupportedOption(String value) {
      if (value != null && !value.isBlank()) {
         for (String argument : value.split("\\s+")) {
            if (isUnsupportedArgument(argument)) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   static boolean isUnsupportedArgument(String argument) {
      if (argument == null) {
         return false;
      } else {
         String normalized = argument.trim().toLowerCase(Locale.ROOT);

         while (normalized.startsWith("\"") || normalized.startsWith("'")) {
            normalized = normalized.substring(1);
         }

         for (String option : BLOCKED_OPTIONS) {
            if (normalized.equals(option) || normalized.startsWith(option + ":") || normalized.startsWith(option + "=")) {
               return true;
            }
         }

         return false;
      }
   }

   private static void reject() {
      throw new IllegalStateException("Unsupported JVM launch option.");
   }
}
