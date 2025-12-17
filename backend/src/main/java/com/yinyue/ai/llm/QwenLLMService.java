package com.yinyue.ai.llm;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * ======================================================================================
 * 类名：QwenLLMService (通义千问大模型服务 - Spring AI 版)
 * 
 * 作用：这个类就像是一个“翻译官”兼“分析师”。
 * 它的工作是把我们听到的歌词、歌名告诉阿里云的超级大脑（通义千问），
 * 让超级大脑帮我们分析这首歌讲了什么故事，是什么情绪，然后写一段用来画画的描述语。
 * 
 * 【更新说明】：
 * 以前我们是自己手写信（HTTP请求）寄给阿里云，很麻烦。
 * 现在我们升级了，请了一个专业的秘书（Spring AI），它能帮我们自动处理所有繁琐的信件往来。
 * ======================================================================================
 */
@Service // @Service 的意思就是告诉 Spring 框架：“嘿，我是一个很有用的服务类，请在程序启动的时候自动把我创建出来，方便别人直接使用我。”
public class QwenLLMService {

    // @Value 的意思是：去配置文件(application.yml)里找一个叫 spring.ai.openai.api-key 的东西。
    // 我们主要用它来检查是不是没配置 key，如果没配置，就用假数据，防止报错。
    @Value("${spring.ai.openai.api-key:}")
    private String apiKey;

    // ChatClient 是 Spring AI 提供的超级秘书。
    // 我们只要告诉它“帮我问问这个”，它就会自动帮我们联系阿里云，把结果拿回来。
    // 比以前那个“邮递员”(RestTemplate) 更加智能、更加高级。
    private final ChatClient chatClient;

    // 构造函数：当这个类被创建的时候，Spring 会自动把 ChatClient.Builder（秘书招聘官）塞进来。
    // 我们用招聘官现场雇佣一个专属的 ChatClient。
    public QwenLLMService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    /**
     * 方法名：analyzeMusicLyrics (分析歌词)
     * 
     * 作用：这是给外面人调用的主入口。
     * 你给我歌词、歌名、歌手，我给你返回一个详细的分析结果。
     * 
     * @param lyrics 歌词 (比如："简单点，说话的方式简单点...")
     * @param title 歌名 (比如："演员")
     * @param artist 歌手 (比如："薛之谦")
     * @return 返回一段 JSON 格式的文字，里面包含了 AI 觉得这首歌是什么主题、什么心情。
     */
    public String analyzeMusicLyrics(String lyrics, String title, String artist) {
        // 第一步：先把这些零散的信息（歌词、歌名）拼凑成一段完整的话（Prompt）。
        // 就像是给 AI 写了一封详细的求助信。
        String prompt = buildMusicAnalysisPrompt(lyrics, title, artist);
        
        // 第二步：把这封信寄出去，等待 AI 的回信。
        return callQwenLLM(prompt);
    }

    /**
     * 方法名：callQwenLLM (呼叫通义千问)
     * 
     * 作用：这是真正干脏活累活的地方。
     * 以前这里写了几十行代码来打包数据，现在有了 Spring AI，几行代码就搞定了！
     * 
     * @param prompt 发送给 AI 的提示词
     * @return AI 回复的内容
     */
    public String callQwenLLM(String prompt) {
        // 先检查一下钥匙（API Key）有没有带，或者是不是假的。
        // 如果没带钥匙，或者钥匙是假的，那就别去麻烦阿里云了，直接自己编一个结果（Mock数据）返回去。
        // 这样即使没有配置好，程序也不会报错崩溃。
        if (apiKey == null || apiKey.isEmpty() || apiKey.startsWith("your_")) {
            return mockAnalysis(prompt);
        }

        // try...catch 的意思是：尝试做一件危险的事情（上网请求）。
        // 如果中间出了任何问题（断网了、服务器炸了），就会跳到 catch 里面去处理，而不是让整个程序死机。
        try {
            // =================================================================
            // Spring AI 的魔法时刻！
            // 
            // chatClient.prompt() -> 嘿，秘书，我要提问了。
            // .user(prompt)       -> 问题的内容是这个（prompt）。
            // .call()             -> 赶紧打个电话给阿里云。
            // .content()          -> 只要把对方说的话（内容）告诉我，其他的客套话都不要。
            // =================================================================
            String response = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

            // 如果回信是空的，那说明出大问题了。
            if (response == null || response.isEmpty()) {
                throw new RuntimeException("AI 没理我们，响应是空的");
            }
            
            return response;

        } catch (Exception e) {
            // 如果整个过程任何地方出错了，就会来到这里。
            System.err.println("AI 调用出错了: " + e.getMessage());
            e.printStackTrace(); // 打印详细的错误日记，方便程序员查错
            // 出错了也不怕，我们有备用方案（Mock数据），保证用户看到的界面是正常的。
            return mockAnalysis(prompt);
        }
    }
    
