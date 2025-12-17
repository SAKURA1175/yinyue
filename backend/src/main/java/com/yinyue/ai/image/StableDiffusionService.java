package com.yinyue.ai.image;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.google.gson.Gson;

/**
 * ======================================================================================
 * 【类名】StableDiffusionService (AI 绘图服务)
 * 
 * 【核心作用】
 * 这个类是项目中"画图"功能的核心引擎。它就像一个"接线员"或"经纪人"，负责：
 * 1️⃣ 从前端/Controller 接收用户的画图需求（文字描述、参数配置）
 * 2️⃣ 把这些需求打包、整理、编码成 HTTP 请求
 * 3️⃣ 发送给本地运行的 Stable Diffusion 服务（一个真正的 AI 画图引擎）
 * 4️⃣ 等待画图完成
 * 5️⃣ 把生成的图片（Base64格式）返回给调用者
 * 
 * 【简单类比】
 * 如果你想让一个画家画一幅画：
 * - "用户" = 你这个有创意的人
 * - "StableDiffusionService" = 画廊老板，帮你和画家沟通
 * - "Stable Diffusion" = 真正的画家
 * 
 * 【工作流程】
 * 用户描述需求 → Controller 调用本类 → 本类连接 SD → SD画图 → 返回图片
 * 
 * 【为什么需要这个类】
 * - 隐藏 SD API 的复杂细节，提供更易用的接口
 * - 对参数进行"美化"处理（加高质量修饰词）
 * - 集中管理 SD 连接配置
 * - 便于日志记录、错误处理、性能优化
 * - 支持多种不同的生图功能（通用生图、专业封面生图等）
 * ======================================================================================
 */
@Service
public class StableDiffusionService {

    // 【日志记录器】记录程序运行过程中的关键事件
    // 用途：追踪哪个图片在什么时间生成、耗时多久、是否出错
    // 这对调试和性能分析非常重要
    private static final Logger log = LoggerFactory.getLogger(StableDiffusionService.class);

    // 【Stable Diffusion 服务的网络地址】
    // 这是 SD WebUI 暴露出来的 API 端点（"端点"就是网络接口的地址）
    // 读取来源：application.yml 中的 app.ai.stable-diffusion.endpoint
    // 默认值：http://127.0.0.1:7860（本机，端口 7860）
    // 这个值可以改成远程服务器的地址，比如 http://192.168.1.100:7860
    @Value("${app.ai.stable-diffusion.endpoint:http://127.0.0.1:7860}")
    private String sdEndpoint;

    // 【Spring 提供的 HTTP 请求工具】
    // 用途：这是发送 HTTP POST/GET 请求的"邮递员"，用来和 SD 通讯
    // 特点：自动处理 HTTP 细节，允许我们用 Java 简洁地写网络请求
    private final RestTemplate restTemplate;
    
    // 【JSON 数据转换工具】
    // 用途：把 Java 对象（如 HashMap）转换成 JSON 字符串，或反之
    // 为什么需要？因为网络传输只能用文本格式（JSON），不能直接传 Java 对象
    private final Gson gson;

