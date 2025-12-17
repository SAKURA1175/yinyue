package com.yinyue.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * ======================================================================================
 * 类名：RestTemplateConfig (网络请求工具配置)
 * 
 * 作用：配置程序里的“专用邮递员”。
 * 
 * 在我们的程序里，经常要去访问别人的网站（比如去问阿里云 AI，去问 Stable Diffusion 画图）。
 * RestTemplate 就是负责干这事的工具。
 * 
 * 这个类的作用是把这个工具组装好，设置好超时时间（防止一直干等），然后放在 Spring 的工具箱里，
 * 谁要用就直接拿（@Autowired）。
 * ======================================================================================
 */
@Configuration
public class RestTemplateConfig {

    /**
     * 方法名：restTemplate (创建邮递员)
     * 
     * @Bean 的意思是：把这个方法返回的东西（RestTemplate）变成一个公用的组件，
     * 整个程序里只有一个（单例），大家共用。
     * 
     * @param builder 这是一个专门用来造 RestTemplate 的工厂
     * @return 组装好的 RestTemplate
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                // 设置连接超时：如果在 10 秒内连不上对方服务器（比如对方断网了），就放弃报错。
                .setConnectTimeout(Duration.ofSeconds(10))
                
                // 设置读取超时：如果连上了，但是对方 60 秒都没把数据发完（比如画图画太慢），也放弃报错。
                .setReadTimeout(Duration.ofSeconds(60))
                
                // 使用自定义的请求工厂（为了解决只能读一次数据的问题，看下面的解释）
                .requestFactory(this::clientHttpRequestFactory)
                
                .build();
    }

    /**
     * 方法名：clientHttpRequestFactory (创建请求工厂)
     * 
     * 作用：这是一个高级设置。
     * 默认的工厂有一个缺点：回信的内容（InputStream）只能读一次。
     * 如果我们想打印日志看看回信是啥，读了一次之后，程序后面就读不到数据了，会报错。
     * 
     * BufferingClientHttpRequestFactory 就像是一个“复印机”。
     * 它把回信的内容先存在内存里（Buffer），这样你想读几次都可以（既能打印日志，又能处理业务）。
     */
    private ClientHttpRequestFactory clientHttpRequestFactory() {
        // 创建一个基础工厂
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        
        // 再次确认一下超时时间（双保险）
        factory.setConnectTimeout(60000); // 60秒
        factory.setReadTimeout(300000);   // 300秒 (5分钟) - 因为 AI 画图有时候真的很慢！
        
        // 开启缓冲功能
        factory.setBufferRequestBody(true);
        
        // 把基础工厂包装成带缓冲功能的工厂
        return new BufferingClientHttpRequestFactory(factory);
    }
}