    /**
     * 方法名：mockAnalysis (模拟分析)
     * 
     * 作用：这是一个“备胎”。当真正的 AI 坏掉的时候，它就顶上去。
     * 它会根据提示词里有没有“悲伤”或“快乐”这些词，简单地猜一下，返回一个假装是 AI 分析出来的结果。
     * 
     * @param prompt 提示词
     * @return 伪造的分析结果 JSON
     */
    private String mockAnalysis(String prompt) {
        // 默认情况：假装这首歌很安静
        String theme = "【AI服务连接失败】默认主题：宁静致远";
        String mood = "平和";
        String style = "极简主义";
        String colors = "[\"#CCCCCC\", \"#999999\", \"#666666\"]"; // 灰色系
        String imagePrompt = "minimalist landscape, calm atmosphere, fog, soft light, high quality";
        
        // 如果歌词里有“悲伤”或者“分手”
        if (prompt.contains("悲伤") || prompt.contains("分手")) {
            theme = "【AI服务连接失败】默认主题：伤感回忆";
            mood = "忧郁";
            style = "黑白摄影";
            colors = "[\"#000000\", \"#333333\", \"#666666\"]"; // 黑色系
            imagePrompt = "Black and white photography, sad atmosphere, rain on window, lonely figure, high contrast";
        } 
        // 如果歌词里有“快乐”或者“阳光”
        else if (prompt.contains("快乐") || prompt.contains("阳光")) {
             theme = "阳光活力";
             mood = "欢快";
             style = "波普艺术";
             colors = "[\"#FFD700\", \"#FF6B6B\", \"#4ECDC4\"]"; // 彩色系
             imagePrompt = "Vibrant pop art style, sunshine, happy people, colorful background, energetic";
        }
        
        // 把上面猜的结果拼成一个 JSON 字符串返回去
        return String.format(
            "{\n" +
            "  \"theme\": \"%s\",\n" +
            "  \"mood\": \"%s\",\n" +
            "  \"visual_style\": \"%s\",\n" +
            "  \"colors\": %s,\n" +
            "  \"image_prompt_en\": \"%s\"\n" +
            "}",
            theme, mood, style, colors, imagePrompt
        );
    }

    /**
     * 方法名：buildMusicAnalysisPrompt (构建提示词)
     * 
     * 作用：把歌词、歌名这些原材料，加工成一段 AI 能听懂的指令。
     * 
     * @return 拼好的一长串字符串
     */
    private String buildMusicAnalysisPrompt(String lyrics, String title, String artist) {
        // StringBuilder 就像是一个可以自动变长的写字板，比用 String 相加效率更高。
        StringBuilder prompt = new StringBuilder();
        
        // 开始给 AI 洗脑，设定人设
        prompt.append("你是一个专业的音乐分析师和视觉设计师。\n\n");
        prompt.append("请根据以下音乐信息，生成一份详细的专辑封面设计建议。\n\n");
        
        // 喂给 AI 数据
        prompt.append("【音乐信息】\n");
        prompt.append("歌名: ").append(title).append("\n");
        prompt.append("歌手: ").append(artist).append("\n");
        prompt.append("歌词:\n").append(lyrics).append("\n\n");
        
        // 告诉 AI 我们要什么格式的回答
        prompt.append("【要求】\n");
        prompt.append("请用 JSON 格式返回以下信息（不要包含任何其他文本）:\n");
        prompt.append("{\n");
        prompt.append("  \"theme\": \"主题\",\n");
        prompt.append("  \"mood\": \"氛围\",\n");
        prompt.append("  \"visual_style\": \"视觉风格\",\n");
        prompt.append("  \"colors\": [\"颜色1\", \"颜色2\", \"颜色3\"],\n");
        prompt.append("  \"image_prompt_en\": \"英文图片生成提示词\"\n");
        prompt.append("}\n");

        return prompt.toString(); // 把写字板上的字变成最终的字符串
    }

    /**
     * 方法名：testConnection (测试连接)
     * 
     * 作用：试探一下 AI 还在不在。
     * 发一个最简单的“你好”，看它回不回复。
     */
    public boolean testConnection() {
        try {
            String testPrompt = "你好，请回复 OK";
            String response = callQwenLLM(testPrompt);
            // 如果回复不是空的，说明连接成功
            return response != null && !response.isEmpty();
        } catch (Exception e) {
            // 只要报错，就说明连接失败
            return false;
        }
    }
}
