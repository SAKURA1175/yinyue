package com.yinyue.service;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class SourceSeparationService {

    private static final Type MAP_TYPE = new TypeToken<Map<String, Object>>() { }.getType();

    private final Gson gson = new Gson();

    @Value("${app.separation.python-command:${app.analysis.python-command:.venv-audio/Scripts/python.exe}}")
    private String pythonCommand;

    @Value("${app.separation.script-path:./scripts/separate_audio.py}")
    private String scriptPath;

    @Value("${app.separation.output-root:./runtime/separated}")
    private String outputRoot;

    @Value("${app.separation.timeout-seconds:180}")
    private long timeoutSeconds;

    @Value("${app.separation.model:htdemucs}")
    private String model;

    @Value("${app.separation.device:auto}")
    private String device;

    public Map<String, Object> separate(Path audioFile) {
        return separate(audioFile, null);
    }

    public Map<String, Object> separate(Path audioFile, String twoStems) {
        String resolvedPython = PythonScriptSupport.resolveCommand(pythonCommand);
        Path resolvedScript = PythonScriptSupport.resolvePath(scriptPath);
        Path resolvedOutputRoot = PythonScriptSupport.resolvePath(outputRoot);

        if (!Files.exists(resolvedScript)) {
            throw new IllegalStateException("音频分轨脚本不存在: " + resolvedScript);
        }

        List<String> command = new ArrayList<>();
        command.add(resolvedPython);
        command.add(resolvedScript.toString());
        command.add(audioFile.toAbsolutePath().normalize().toString());
        command.add("--output-root");
        command.add(resolvedOutputRoot.toString());
        command.add("--model");
        command.add(model);
        command.add("--device");
        command.add(device);
        if (twoStems != null && !twoStems.isBlank()) {
            command.add("--two-stems");
            command.add(twoStems);
        }

        return PythonScriptSupport.runJsonCommand(command, timeoutSeconds, gson, MAP_TYPE, "音频分轨失败: ");
    }
}
