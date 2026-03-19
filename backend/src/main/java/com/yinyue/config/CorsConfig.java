package com.yinyue.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * ======================================================================================
 * 类名：CorsConfig (跨域配置类)
 * 
 * 作用：这个类就像是“小区门口的保安大爷”。
 * 
 * 为什么需要它？
 * 想象一下，前端网页（比如 React 做的页面）住在 A 小区（端口 5173），
 * 后端服务器（Spring Boot）住在 B 小区（端口 8080）。
 * 
 * 浏览器有一个安全规则叫“同源策略”，默认情况下，A 小区的人不能随便去 B 小区串门拿东西（发请求）。
 * 浏览器会拦住说：“你们不是一家人（端口不同），不能乱跑！”
 * 
 * 这个类的作用就是告诉浏览器：“保安大爷，A 小区（5173）是我的亲戚，放他们进来吧！”
 * ======================================================================================
 */
@Configuration // 告诉 Spring：这是一个配置类，系统启动的时候要先读我。
public class CorsConfig implements WebMvcConfigurer {

    @Value("${app.cors.allowed-origins:http://localhost:5173,http://localhost:5174,http://127.0.0.1:5173,http://127.0.0.1:5174}")
    private String allowedOrigins;

    /**
     * 方法名：addCorsMappings (添加跨域映射规则)
     * 
     * 作用：制定具体的放行规则。
     * 
     * @param registry 注册表，用来登记谁可以进来。
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // addMapping("/api/**") 意思是：只有访问 "/api/" 开头的接口（比如 /api/upload, /api/music），才适用这个规则。
        registry.addMapping("/api/**")
                
                // allowedOrigins: 允许哪些“小区”的人进来。
                // 这里我们允许了本机的前端开发环境（localhost:5173 等）。
                .allowedOrigins(parseAllowedOrigins())
                
                // allowedMethods: 允许他们做什么动作。
                // GET: 查东西
                // POST: 提交东西（比如上传）
                // PUT: 修改东西
                // DELETE: 删除东西
                // OPTIONS: 试探一下（浏览器会先发这个请求问问能不能连）
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                
                // allowedHeaders: 允许带什么“信物”（比如 Token 身份验证信息）。
                // "*" 表示带什么都行。
                .allowedHeaders("*")
                
                // maxAge: 这条规则的有效期。
                // 3600 秒（1小时）内，浏览器不用每次都问保安大爷“能不能进”，直接进就行。
                .maxAge(3600);
    }

    private String[] parseAllowedOrigins() {
        return allowedOrigins.split("\\s*,\\s*");
    }
}
