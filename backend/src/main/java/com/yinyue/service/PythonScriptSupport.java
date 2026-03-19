package com.yinyue.service;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

final class PythonScriptSupport {

    private PythonScriptSupport() {
    }

    static String resolveCommand(String command) {
        if (command == null || command.isBlank()) {
            throw new IllegalArgumentException("Python 命令不能为空");
        }

        if (!command.contains("/") && !command.contains("\\") && !command.endsWith(".exe")) {
            return command;
        }

        return resolvePath(command).toString();
    }

    static Path resolvePath(String configuredPath) {
        Path path = Paths.get(configuredPath);
        if (path.isAbsolute()) {
            return path.normalize();
        }

        Path workingDirectory = Paths.get("").toAbsolutePath().normalize();
        List<Path> candidates = List.of(
                workingDirectory.resolve(path).normalize(),
                workingDirectory.resolve("..").resolve(path).normalize()
        );

        for (Path candidate : candidates) {
            if (Files.exists(candidate)) {
                return candidate;
            }
        }

        return candidates.get(0);
    }

    static <T> T runJsonCommand(List<String> command,
                                long timeoutSeconds,
                                Gson gson,
                                Type outputType,
                                String failureMessage) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append(System.lineSeparator());
                }
            }

            boolean completed = process.waitFor(Duration.ofSeconds(timeoutSeconds).toMillis(), TimeUnit.MILLISECONDS);
            if (!completed) {
                process.destroyForcibly();
                throw new IllegalStateException(failureMessage + "超时");
            }

            if (process.exitValue() != 0) {
                throw new IllegalStateException(failureMessage + output.toString().trim());
            }

            T result = gson.fromJson(output.toString(), outputType);
            if (result == null) {
                throw new IllegalStateException(failureMessage + "结果为空");
            }
            return result;
        } catch (Exception e) {
            throw new RuntimeException(failureMessage + e.getMessage(), e);
        }
    }
}
