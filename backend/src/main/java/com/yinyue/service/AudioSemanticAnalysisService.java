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
public class AudioSemanticAnalysisService {

    private static final Type MAP_TYPE = new TypeToken<Map<String, Object>>() { }.getType();

    private final Gson gson = new Gson();

    @Value("${app.semantic.python-command:${app.analysis.python-command:.venv-audio/Scripts/python.exe}}")
    private String pythonCommand;

    @Value("${app.semantic.script-path:./scripts/extract_music_semantics.py}")
    private String scriptPath;

    @Value("${app.semantic.timeout-seconds:180}")
    private long timeoutSeconds;

    @Value("${app.semantic.model-id:m-a-p/MERT-v1-95M}")
    private String modelId;

    @Value("${app.semantic.device:auto}")
    private String device;

    @Value("${app.semantic.max-seconds:30}")
    private int maxSeconds;

    public Map<String, Object> analyze(Path audioFile) {
        String resolvedPython = PythonScriptSupport.resolveCommand(pythonCommand);
        Path resolvedScript = PythonScriptSupport.resolvePath(scriptPath);

        if (!Files.exists(resolvedScript)) {
            throw new IllegalStateException("音乐语义分析脚本不存在: " + resolvedScript);
        }

        List<String> command = new ArrayList<>();
        command.add(resolvedPython);
        command.add(resolvedScript.toString());
        command.add(audioFile.toAbsolutePath().normalize().toString());
        command.add("--model-id");
        command.add(modelId);
        command.add("--device");
        command.add(device);
        command.add("--max-seconds");
        command.add(String.valueOf(maxSeconds));

        return PythonScriptSupport.runJsonCommand(command, timeoutSeconds, gson, MAP_TYPE, "音乐语义分析失败: ");
    }
}
