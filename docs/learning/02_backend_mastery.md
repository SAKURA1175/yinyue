# 🧠 02. 后端逻辑：Spring Boot 服务大脑

如果说前端是"皮肤"，后端就是"大脑"和"内脏"。它负责思考（逻辑）、记忆（数据库）和指挥（API）。
本项目使用 **Java** 和 **Spring Boot** 框架。

---

## 1. 三层架构 (Controller - Service - Repository)

这是后端最经典的设计模式，就像餐厅的分工：

1.  **Controller (前台服务员)**: 
    *   **职责**: 接待客人 (前端请求)，检查菜单 (参数校验)，把单子给厨房。
    *   **代码特征**: 带有 `@RestController`, `@PostMapping`。
    *   **项目案例**: `FileUploadController.java`
        ```java
        @PostMapping("/upload") // 只要有人访问 /upload，就进这里
        public ResponseEntity<String> upload(@RequestParam("file") MultipartFile file) {
            // ... 交给 Service 处理
        }
        ```

2.  **Service (厨房大厨)**:
    *   **职责**: 真正的干活。切菜、炒菜、摆盘（业务逻辑）。
    *   **代码特征**: 带有 `@Service`。
    *   **项目案例**: `FileUploadService.java`
        *   它负责判断文件是不是空的。
        *   它负责给文件起个新名字 (UUID)。
        *   它负责把文件保存到 `D:\yinyue\backend\uploads` 文件夹里。

3.  **Repository (仓库管理员)**:
    *   **职责**: 和数据库打交道。存数据、取数据。
    *   **代码特征**: 继承 `JpaRepository`。
    *   **项目案例**: `MusicTrackRepository.java`
        *   当 Service 说 "保存这条记录"，Repository 就自动生成 SQL 语句 `INSERT INTO ...` 写入 MySQL。

---

## 2. 依赖注入 (Dependency Injection / Autowired)

**理论**：
在 Java 里，对象 A 需要用到对象 B，通常要 `new B()`。但在 Spring Boot 里，你不需要自己 new，你只需要喊一声 "我需要 B"，Spring 就会自动把 B 送到你手里。这就叫 **依赖注入**。

**项目实战**：
看看 `AIController.java`：
```java
@RestController
public class AIController {

    @Autowired // 这里的 @Autowired 就是在喊 "给我一个 QwenLLMService！"
    private QwenLLMService qwenLLMService;

    // 然后下面就可以直接用了，不用自己 new
    qwenLLMService.callQwenLLM(prompt);
}
```

---

## 3. 配置文件 (application.yml)

**理论**：
不想把密码、端口号写死在代码里？那就写在配置文件里。

**项目实战**：
打开 `src/main/resources/application.yml`。
*   `server.port: 8080`: 决定了后端监听哪个端口。
*   `app.ai.qwen.api-key`: 你的阿里云 Key。代码里通过 `@Value("${app.ai.qwen.api-key}")` 来读取它。

---

## 🎯 课后作业 (后端篇)

1.  **追踪请求**：想象你是一个 HTTP 请求。从 `FileUploadController` 进来，怎么流转到 `FileUploadService`，最后文件是怎么落地的？
2.  **修改端口**：尝试把 `application.yml` 里的 `8080` 改成 `9090`，重启后端。前端还能连上吗？（提示：前端代码里写死了 8080，所以前端也要改）。
3.  **看懂日志**：重启后端时，盯着控制台。试着从一大堆日志里找到 "Tomcat started on port 8080" 这句话，这是后端启动成功的金标准。
