package com.yinyue.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

/**
 * 【配置类】RetryConfig (启用 Spring Retry 重试机制)
 * 
 * 【核心作用】
 * 告诉 Spring Framework 启用 @Retryable 注解的功能。
 * 没有这个配置，即使在方法上写了 @Retryable，也不会生效。
 * 
 * 【工作原理】
 * Spring 会为标记了 @Retryable 的方法创建代理对象，
 * 自动在方法调用失败时进行重试。
 * 
 * 【重试策略】
 * 每个 AI 服务的重试参数都不一样：
 * - AuddApiService.recognizeAudio(): 3 次重试，间隔 1s-2s-4s
 * - AuddApiService.recognizeAudioUrl(): 2 次重试，间隔 1s-2s
 * - QwenAsrService.transcribeAudio(): 3 次重试，间隔 1.5s-3s-6s
 * - QwenLLMService.callQwenLLM(): 已通过 @Retryable 实现
 */
@Configuration
@EnableRetry
public class RetryConfig {
    // 配置类本身不需要任何代码，只是为了让 @EnableRetry 注解生效
}
