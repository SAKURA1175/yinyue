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
public class AudioFeatureAnalysisService {

    private static final Type MAP_TYPE = new TypeToken<Map<String, Object>>() { }.getType();

    private final Gson gson = new Gson();

    @Value("${app.analysis.python-command:.venv-audio/Scripts/python.exe}")
    private String pythonCommand;

    @Value("${app.analysis.script-path:./scripts/analyze_audio.py}")
    private String scriptPath;

    @Value("${app.analysis.provider:auto}")
    private String provider;

    @Value("${app.analysis.timeout-seconds:60}")
    private long timeoutSeconds;

    public Map<String, Object> analyze(Path audioFile) {
        return analyze(audioFile, null);
    }

    public Map<String, Object> analyze(Path audioFile, Path stemsDir) {
        String resolvedPython = PythonScriptSupport.resolveCommand(pythonCommand);
        Path resolvedScript = PythonScriptSupport.resolvePath(scriptPath);

        if (!Files.exists(resolvedScript)) {
            throw new IllegalStateException("音频分析脚本不存在: " + resolvedScript);
        }

        List<String> command = new ArrayList<>();
        command.add(resolvedPython);
        command.add(resolvedScript.toString());
        command.add(audioFile.toAbsolutePath().normalize().toString());
        command.add("--provider");
        command.add(provider);
        if (stemsDir != null) {
            command.add("--stems-dir");
            command.add(stemsDir.toAbsolutePath().normalize().toString());
        }

        return PythonScriptSupport.runJsonCommand(command, timeoutSeconds, gson, MAP_TYPE, "本地音频分析失败: ");
    }
}
