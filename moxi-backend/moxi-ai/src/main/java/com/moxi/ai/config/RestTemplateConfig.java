package com.moxi.ai.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * RestTemplate 配置。
 *
 * <p>为 {@code AgentClient} 等组件提供统一的 {@link RestTemplate} Bean，
 * 连接/读取超时依据 {@code agent.timeout-ms} 配置。</p>
 */
@Configuration
public class RestTemplateConfig {

    @Value("${agent.timeout-ms:120000}")
    private int timeoutMs;

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeoutMs);
        factory.setReadTimeout(timeoutMs);
        return new RestTemplate(factory);
    }
}