    // 构造函数：初始化工具
    public StableDiffusionService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.gson = new Gson();
    }

    /**
     * 【类名】ImageGenerationOptions (绘图参数包)
     * 
     * 【核心作用】
     * 这是一个用来装参数的"大礼包"。因为画一幅画需要设置很多东西（画什么、不画什么、画多大、画几步、
     * 用什么笔刷...），如果一个个传参数太麻烦了，所以我们把它们都打包在这个类里面。
     * 
     * 【设计模式】Builder 模式
     * 这里用到了一个叫"Builder模式"的高级写法。
     * 你可以把它想象成在赛百味（Subway）点餐：
     * "我要一个三明治(builder)，加火腿(prompt)，加芝士(steps)，不要洋葱(negativePrompt)... 完成(build)！"
     * 
     * 【优点】
     * ✅ 可读性强：一目了然地看到所有参数
     * ✅ 灵活性高：可以只设置需要的参数，其他参数用默认值
     * ✅ 链式调用：.prompt(...).steps(...).build() 非常优美
     * ✅ 类型安全：不用传一大堆 String/int，容易出错
     */
    public static class ImageGenerationOptions {
        // 【正向提示词】 PROMPT - 告诉AI你要画什么
        // 示例："a beautiful girl, red dress, sunset, oil painting style"
        // 语言：推荐用英文，AI 对英文理解更准确
        // 长度：无限制，但太长了 AI 可能混淆重点
        private String prompt;
    
        // 【反向提示词】 NEGATIVE PROMPT - 告诉AI你不要画什么
        // 示例："ugly, blurry, low quality, watermark, text"
        // 用途：排除不想要的元素，比如不要多只手、不要扭曲变形
        // 技巧：负向提示词对生图质量影响很大，值得精心设计
        private String negativePrompt = "";
    
        // 【图片宽度】WIDTH - 生成的图片有多宽（像素）
        // 常见值：512, 768, 1024
        // 注意：Stable Diffusion 最擅长 512x512，宽高应该是 64 的倍数
        // 越大内存占用越多，生成越慢，成本越高
        private int width = 512;
    
        // 【图片高度】HEIGHT - 生成的图片有多高（像素）
        // 常见值：512, 768, 1024
        // 专业建议：封面用 512x512（正方形），容易出好效果
        private int height = 512;
    
        // 【迭代步数】STEPS - 画家要画多少"笔"
        // 范围：1-50（有的模型支持更多）
        // 值越大：图片越细致，但也越慢，成本越高
        // 常用值：20-30（平衡质量和速度），50+（追求完美质量）
        // 经验：20 步已经不错了，30 步很好，50 步可能过度
        private int steps = 20;
    
        // 【相关性系数】CFG SCALE（Classifier Free Guidance）- 画家多听你的话
        // 范围：1-20（超过 20 可能出现伪影）
        // 值的含义：
        //   - 1.0：完全忽视你的描述，AI 随意创作（很疯狂）
        //   - 7.0-8.0：平衡（推荐，既听话又有创意）
        //   - 15-20：非常听话（严格按你的要求，可能不自然）
        // 调试建议：先用 7.0，如果出来的图不像描述就增大到 10-12
        private double cfgScale = 7.0;
    
        // 【采样器名称】SAMPLER NAME - 画画的算法/风格
        // 常见采样器：
        //   - Euler：经典，速度快，质量中等
        //   - Euler a (ancestral)：有变化性，有时出现意外好效果
        //   - DPM++ 2M Karras：较新，质量好，速度慢
        //   - DDIM：很老，不推荐
        // 选择建议：试试 DPM++ 2M Karras，如果慢就改成 Euler
        private String samplerName = "Euler a";
    
        // 【随机种子】SEED - 控制随机数生成器的起点
        // 值为 -1：完全随机（每次生成都不同）
        // 值为固定数字（如 12345）：每次生成完全相同的图（用于复现结果）
        // 用途：如果喜欢某次生成的效果，记住种子号，下次就能精修
        private long seed = -1;
    
        // 【高清修复开关】ENABLE HIRES - 是否启用高清修复
        // false（默认）：直接生成 512x512
        // true：先生成 512x512，再放大和精修
        // 优点：细节更好，特别是脸部
        // 代价：要额外花费 50% 的时间和计算资源
        private boolean enableHires = false;
    
        // 【高清放大倍数】HR SCALE - 放大几倍
        // 常见值：1.5（放大 1.5 倍，512x512 变 768x768）、2.0（放大 2 倍，512x512 变 1024x1024）
        // 建议：1.5 最平衡（质量提升明显但不过度），2.0 质量最好但很慢
        private double hrScale = 2.0;
    
        // 【高清放大算法】HR UPSCALER - 放大图片用什么算法
        // 常见值：
        //   - "Latent"：速度快，效果还行（推荐）
        //   - "ESRGAN"：质量最好但较慢
        //   - "REAL-ESRGAN"：效果和速度都不错
        // 通常不用改，用默认值 Latent 即可
        private String hrUpscaler = "Latent";
    
        // 【高清重绘幅度】DENOISING STRENGTH - 精修的时候改动多大
        // 范围：0-1
        // 值的含义：
        //   - 0.0：完全不改，原图保留 100%（等于没精修）
        //   - 0.5：一半一半（平衡改动和保留）
        //   - 0.7-0.9：较大改动（推荐用 0.7，细节精修效果好）
        //   - 1.0：完全重新生成（几乎等于从头生成）
        // 建议：保持默认 0.7 即可
        private double denoisingStrength = 0.7;

        // =================================================================
        // 下面这些是 Getter 方法，就是让别人能看到这些参数的值
        // =================================================================
        public String getPrompt() { return prompt; }
        public String getNegativePrompt() { return negativePrompt; }
        public int getWidth() { return width; }
        public int getHeight() { return height; }
        public int getSteps() { return steps; }
        public double getCfgScale() { return cfgScale; }
        public String getSamplerName() { return samplerName; }
        public long getSeed() { return seed; }
        public boolean isEnableHires() { return enableHires; }
        public double getHrScale() { return hrScale; }
        public String getHrUpscaler() { return hrUpscaler; }
        public double getDenoisingStrength() { return denoisingStrength; }

        // 创建一个点餐员 (Builder)
        public static Builder builder() {
            return new Builder();
        }

        // 点餐员类
        public static class Builder {
            private ImageGenerationOptions options = new ImageGenerationOptions();

            // 每一个方法都是在设置一个参数，然后返回自己，这样就可以一直点下去（链式调用）。
            public Builder prompt(String prompt) { options.prompt = prompt; return this; }
            public Builder negativePrompt(String negativePrompt) { options.negativePrompt = negativePrompt; return this; }
            public Builder width(int width) { options.width = width; return this; }
            public Builder height(int height) { options.height = height; return this; }
            public Builder steps(int steps) { options.steps = steps; return this; }
            public Builder cfgScale(double cfgScale) { options.cfgScale = cfgScale; return this; }
            public Builder samplerName(String samplerName) { options.samplerName = samplerName; return this; }
            public Builder seed(long seed) { options.seed = seed; return this; }
            public Builder enableHires(boolean enableHires) { options.enableHires = enableHires; return this; }
            public Builder hrScale(double hrScale) { options.hrScale = hrScale; return this; }
            public Builder hrUpscaler(String hrUpscaler) { options.hrUpscaler = hrUpscaler; return this; }
            public Builder denoisingStrength(double denoisingStrength) { options.denoisingStrength = denoisingStrength; return this; }

            // 最后打包，把设置好的参数包交给你
            public ImageGenerationOptions build() {
                return options;
            }
        }
    }

    /**
     * 方法名：generateAlbumCover (生成专业的专辑封面)
     * 
     * 作用：这是专门为专辑封面定制的方法。
     * 它不仅仅是把你的描述发给 AI，还会偷偷在后面加上一堆“魔法咒语”（高质量修饰词）。
     * 这样即使你说得很简单，画出来的图也会显得很专业、很高清。
     * 
     * @param prompt 基础提示词（比如：一张悲伤的脸）
     * @return 图片的 Base64 编码字符串（可以理解为图片变成了一长串乱码文字）
     */
    public String generateAlbumCover(String prompt) {
        // 1. 加料：在你的描述后面加上“大师级作品”、“8k分辨率”、“电影光效”等词。
        String enhancedPrompt = prompt + ", (masterpiece, best quality, highres:1.2), (official album cover art:1.3), " +
                "8k resolution, highly detailed, complex details, cinematic lighting, sharp focus, " +
                "professional digital painting, trending on artstation, concept art, " +
                "vivid colors, artistic composition";

        // 2. 避坑：告诉 AI 千万不要画什么。比如不要色情(nsfw)、不要低质量、不要水印、不要乱码文字。
        String enhancedNegativePrompt = "(nsfw:1.5), (low quality, normal quality, worst quality:1.5), " +
                "(bad anatomy), (deformed), (blurred), (ugly), (mutation), (disfigured), " +
                "watermark, text, signature, username, logo, artist name, title, " +
                "cropping, out of frame, lowres, jpeg artifacts, grain, noise";

        // 3. 打包参数：使用上面说的 Builder 模式，把所有要求装好。
        ImageGenerationOptions options = ImageGenerationOptions.builder()
                .prompt(enhancedPrompt)
                .negativePrompt(enhancedNegativePrompt)
                .width(512)   // 宽度 512 像素
                .height(512)  // 高度 512 像素
                .steps(40)    // 画 40 笔，比默认的 20 笔更细致
                .cfgScale(7.5)// 听话程度 7.5
                .samplerName("DPM++ 2M Karras") // 用这个采样器画出来的图质感比较好
                .build();
        
        // 4. 调用通用方法去生成
        return generateImage(options);
    }

    /**
     * 方法名：generateImage (通用图片生成 - 底层方法)
     * 
     * 作用：真正的干活方法。它负责把参数包拆开，填进 HTTP 请求里，发给 Stable Diffusion。
     * 
     * @param options 参数包
     * @return 图片 Base64
     */
    public String generateImage(ImageGenerationOptions options) {
        try {
            log.info("开始生成图片，提示词: {}", options.getPrompt());

            // 1. 把我们的参数包（Java对象）转换成 Map，方便后面转成 JSON。
            Map<String, Object> requestPayload = new HashMap<>();
            requestPayload.put("prompt", options.getPrompt());
            requestPayload.put("negative_prompt", options.getNegativePrompt());
            requestPayload.put("steps", options.getSteps());
            requestPayload.put("width", options.getWidth());
            requestPayload.put("height", options.getHeight());
            requestPayload.put("cfg_scale", options.getCfgScale());
            requestPayload.put("sampler_name", options.getSamplerName());
            requestPayload.put("seed", options.getSeed());

            // 如果开启了高清修复，还要加这些参数
            if (options.isEnableHires()) {
                requestPayload.put("enable_hr", true);
                requestPayload.put("hr_scale", options.getHrScale());
                requestPayload.put("hr_upscaler", options.getHrUpscaler());
                requestPayload.put("denoising_strength", options.getDenoisingStrength());
            }

            // 2. 准备发信
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            String jsonPayload = gson.toJson(requestPayload); // 转成 JSON 字符串
            HttpEntity<String> request = new HttpEntity<>(jsonPayload, headers);

            // 3. 发送请求！
            // 记录一下开始时间
            long startTime = System.currentTimeMillis();
            
            // postForObject 向 /sdapi/v1/txt2img 这个接口发送请求
            Map<String, Object> responseBody = restTemplate.postForObject(
                    sdEndpoint + "/sdapi/v1/txt2img",
                    request,
                    Map.class
            );
            
            // 计算一下画了多久
            long duration = System.currentTimeMillis() - startTime;
            log.info("图片生成完成，耗时: {} 毫秒", duration);

            // 4. 检查结果
            if (responseBody == null) {
                log.error("API 响应是空的，可能是 SD 没启动");
                throw new RuntimeException("图片生成失败：API 响应为空");
            }

            // SD 返回的结果里，images 是一个列表（虽然通常只有一张图）。
            List<String> images = (List<String>) responseBody.get("images");
            if (images == null || images.isEmpty()) {
                log.error("API 返回了数据，但是里面没有图片");
                throw new RuntimeException("图片生成失败：未获得图片数据");
            }

            // 返回第一张图片（Base64 字符串）
            return images.get(0);

        } catch (Exception e) {
            // 如果出错了，记录日志并告诉外面
            log.error("图片生成异常: {}", e.getMessage(), e);
            throw new RuntimeException("图片生成失败: " + e.getMessage());
        }
    }

    /**
     * 方法名：generateImage (老方法的兼容版)
     * 
     * 作用：如果有旧代码还在用这种简单的参数调用，这个方法会自动帮它转成新的 Builder 模式。
     * 这叫“向后兼容”，保证旧代码不会报错。
     */
    public String generateImage(String prompt, String negativePrompt, int steps, int width, int height) {
        ImageGenerationOptions options = ImageGenerationOptions.builder()
                .prompt(prompt)
                .negativePrompt(negativePrompt)
                .steps(steps)
                .width(width)
                .height(height)
                .cfgScale(7.5)
                .samplerName("Euler") // 默认用 Euler
                .build();
        return generateImage(options);
    }

    /**
     * 方法名：isServiceAvailable (服务还在吗)
     * 
     * 作用：Ping 一下 Stable Diffusion，看它是不是活着。
     * 就像是打电话问：“喂，你在吗？”
     */
    public boolean isServiceAvailable() {
        try {
            Map response = restTemplate.getForObject(
                    sdEndpoint + "/info", // 访问 info 接口
                    Map.class
            );
            return response != null; // 如果能收到回复，就是活着的
        } catch (Exception e) {
            log.warn("Stable Diffusion 服务好像挂了: {}", e.getMessage());
            return false;
        }
    }
}
