package com.finance.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 异步任务配置
 * 用于风险检测等异步任务的线程池配置
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * 风险检测专用线程池
     * 核心线程2个，最大5个，避免占用过多资源
     */
    @Bean("riskDetectionExecutor")
    public Executor riskDetectionExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("risk-detect-");
        executor.initialize();
        return executor;
    }
}